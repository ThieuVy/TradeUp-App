/* eslint-disable valid-jsdoc */ // Tạm thời bỏ qua quy tắc JSDoc
const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");
admin.initializeApp();

// Lấy khóa bí mật được lưu trữ an toàn trong cấu hình
const stripe = require("stripe")(functions.config().stripe.secret);

/**
 * Endpoint 1: Lấy hoặc tạo Stripe Customer ID cho người dùng Firebase.
 * Lưu ID này vào Firestore để tái sử dụng.
 * @param {string} userId ID của người dùng.
 * @return {Promise<string>} Customer ID.
 */
const getOrCreateCustomer = async (userId) => {
    const userDoc = await admin.firestore().collection("users").doc(userId).get();
    const userData = userDoc.data();

    if (userData && userData.stripeCustomerId) {
        return userData.stripeCustomerId;
    }

    const customer = await stripe.customers.create({
        email: userData.email,
        metadata: {firebaseUID: userId},
    });

    await admin.firestore().collection("users").doc(userId).update({
        stripeCustomerId: customer.id,
    });

    return customer.id;
};

/**
 * Cloud Function 1: createSetupIntent
 * Mục đích: Tạo "setup intent" để thiết lập phương thức thanh toán.
 * Ứng dụng sẽ dùng `client_secret` để hiển thị PaymentSheet.
 */
exports.createSetupIntent = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError(
            "unauthenticated",
            "Hàm này phải được gọi khi đã xác thực.",
        );
    }

    const userId = context.auth.uid;
    const customerId = await getOrCreateCustomer(userId);

    const setupIntent = await stripe.setupIntents.create({
        customer: customerId,
        payment_method_types: ["card"],
    });

    return {
        clientSecret: setupIntent.client_secret,
        customerId: customerId,
    };
});


/**
 * Cloud Function 2: createEphemeralKey
 * Mục đích: Tạo "ephemeral key" cho phép ứng dụng thực hiện các hành động thay mặt khách hàng.
 */
exports.createEphemeralKey = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError(
          "unauthenticated",
          "Hàm này phải được gọi khi đã xác thực.",
        );
    }
    const userId = context.auth.uid;
    const customerId = await getOrCreateCustomer(userId);
    const apiVersion = data.apiVersion;

    const key = await stripe.ephemeralKeys.create(
        {customer: customerId},
        {apiVersion: apiVersion},
    );

    return {
        ephemeralKey: key.secret,
    };
});

/**
 * Cloud Function 3: permanentlyDeleteUserAccount
 * Xóa vĩnh viễn tài khoản người dùng khỏi Authentication, Firestore và dữ liệu liên quan.
 * Hành động này không thể khôi phục lại.
 */
exports.permanentlyDeleteUserAccount = functions.https.onCall(
    async (data, context) => {
        if (!context.auth) {
            throw new functions.https.HttpsError(
                "unauthenticated",
                "Hàm này phải được gọi khi đã xác thực.",
            );
        }

        const uid = context.auth.uid;
        const db = admin.firestore();

        try {
            // Bước 1: Xóa người dùng khỏi Firebase Authentication
            await admin.auth().deleteUser(uid);
            console.log("Đã xóa người dùng khỏi Auth:", uid);

            // Bước 2: Xóa tài liệu người dùng khỏi Firestore
            await db.collection("users").doc(uid).delete();
            console.log("Đã xóa dữ liệu người dùng khỏi Firestore:", uid);

            // === BẮT ĐẦU PHẦN SỬA LỖI ===
            // Bước 3: Xóa tất cả bài đăng của người dùng
            const listingsQuery = db.collection("listings").where("sellerId", "==", uid);
            const listingsSnapshot = await listingsQuery.get();

            if (!listingsSnapshot.empty) {
                const batch = db.batch();
                listingsSnapshot.forEach((doc) => {
                    batch.delete(doc.ref);
                });
                await batch.commit();
                console.log(`Đã xóa ${listingsSnapshot.size} bài đăng cho người dùng:`, uid);
            } else {
                console.log(`Không có bài đăng nào để xóa cho người dùng:`, uid);
            }
            // === KẾT THÚC PHẦN SỬA LỖI ===

            // TODO: (Tùy chọn nâng cao) Thêm logic để xóa đánh giá, lời đề nghị, trò chuyện,... của người dùng

            return {success: true, message: "Tài khoản đã được xóa thành công."};
        } catch (error) {
            console.error("Lỗi khi xóa người dùng:", uid, error);
            throw new functions.https.HttpsError(
                "internal",
                "Xóa tài khoản người dùng thất bại.",
                error,
            );
        }
    },
);

/**
 * Cloud Function 4: sendChatNotification
 * Mục đích: Gửi thông báo đẩy khi có tin nhắn mới.
 * Kích hoạt: khi có tài liệu mới trong tập con 'messages'.
 */
