/* eslint-disable max-len */
/* eslint-disable valid-jsdoc */
const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");
const stripe = require("stripe")(functions.config().stripe.secret);

admin.initializeApp();

// ========================================================
// HÀM HỖ TRỢ & BẢO MẬT
// ========================================================

/**
 * Xác minh xem người gọi hàm có phải là Admin không.
 * @param {functions.https.CallableContext} context Context của hàm.
 */
const verifyAdmin = async (context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Yêu cầu cần được xác thực.");
    }
    const user = await admin.auth().getUser(context.auth.uid);
    if (user.customClaims && user.customClaims.admin === true) {
        return true;
    }
    throw new functions.https.HttpsError("permission-denied", "Chỉ quản trị viên mới có thể thực hiện hành động này.");
};

/**
 * Lấy hoặc tạo Stripe Customer ID cho người dùng Firebase.
 */
const getOrCreateCustomer = async (userId) => {
    const userDoc = await admin.firestore().collection("users").doc(userId).get();
    const userData = userDoc.data();

    if (userData && userData.stripeCustomerId) {
        return userData.stripeCustomerId;
    }

    const customer = await stripe.customers.create({
        email: userData.email,
        metadata: { firebaseUID: userId },
    });

    await admin.firestore().collection("users").doc(userId).update({
        stripeCustomerId: customer.id,
    });

    return customer.id;
};

// ========================================================
// CLOUD FUNCTIONS DÀNH CHO ADMIN
// ========================================================

/**
 * Cloud Function: setAdminClaim
 * Gán quyền admin cho một người dùng.
 */
exports.setAdminClaim = functions.https.onCall(async (data, context) => {
    await verifyAdmin(context);

    const uid = data.uid;
    if (typeof uid !== "string" || uid.length === 0) {
        throw new functions.https.HttpsError("invalid-argument", "UID không hợp lệ hoặc bị thiếu.");
    }

    try {
        await admin.auth().setCustomUserClaims(uid, { admin: true });
        console.log(`Cấp quyền admin thành công cho UID: ${uid}`);
        return { message: `Thành công! Người dùng ${uid} đã được cấp quyền admin.` };
    } catch (error) {
        console.error("Lỗi khi gán quyền admin cho UID:", uid, error);
        throw new functions.https.HttpsError("internal", "Không thể gán quyền admin.");
    }
});

/**
 * Cloud Function: moderateReview
 * Admin duyệt một đánh giá.
 */
exports.moderateReview = functions.https.onCall(async (data, context) => {
    await verifyAdmin(context);

    const { reviewId, newStatus } = data;
    if (!reviewId || !newStatus || !["approved", "rejected"].includes(newStatus)) {
        throw new functions.https.HttpsError("invalid-argument", "Vui lòng cung cấp reviewId và newStatus hợp lệ.");
    }

    try {
        await admin.firestore().collection("reviews").doc(reviewId).update({ moderationStatus: newStatus });
        return { success: true, message: `Đánh giá ${reviewId} đã được ${newStatus}.` };
    } catch (error) {
        console.error("Lỗi khi duyệt review:", error);
        throw new functions.https.HttpsError("internal", "Không thể cập nhật đánh giá.");
    }
});

/**
 * Cloud Function: resolveReport
 * Admin xử lý báo cáo và (tùy chọn) treo tài khoản người bị báo cáo.
 */
