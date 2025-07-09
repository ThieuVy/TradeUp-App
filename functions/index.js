/* eslint-disable valid-jsdoc */ // Tạm thời bỏ qua quy tắc JSDoc
const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");
admin.initializeApp();

// Lấy secret key đã lưu an toàn từ config
const stripe = require("stripe")(functions.config().stripe.secret);

/**
 * Endpoint 1: Lấy hoặc tạo một Stripe Customer ID cho người dùng Firebase.
 * Lưu ID này vào Firestore để tái sử dụng.
 * @param {string} userId The user's ID.
 * @return {Promise<string>} The customer ID.
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
 * Mục đích: Tạo một "ý định thiết lập" phương thức thanh toán.
 * Client sẽ dùng `client_secret` từ đây để hiển thị PaymentSheet.
 */
exports.createSetupIntent = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError(
            "unauthenticated",
            "The function must be called while authenticated.",
        );
    }

    const userId = context.auth.uid;
    const customerId = await getOrCreateCustomer(userId);

    const setupIntent = await stripe.setupIntents.create({
        customer: customerId,
        // Chỉ định các loại phương thức thanh toán bạn muốn hỗ trợ. 'card' là thẻ.
        // Đây là cách thay thế cho `allowsDelayedPaymentMethods` và là yêu cầu của các API mới.
        payment_method_types: ["card"],
    });

    return {
        clientSecret: setupIntent.client_secret,
        customerId: customerId,
    };
});


/**
 * Cloud Function 2: createEphemeralKey
 * Mục đích: Tạo một "khóa tạm thời" cho phép client thực hiện các hành động
 * thay mặt cho customer một cách an toàn (hiển thị danh sách thẻ đã lưu).
 */
exports.createEphemeralKey = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError(
          "unauthenticated",
          "The function must be called while authenticated.",
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
 * Xóa tài khoản người dùng khỏi Authentication, Firestore và tất cả các dữ liệu liên quan.
 * Đây là một hành động không thể hoàn tác.
 */
exports.permanentlyDeleteUserAccount = functions.https.onCall(
    async (data, context) => {
        // Kiểm tra xác thực
        if (!context.auth) {
            throw new functions.https.HttpsError(
                "unauthenticated",
                "The function must be called while authenticated.",
            );
        }

        const uid = context.auth.uid;
        const db = admin.firestore();

        try {
            // Bước 1: Xóa người dùng khỏi Firebase Authentication
            await admin.auth().deleteUser(uid);
            console.log("Successfully deleted user from Auth:", uid);

            // Bước 2: Xóa document người dùng khỏi Firestore
            await db.collection("users").doc(uid).delete();
            console.log("Successfully deleted user data from Firestore:", uid);

            // Bước 3: Xóa tất cả các tin đăng của người dùng này
            // Sử dụng batch write để xóa nhiều document một cách hiệu quả.
            const listingsQuery = db.collection("listings").where("sellerId", "==", uid);
            const listingsSnapshot = await listingsQuery.get();

            if (!listingsSnapshot.empty) {
                const batch = db.batch();
                listingsSnapshot.forEach((doc) => {
                    batch.delete(doc.ref);
                });
                await batch.commit();
                console.log(`Successfully deleted ${listingsSnapshot.size} listings for user:`, uid);
            } else {
                console.log(`No listings found for user:`, uid);
            }

            // TODO: (Tùy chọn nâng cao) Có thể thêm logic để xóa các reviews, offers, chats... của người dùng này.

            return {success: true, message: "Account deleted successfully."};
        } catch (error) {
            console.error("Error deleting user:", uid, error);
            throw new functions.https.HttpsError(
                "internal",
                "Failed to delete user account.",
                error,
            );
        }
    },
);

/**
 * Cloud Function 4: sendChatNotification
 * Mục đích: Kích hoạt khi có tin nhắn mới và gửi Push Notification cho người nhận.
 * Trigger: onWrite trên documents trong subcollection 'messages'.
 */
exports.sendChatNotification = functions.firestore
    .document("chats/{chatId}/messages/{messageId}")
    .onCreate(async (snap, context) => {
        const message = snap.data();
        const chatId = context.params.chatId;
        const senderId = message.senderId;

        // 1. Lấy thông tin cuộc trò chuyện để tìm người nhận
        const chatDoc = await admin.firestore().collection("chats").doc(chatId).get();
        if (!chatDoc.exists) {
            console.log("Không tìm thấy chat document:", chatId);
            return null;
        }
        const chatData = chatDoc.data();
        const members = chatData.members;

        // 2. Xác định ID người nhận
        const recipientId = members.find((id) => id !== senderId);
        if (!recipientId) {
            console.log("Không tìm thấy người nhận trong chat:", chatId);
            return null;
        }

        // 3. Lấy thông tin người nhận (để lấy FCM token) và người gửi (để lấy tên)
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

        // 4. Xây dựng payload cho notification
        const payload = {
            notification: {
                title: `Tin nhắn mới từ ${senderData.name}`,
                body: message.text,
                icon: senderData.profileImageUrl || "default_icon_url", // Cung cấp icon mặc định nếu cần
                click_action: "FLUTTER_NOTIFICATION_CLICK", // Cần thiết cho một số client
            },
            data: {
                chatId: chatId,
                senderId: senderId,
                // Thêm các dữ liệu khác bạn muốn gửi đến client ở đây
            },
        };

        // 5. Gửi notification
        try {
            await admin.messaging().sendToDevice(fcmToken, payload);
            console.log("Gửi notification thành công đến:", recipientId);
        } catch (error) {
            console.error("Lỗi khi gửi notification:", error);
        }

        return null;
    });
