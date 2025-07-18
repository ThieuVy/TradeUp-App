/* eslint-disable max-len */
/* eslint-disable valid-jsdoc */
const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");
const stripe = require("stripe")(functions.config().stripe.secret);

admin.initializeApp();

// ========================================================
// HÀM HỖ TRỢ & BẢO MẬT
// ========================================================
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

/**
 * ===================================================================
 * ===                    RỦI RO BẢO MẬT CAO                     ===
 * ===================================================================
 * Chức năng `setAdminClaim` dưới đây dùng để cấp quyền Admin cao nhất
 * cho một tài khoản. Chỉ sử dụng chức năng này MỘT LẦN DUY NHẤT để
 * thiết lập tài khoản admin đầu tiên.
 *
 * SAU KHI SỬ DỤNG, BẠN PHẢI VÔ HIỆU HÓA (COMMENT OUT) TOÀN BỘ
 * KHỐI MÃ NÀY VÀ TRIỂN KHAI LẠI BẰNG LỆNH:
 * `firebase deploy --only functions`
 *
 * Việc để chức năng này hoạt động có thể tạo ra lỗ hổng bảo mật nghiêm trọng.
 */
/*
exports.setAdminClaim = functions.https.onCall(async (data, context) => {
    // Tạm thời comment ra để đảm bảo an toàn.
    // Mở ra khi cần cấp quyền cho admin mới và deploy lại.
    // Sau khi dùng xong, comment lại và deploy lần nữa.

    // Bước 1: Xác thực người gọi phải là admin (nếu muốn admin hiện tại cấp quyền cho admin mới)
    // Hoặc bỏ qua bước này cho lần cấp quyền đầu tiên.
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
*/

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
 * Cloud Function: Tạo Payment Intent cho hệ thống ký quỹ (Escrow).
 * Chỉ giữ tiền của khách hàng (authorize) chứ chưa thu (capture).
 */
exports.createPaymentIntentForEscrow = functions.runWith({ enforceAppCheck: true }).https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Cần xác thực để thanh toán.");
    }
    const userId = context.auth.uid;
    const { amount, listingId, sellerId } = data;

    if (!amount || amount <= 0 || !listingId || !sellerId) {
        throw new functions.https.HttpsError("invalid-argument", "Thiếu thông tin thanh toán (amount, listingId, sellerId).");
    }

    try {
        const customerId = await getOrCreateCustomer(userId);
        const paymentIntent = await stripe.paymentIntents.create({
            // Stripe yêu cầu amount là số nguyên (đơn vị nhỏ nhất)
            // Đối với VND không có đơn vị nhỏ hơn, nên ta giữ nguyên.
            amount: Math.round(amount),
            currency: "vnd",
            customer: customerId,
            capture_method: "manual", // QUAN TRỌNG: Chỉ giữ tiền, chưa thu tiền ngay
            metadata: {
                firebaseUID: userId,
                listingId: listingId,
                flow: "escrow"
            },
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
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        const ephemeralKey = await stripe.ephemeralKeys.create(
            { customer: customerId },
            { apiVersion: '2024-04-10' } // Sử dụng phiên bản API Stripe mới nhất
        );

        return {
            clientSecret: paymentIntent.client_secret,
            ephemeralKeySecret: ephemeralKey.secret,
            customerId: customerId,
        };

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
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Yêu cầu cần được xác thực.");
    }
    const uid = context.auth.uid;
    const db = admin.firestore();

    try {
        // 1. Xóa tất cả tin đăng của người dùng
        const listingsQuery = db.collection("listings").where("sellerId", "==", uid);
        const listingsSnapshot = await listingsQuery.get();
        if (!listingsSnapshot.empty) {
            const batch = db.batch();
            listingsSnapshot.forEach(doc => batch.delete(doc.ref));
            await batch.commit();
        }

        // 2. Xóa document user trong Firestore
        await db.collection("users").doc(uid).delete();

        // 3. Xóa user khỏi Authentication (thực hiện cuối cùng)
        await admin.auth().deleteUser(uid);

        return { success: true, message: "Tài khoản và tất cả dữ liệu liên quan đã được xóa vĩnh viễn." };
    } catch (error) {
        console.error("Lỗi khi xóa vĩnh viễn người dùng:", uid, error);
        throw new functions.https.HttpsError("internal", "Xóa tài khoản thất bại.");
    }
});

// ========================================================
// TRIGGERS TỰ ĐỘNG
// ========================================================