exports.resolveReport = functions.https.onCall(async (data, context) => {
    await verifyAdmin(context);

    const { reportId, reportedUserId, shouldSuspend } = data;
    if (!reportId) {
        throw new functions.https.HttpsError("invalid-argument", "Thiếu reportId.");
    }

    const db = admin.firestore();
    const batch = db.batch();
    const reportRef = db.collection("reports").doc(reportId);

    batch.update(reportRef, {
        status: "resolved",
        resolvedBy: context.auth.uid,
        resolvedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    if (shouldSuspend && reportedUserId) {
        const userRef = db.collection("users").doc(reportedUserId);
        batch.update(userRef, { accountStatus: "suspended" });
    }

    try {
        await batch.commit();
        const message = `Báo cáo ${reportId} đã được giải quyết.${shouldSuspend ? ` Người dùng ${reportedUserId} đã bị treo.` : ""}`;
        return { success: true, message };
    } catch (error) {
        console.error("Lỗi khi giải quyết báo cáo:", error);
        throw new functions.https.HttpsError("internal", "Không thể giải quyết báo cáo.");
    }
});

/**
 * Cloud Function: updateUserStatus
 * Admin cập nhật trạng thái người dùng.
 */
exports.updateUserStatus = functions.https.onCall(async (data, context) => {
    await verifyAdmin(context);

    const { targetUid, newStatus } = data;
    if (!targetUid || !["active", "suspended"].includes(newStatus)) {
        throw new functions.https.HttpsError("invalid-argument", "Dữ liệu không hợp lệ.");
    }

    try {
        await admin.firestore().collection("users").doc(targetUid).update({ accountStatus: newStatus });
        return { success: true, message: `Tài khoản đã được ${newStatus === "active" ? "kích hoạt" : "treo"}.` };
    } catch (error) {
        console.error("Lỗi khi cập nhật trạng thái:", error);
        throw new functions.https.HttpsError("internal", "Không thể cập nhật trạng thái.");
    }
});

// ========================================================
// CLOUD FUNCTIONS CHO NGƯỜI DÙNG & THANH TOÁN
// ========================================================

/**
 * Cloud Function: createSetupIntent
 */
exports.createSetupIntent = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Cần xác thực.");

    const userId = context.auth.uid;
    const customerId = await getOrCreateCustomer(userId);

    const setupIntent = await stripe.setupIntents.create({
        customer: customerId,
        payment_method_types: ["card"],
    });

    return { clientSecret: setupIntent.client_secret, customerId };
});

/**
 * Cloud Function: createEphemeralKey
 */
exports.createEphemeralKey = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Cần xác thực.");

    const userId = context.auth.uid;
    const apiVersion = data.apiVersion;
    const customerId = await getOrCreateCustomer(userId);

    const key = await stripe.ephemeralKeys.create({ customer: customerId }, { apiVersion });
    return { ephemeralKey: key.secret };
});

/**
 * Cloud Function: createPaymentIntentForEscrow
 */
exports.createPaymentIntentForEscrow = functions.runWith({ enforceAppCheck: true }).https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Cần xác thực để thanh toán.");

    const userId = context.auth.uid;
    const { amount, listingId, sellerId } = data;

    if (!amount || amount <= 0 || !listingId || !sellerId) {
        throw new functions.https.HttpsError("invalid-argument", "Thiếu thông tin thanh toán (amount, listingId, sellerId).");
    }

    try {
        const customerId = await getOrCreateCustomer(userId);
        const paymentIntent = await stripe.paymentIntents.create({
            amount: Math.round(amount), // Stripe yêu cầu amount là số nguyên (đơn vị nhỏ nhất, ví dụ: xu, hoặc đồng cho VND)
            currency: "vnd", // Đơn vị tiền tệ Việt Nam
            customer: customerId,
            capture_method: "manual", // QUAN TRỌNG: Chỉ giữ tiền, chưa thu tiền ngay
            metadata: { firebaseUID: userId, listingId: listingId },
        });

        console.log(`Tạo PaymentIntent ${paymentIntent.id} cho user ${userId} thành công.`);

        // Ghi lại thông tin ký quỹ vào Firestore
        await admin.firestore().collection("escrows").doc(paymentIntent.id).set({
            listingId,
            buyerId: userId,
            sellerId,
            amount,
            status: "pending_payment", // Trạng thái chờ thanh toán từ người mua
            paymentIntentId: paymentIntent.id,
            created_at: admin.firestore.FieldValue.serverTimestamp(),
        });

        return { clientSecret: paymentIntent.client_secret, paymentIntentId: paymentIntent.id };

    } catch (error) {
        console.error("Lỗi nghiêm trọng khi tạo PaymentIntent:", error);
        throw new functions.https.HttpsError("internal", "Không thể khởi tạo thanh toán. Vui lòng thử lại sau.", error.message);
    }
});

/**
 * Cloud Function: captureEscrowPayment
 */
exports.captureEscrowPayment = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Cần xác thực.");

    const { paymentIntentId } = data;
    if (!paymentIntentId) throw new functions.https.HttpsError("invalid-argument", "Thiếu paymentIntentId.");

    try {
        const paymentIntent = await stripe.paymentIntents.capture(paymentIntentId);
        await admin.firestore().collection("escrows").doc(paymentIntentId).update({
            status: "completed",
            captured_at: admin.firestore.FieldValue.serverTimestamp(),
        });
        return { success: true, message: "Thanh toán hoàn tất." };
    } catch (error) {
        console.error("Lỗi khi thu tiền:", error);
        throw new functions.https.HttpsError("internal", "Thu tiền thất bại.");
    }
});

/**
 * Cloud Function: permanentlyDeleteUserAccount
 */