exports.sendChatNotification = functions.firestore
    .document("chats/{chatId}/messages/{messageId}")
    .onCreate(async (snap, context) => {
        const message = snap.data();
        const chatId = context.params.chatId;
        const senderId = message.senderId;

        // 1. Lấy thông tin cuộc trò chuyện để xác định người nhận
        const chatDoc = await admin.firestore().collection("chats").doc(chatId).get();
        if (!chatDoc.exists) {
            console.log("Không tìm thấy tài liệu chat:", chatId);
            return null;
        }
        const chatData = chatDoc.data();
        const members = chatData.members;

        // 2. Xác định ID người nhận
        const recipientId = members.find((id) => id !== senderId);
        if (!recipientId) {
            console.log("Không tìm thấy người nhận trong cuộc trò chuyện:", chatId);
            return null;
        }

        // 3. Lấy thông tin người nhận và người gửi
        const recipientDoc = await admin.firestore().collection("users").doc(recipientId).get();
        const senderDoc = await admin.firestore().collection("users").doc(senderId).get();

        if (!recipientDoc.exists || !senderDoc.exists) {
            console.log("Không tìm thấy thông tin người gửi hoặc người nhận.");
            return null;
        }

        const recipientData = recipientDoc.data();
        const senderData = senderDoc.data();
        const fcmToken = recipientData.fcmToken;

        if (!fcmToken) {
            console.log("Người nhận không có FCM token:", recipientId);
            return null;
        }

        // 4. Tạo nội dung thông báo
        const payload = {
            notification: {
                title: `Tin nhắn mới từ ${senderData.name}`,
                body: message.text,
                icon: senderData.profileImageUrl || "default_icon_url",
                click_action: "FLUTTER_NOTIFICATION_CLICK",
            },
            data: {
                chatId: chatId,
                senderId: senderId,
            },
        };

        // 5. Gửi thông báo
        try {
            await admin.messaging().sendToDevice(fcmToken, payload);
            console.log("Đã gửi thông báo đến:", recipientId);
        } catch (error) {
            console.error("Lỗi khi gửi thông báo:", error);
        }

        return null;
    });

/**
 * Cloud Function 5: createPaymentIntentForEscrow
 * Mục đích: Tạo một Payment Intent để giữ tiền của người mua (escrow).
 * Sử dụng capture_method: 'manual' để chỉ ủy quyền, chưa thu tiền ngay.
 * @param {number} amount - Số tiền (tính bằng đơn vị nhỏ nhất, ví dụ: VNĐ)
 * @param {string} listingId - ID của sản phẩm
 * @return {Promise<object>} Chứa clientSecret và paymentIntentId.
 */
exports.createPaymentIntentForEscrow = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Hàm này phải được gọi khi đã xác thực.");
    }
    const userId = context.auth.uid;
    const { amount, listingId } = data;

    if (!amount || amount <= 0 || !listingId) {
        throw new functions.https.HttpsError("invalid-argument", "Vui lòng cung cấp đủ thông tin (amount, listingId).");
    }

    const customerId = await getOrCreateCustomer(userId);

    // Tạo Payment Intent với chế độ giữ tiền thủ công
    const paymentIntent = await stripe.paymentIntents.create({
        amount: Math.round(amount), // Stripe yêu cầu số nguyên
        currency: "vnd",
        customer: customerId,
        capture_method: "manual", // QUAN TRỌNG: Chìa khóa cho hệ thống ký quỹ
        metadata: {
            firebaseUID: userId,
            listingId: listingId,
        },
    });

    // (Tùy chọn) Tạo một document trong collection 'escrows' để theo dõi
    await admin.firestore().collection("escrows").doc(paymentIntent.id).set({
        listingId: listingId,
        buyerId: userId,
        sellerId: data.sellerId, // Cần truyền sellerId từ client
        amount: amount,
        status: "pending_payment", // Trạng thái ban đầu
        paymentIntentId: paymentIntent.id,
        created_at: admin.firestore.FieldValue.serverTimestamp(),
    });

    return {
        clientSecret: paymentIntent.client_secret,
        paymentIntentId: paymentIntent.id,
    };
});


/**
 * Cloud Function 6: captureEscrowPayment
 * Mục đích: Thu tiền từ một Payment Intent đã được ủy quyền trước đó.
 * Được gọi khi người mua xác nhận đã nhận hàng.
 * @param {string} paymentIntentId - ID của Payment Intent cần thu tiền.
 * @return {Promise<object>} Trạng thái thành công.
 */
exports.captureEscrowPayment = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Hàm này phải được gọi khi đã xác thực.");
    }
    const { paymentIntentId } = data;
    if (!paymentIntentId) {
        throw new functions.https.HttpsError("invalid-argument", "Vui lòng cung cấp paymentIntentId.");
    }

    try {
        // Thu tiền
        const paymentIntent = await stripe.paymentIntents.capture(paymentIntentId);

        // Cập nhật trạng thái trong Firestore
        await admin.firestore().collection("escrows").doc(paymentIntentId).update({
            status: "completed",
            captured_at: admin.firestore.FieldValue.serverTimestamp(),
        });

        // TODO: (Nâng cao) Tại đây, bạn sẽ thực hiện việc chuyển tiền (Transfer) cho người bán
        // thông qua Stripe Connect.

        return { success: true, message: "Thanh toán đã được hoàn tất." };
    } catch (error) {
        console.error("Lỗi khi thu tiền ký quỹ:", error);
        throw new functions.https.HttpsError("internal", "Thu tiền ký quỹ thất bại.", error.message);
    }
});