exports.sendChatNotification = functions.firestore
    .document("chats/{chatId}/messages/{messageId}")
    .onCreate(async (snap, context) => {
        const message = snap.data();
        const chatId = context.params.chatId;
        const senderId = message.senderId;

        const chatDoc = await admin.firestore().collection("chats").doc(chatId).get();
        if (!chatDoc.exists) return null;

        const recipientId = chatDoc.data().members.find((id) => id !== senderId);
        if (!recipientId) return null;

        const senderDoc = await admin.firestore().collection("users").doc(senderId).get();
        const recipientDoc = await admin.firestore().collection("users").doc(recipientId).get();
        if (!senderDoc.exists || !recipientDoc.exists) return null;

        const senderData = senderDoc.data();
        const recipientData = recipientDoc.data();

        const notificationPayload = {
            userId: recipientId,
            title: `Tin nhắn mới từ ${senderData.name || "Một người dùng"}`,
            content: message.text || "Đã gửi một hình ảnh.",
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            type: "MESSAGE",
            category: "Tin nhắn",
            isRead: false,
            actionUrl: `chat/${chatId}`
        };
        await admin.firestore().collection("notifications").add(notificationPayload);

        const fcmToken = recipientData.fcmToken;
        if (fcmToken) {
            const pushPayload = {
                notification: {
                    title: `Tin nhắn mới từ ${senderData.name}`,
                    body: message.text || "Đã gửi một hình ảnh",
                    icon: senderData.profileImageUrl || "default_icon_url",
                    click_action: "FLUTTER_NOTIFICATION_CLICK",
                    tag: chatId,
                },
                data: {
                    chatId: chatId,
                    senderId: senderId,
                },
            };
            try {
                await admin.messaging().sendToDevice(fcmToken, pushPayload);
            } catch (error) {
                console.error("Lỗi khi gửi push notification:", error);
            }
        }
        return null;
    });

/**
 * TRIGGER: Tự động tạo thông báo cho người bán khi có một offer mới.
 * (Hàm này bạn đã viết đúng, chỉ cần đảm bảo nó tồn tại)
 */
exports.sendNewOfferNotification = functions.firestore
    .document("offers/{offerId}")
    .onCreate(async (snap, context) => {
        const offer = snap.data();
        if (!offer) return null;

        const sellerId = offer.sellerId;
        const buyerName = offer.buyerName || "Một người dùng";
        const offerPrice = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(offer.offerPrice);
        const listingDoc = await admin.firestore().collection("listings").doc(offer.listingId).get();
        const listingTitle = listingDoc.exists ? listingDoc.data().title : "sản phẩm của bạn";

        const notificationPayload = {
            userId: sellerId,
            title: "Bạn có đề nghị mới!",
            content: `${buyerName} đã đề nghị ${offerPrice} cho sản phẩm "${listingTitle}".`,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            type: "PROMOTION",
            category: "Ưu đãi",
            isRead: false,
            actionUrl: `listing/${offer.listingId}`
        };
        return admin.firestore().collection("notifications").add(notificationPayload);
    });

/**
 * Lấy lịch sử giao dịch Stripe cho người dùng đã xác thực.
 */
exports.getPaymentHistory = functions.https.onCall(async (data, context) => {
    // 1. Xác thực người dùng
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Yêu cầu cần được xác thực.");
    }
    const userId = context.auth.uid;

    try {
        // 2. Lấy Stripe Customer ID từ Firestore
        const customerId = await getOrCreateCustomer(userId);

        // 3. Gọi Stripe API để lấy danh sách Payment Intents
        const paymentIntents = await stripe.paymentIntents.list({
            customer: customerId,
            limit: 50, // Giới hạn 50 giao dịch gần nhất
        });

        // 4. Định dạng lại dữ liệu trả về cho client
        const history = paymentIntents.data.map((pi) => ({
            id: pi.id,
            description: pi.description || "Thanh toán cho sản phẩm",
            amount: pi.amount,
            currency: pi.currency,
            status: pi.status, // succeeded, requires_payment_method, canceled
            created: pi.created, // Unix timestamp
        }));

        return { success: true, history: history };
    } catch (error) {
        console.error("Lỗi khi lấy lịch sử thanh toán Stripe:", error);
        throw new functions.https.HttpsError("internal", "Không thể lấy lịch sử giao dịch.");
    }
});

/**
 * TRIGGER: Tự động tăng offersCount trên một listing khi có offer mới.
 */
exports.updateListingOnNewOffer = functions.firestore
    .document("offers/{offerId}")
    .onCreate(async (snap, context) => {
        const offerData = snap.data();
        const listingId = offerData.listingId;

        if (!listingId) {
            console.log("Offer không có listingId, bỏ qua.");
            return null;
        }

        const listingRef = admin.firestore().collection("listings").doc(listingId);

        try {
            // Sử dụng FieldValue.increment để tăng giá trị một cách an toàn
            await listingRef.update({
                offersCount: admin.firestore.FieldValue.increment(1),
            });
            console.log(`Đã tăng offersCount cho listing ${listingId}`);
            return { success: true };
        } catch (error) {
            console.error(
                `Lỗi khi tăng offersCount cho listing ${listingId}:`,
                error
            );
            return { success: false, error: error.message };
        }
    });