exports.permanentlyDeleteUserAccount = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Cần xác thực.");

    const uid = context.auth.uid;
    const db = admin.firestore();

    try {
        await admin.auth().deleteUser(uid);
        await db.collection("users").doc(uid).delete();

        const listingsQuery = db.collection("listings").where("sellerId", "==", uid);
        const listingsSnapshot = await listingsQuery.get();

        if (!listingsSnapshot.empty) {
            const batch = db.batch();
            listingsSnapshot.forEach((doc) => batch.delete(doc.ref));
            await batch.commit();
        }

        return { success: true, message: "Tài khoản đã được xóa." };
    } catch (error) {
        console.error("Lỗi khi xóa người dùng:", error);
        throw new functions.https.HttpsError("internal", "Xóa tài khoản thất bại.");
    }
});

/**
 * Cloud Function: sendChatNotification
 */
exports.sendChatNotification = functions.firestore
    .document("chats/{chatId}/messages/{messageId}")
    .onCreate(async (snap, context) => {
        const message = snap.data();
        const chatId = context.params.chatId;
        const senderId = message.senderId;

        // Lấy thông tin cuộc trò chuyện
        const chatDoc = await admin.firestore().collection("chats").doc(chatId).get();
        if (!chatDoc.exists) return null;

        // Xác định người nhận
        const recipientId = chatDoc.data().members.find((id) => id !== senderId);
        if (!recipientId) return null;

        // Lấy thông tin người gửi và người nhận
        const senderDoc = await admin.firestore().collection("users").doc(senderId).get();
        const recipientDoc = await admin.firestore().collection("users").doc(recipientId).get();
        if (!senderDoc.exists || !recipientDoc.exists) return null;

        const senderData = senderDoc.data();
        const recipientData = recipientDoc.data();

        // 1. Gửi Push Notification (FCM) như cũ
        const fcmToken = recipientData.fcmToken;
        if (fcmToken) {
            const payload = {
                notification: {
                    title: `Tin nhắn mới từ ${senderData.name}`,
                    body: message.text,
                    icon: senderData.profileImageUrl || "default_icon_url",
                    click_action: "FLUTTER_NOTIFICATION_CLICK", // Giữ nguyên cho app client
                },
                data: { chatId, senderId },
            };
            try {
                await admin.messaging().sendToDevice(fcmToken, payload);
                console.log("Đã gửi push notification đến:", recipientId);
            } catch (error) {
                console.error("Lỗi khi gửi push notification:", error);
            }
        }

        // 2. TẠO DOCUMENT THÔNG BÁO MỚI TRONG FIRESTORE
        const notificationPayload = {
            userId: recipientId, // Gửi thông báo cho người nhận
            title: `Tin nhắn mới từ ${senderData.name}`,
            content: message.text,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            type: "MESSAGE",
            category: "Tin nhắn",
            isRead: false,
            actionUrl: `chat/${chatId}` // Deep link để mở cuộc trò chuyện
        };

        // Ghi thông báo vào collection 'notifications'
        await admin.firestore().collection("notifications").add(notificationPayload);
        console.log("Đã tạo document thông báo trong Firestore cho:", recipientId);

        return null;
    });

/**
 * TRIGGER: Tự động tạo thông báo cho người bán khi có một offer mới.
 * Kích hoạt mỗi khi có một document mới được tạo trong collection 'offers'.
 */
exports.sendNewOfferNotification = functions.firestore
    .document("offers/{offerId}")
    .onCreate(async (snap, context) => {
        const offer = snap.data();
        if (!offer) {
            console.log("Không có dữ liệu offer.");
            return null;
        }

        const sellerId = offer.sellerId;
        const buyerName = offer.buyerName || "Một người dùng";
        const offerPrice = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(offer.offerPrice);

        // Lấy thông tin tin đăng để hiển thị tên sản phẩm
        const listingDoc = await admin.firestore().collection("listings").doc(offer.listingId).get();
        const listingTitle = listingDoc.exists ? listingDoc.data().title : "sản phẩm của bạn";

        const notificationPayload = {
            userId: sellerId, // Gửi thông báo cho người bán
            title: "Bạn có đề nghị mới!",
            content: `${buyerName} đã đề nghị ${offerPrice} cho sản phẩm "${listingTitle}".`,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            type: "PROMOTION", // Hoặc "OFFER" nếu bạn muốn định nghĩa type mới
            category: "Ưu đãi",
            isRead: false,
            actionUrl: `listing/${offer.listingId}` // Deep link để mở chi tiết sản phẩm
        };

        // Ghi thông báo vào collection 'notifications'
        return admin.firestore().collection("notifications").add(notificationPayload);
    });

