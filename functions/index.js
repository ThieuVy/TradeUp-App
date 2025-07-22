/* eslint-disable max-len */
/* eslint-disable valid-jsdoc */
const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");
const stripe = require("stripe")(functions.config().stripe.secret);

admin.initializeApp();

// ========================================================
// HÀM HỖ TRỢ & BẢO MẬT
// ========================================================
//const verifyAdmin = async (context) => {
//    if (!context.auth) {
//        throw new functions.https.HttpsError("unauthenticated", "Yêu cầu cần được xác thực.");
//    }
//    const user = await admin.auth().getUser(context.auth.uid);
//    if (user.customClaims && user.customClaims.admin === true) {
//        return true;
//    }
//    throw new functions.https.HttpsError("permission-denied", "Chỉ quản trị viên mới có thể thực hiện hành động này.");
//};

const getOrCreateCustomer = async (userId) => {
    const userRef = admin.firestore().collection("users").doc(userId);
    const userDoc = await userRef.get();

    // Kiểm tra xem document người dùng có tồn tại không
    if (!userDoc.exists) {
        // Ghi log lỗi để bạn có thể debug trên Firebase Console
        console.error(`Không tìm thấy document cho người dùng với UID: ${userId}`);
        // Ném ra một lỗi HttpsError cụ thể để ứng dụng có thể xử lý
        throw new functions.https.HttpsError("not-found", `Không tìm thấy thông tin người dùng cho UID: ${userId}.`);
    }
    const userData = userDoc.data();

    if (userData && userData.stripeCustomerId) {
        return userData.stripeCustomerId;
    }
    const customer = await stripe.customers.create({
        email: userData.email,
        metadata: { firebaseUID: userId },
    });
    await userRef.update({
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
//exports.setAdminClaim = functions.https.onCall(async (data, context) => {
//    // Tạm thời comment ra để đảm bảo an toàn.
//    // Mở ra khi cần cấp quyền cho admin mới và deploy lại.
//    // Sau khi dùng xong, comment lại và deploy lần nữa.
//
//    // Bước 1: Xác thực người gọi phải là admin (nếu muốn admin hiện tại cấp quyền cho admin mới)
//    // Hoặc bỏ qua bước này cho lần cấp quyền đầu tiên.
//    // await verifyAdmin(context); // TẠM THỜI COMMENT DÒNG NÀY LẠI
//
//    const uid = data.uid;
//    if (typeof uid !== "string" || uid.length === 0) {
//        throw new functions.https.HttpsError("invalid-argument", "UID không hợp lệ hoặc bị thiếu.");
//    }
//
//    try {
//        await admin.auth().setCustomUserClaims(uid, { admin: true });
//        console.log(`Cấp quyền admin thành công cho UID: ${uid}`);
//        return { message: `Thành công! Người dùng ${uid} đã được cấp quyền admin.` };
//    } catch (error) {
//        console.error("Lỗi khi gán quyền admin cho UID:", uid, error);
//        throw new functions.https.HttpsError("internal", "Không thể gán quyền admin.");
//    }
//});

const verifyAdmin = async (context) => {
    // Hàm này đã có sẵn trong file của bạn, dùng để kiểm tra người gọi
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
 * Cloud Function: grantAdminRole
 * Cho phép một Admin hiện tại cấp quyền Admin cho một người dùng khác.
 */
exports.grantAdminRole = functions.https.onCall(async (data, context) => {
    // Bước 1: Luôn luôn xác thực người gọi PHẢI LÀ ADMIN
    await verifyAdmin(context);

    const targetUid = data.uid;
    if (typeof targetUid !== "string" || targetUid.length === 0) {
        throw new functions.https.HttpsError("invalid-argument", "UID của người dùng mục tiêu không hợp lệ.");
    }

    try {
        // Bước 2: Gán custom claim cho người dùng mục tiêu
        await admin.auth().setCustomUserClaims(targetUid, { admin: true });
        console.log(`Admin ${context.auth.uid} đã cấp quyền admin cho UID: ${targetUid}`);
        return { success: true, message: `Đã cấp quyền Admin thành công!` };
    } catch (error) {
        console.error("Lỗi khi gán quyền admin cho UID:", targetUid, error);
        throw new functions.https.HttpsError("internal", "Không thể gán quyền admin. Vui lòng thử lại sau.");
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
                amount: Math.round(amount),
                currency: "vnd",
                customer: customerId,

                // Thay vì `capture_method` và `payment_method_types`
                automatic_payment_methods: {
                    enabled: true,
                },

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

        // 1. Cập nhật thông tin cuộc trò chuyện (lastMessage, timestamp)
        const chatRef = admin.firestore().collection("chats").doc(chatId);
        await chatRef.update({
            lastMessage: message.text || "Đã gửi một hình ảnh.",
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            lastMessageSenderId: senderId,
        });

        // 2. Lấy thông tin người nhận và token
        const chatDoc = await chatRef.get();
        if (!chatDoc.exists) return null;

        const recipientId = chatDoc.data().members.find((id) => id !== senderId);
        if (!recipientId) return null;

        const senderDoc = await admin.firestore().collection("users").doc(senderId).get();
        const recipientDoc = await admin.firestore().collection("users").doc(recipientId).get();
        if (!senderDoc.exists || !recipientDoc.exists) return null;

        const senderData = senderDoc.data();
        const recipientData = recipientDoc.data();

        // 3. (Tùy chọn) Tạo một notification document trong Firestore (cho trang thông báo)
        // ... (code tạo notification document)
        const notificationPayload = {
            userId: recipientId, // Gửi cho người nhận
            title: `Tin nhắn mới từ ${senderData.name}`,
            content: message.text || "Đã gửi một hình ảnh.",
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            type: "MESSAGE", // Loại thông báo là tin nhắn
            category: "Tin nhắn",
            read: false,
            relatedId: chatId, // ID liên quan chính là ID của cuộc trò chuyện
        };
        await admin.firestore().collection("notifications").add(notificationPayload);

        // 4. Gửi Push Notification qua FCM
        const fcmToken = recipientData.fcmToken;
        if (fcmToken) {
            const payload = {
                notification: {
                    title: `Tin nhắn mới từ ${senderData.name}`,
                    body: message.text || "Đã gửi một hình ảnh",
                },
                data: {
                    chatId: chatId, // Gửi thêm dữ liệu để xử lý deep-link
                },
            };
            try {
                await admin.messaging().sendToDevice(fcmToken, payload);
                console.log("Gửi push notification thành công đến:", recipientId);
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
        const buyerId = offer.buyerId; // Cần có buyerId
        const listingId = offer.listingId;
        const buyerName = offer.buyerName || "Một người dùng";
        const offerPrice = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(offer.offerPrice);
        const listingDoc = await admin.firestore().collection("listings").doc(listingId).get();
        const listingTitle = listingDoc.exists ? listingDoc.data().title : "sản phẩm của bạn";

        // === PHẦN 1: TẠO THÔNG BÁO (Giữ nguyên) ===
        const offerMessage = {
            senderId: buyerId, // Gửi từ phía người mua
            text: `Đã đề nghị giá ${offerPrice} cho sản phẩm "${listingTitle}".`,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            // Thêm một trường đặc biệt để nhận dạng
            messageType: "OFFER",
            offerPrice: offer.offerPrice,
        };
        // Sử dụng await để đảm bảo hàm chạy tuần tự
        await admin.firestore().collection("notifications").add(notificationPayload);

        // === PHẦN 2: TÌM/TẠO CHAT VÀ GỬI TIN NHẮN ĐỀ NGHỊ (PHẦN MỚI) ===
        try {
            const db = admin.firestore();
            let chatId = null;

            // Tìm kiếm cuộc trò chuyện hiện có
            const chatQuery = await db.collection("chats")
                .where("listingId", "==", listingId)      // Điều kiện 1: Phải liên quan đến ĐÚNG sản phẩm này
                .where("members", "array-contains", buyerId) // Điều kiện 2: Người mua phải là thành viên
                .limit(1)
                .get();

            let chatDocFound = null;
            if (!chatQuery.empty) {
                // Vòng lặp này để chắc chắn người bán cũng là thành viên
                for (const doc of chatQuery.docs) {
                    const members = doc.data().members || [];
                    if (members.includes(sellerId)) {         // Điều kiện 3: Người bán cũng phải là thành viên
                        chatDocFound = doc;
                        break; // Tìm thấy rồi, không cần tìm nữa
                    }
                }
            }

            if (chatDocFound) {
                // TRƯỜNG HỢP 1: ĐÃ TÌM THẤY
                chatId = chatDocFound.id;
                console.log(`Tìm thấy chat đã có: ${chatId}`);
            } else {
                // TRƯỜNG HỢP 2: CHƯA TỒN TẠI -> TẠO MỚI
                const newChatRef = db.collection("chats").doc();
                const newChatData = {
                    listingId: listingId,
                    members: [buyerId, sellerId], // Bao gồm cả người mua và người bán
                    timestamp: admin.firestore.FieldValue.serverTimestamp(),
                    lastMessage: `${buyerName} đã đề nghị ${offerPrice}.`,
                    lastMessageSenderId: buyerId,
                };
                await newChatRef.set(newChatData);
                chatId = newChatRef.id;
                console.log(`Đã tạo chat mới: ${chatId}`);
            }

            // Tạo tin nhắn đặc biệt cho đề nghị giá
            const offerMessage = {
                senderId: buyerId, // Gửi từ phía người mua
                text: `Đã đề nghị giá: ${offerPrice}`,
                timestamp: admin.firestore.FieldValue.serverTimestamp(),
                // Thêm một trường đặc biệt để nhận dạng
                messageType: "OFFER",
                offerPrice: offer.offerPrice,
            };

            // Gửi tin nhắn và cập nhật thông tin chat
            const chatRef = db.collection("chats").doc(chatId);
            await chatRef.collection("messages").add(offerMessage);
            await chatRef.update({
                lastMessage: offerMessage.text,
                timestamp: offerMessage.timestamp,
                lastMessageSenderId: buyerId,
            });

            console.log(`Đã gửi tin nhắn đề nghị vào chat ${chatId} thành công.`);
            return { success: true };

        } catch (error) {
            console.error("Lỗi nghiêm trọng khi gửi tin nhắn đề nghị vào chat:", error);
            return { success: false, error: error.message };
        }
    });

/**
 * Lấy lịch sử giao dịch Stripe cho người dùng đã xác thực.
 */
exports.getPaymentHistory = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Yêu cầu cần được xác thực.");
    }
    const userId = context.auth.uid;

    try {
        const customerId = await getOrCreateCustomer(userId);

        const paymentIntents = await stripe.paymentIntents.list({
            customer: customerId,
            limit: 50,
        });

        // Định dạng lại dữ liệu trả về
        const history = paymentIntents.data.map((pi) => ({
            id: pi.id,
            description: pi.description || "Thanh toán cho sản phẩm",
            amount: pi.amount,
            currency: pi.currency,
            status: pi.status,
            created: pi.created,
        }));

        // Luôn trả về một object có trường `success`
        return { success: true, history: history };

    } catch (error) {
        console.error("Lỗi khi lấy lịch sử thanh toán Stripe:", error);
        // Trả về cấu trúc lỗi nhất quán
        return { success: false, error: error.message };
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

/**
 * TRIGGER: Tự động tạo thông báo xác nhận cho người bán
 * khi họ đăng một tin mới thành công.
 */
exports.sendNewListingConfirmation = functions.firestore
    .document("listings/{listingId}")
    .onCreate(async (snap, context) => {
        // 1. Lấy dữ liệu của tin đăng vừa được tạo
        const listing = snap.data();
        if (!listing) {
            console.log("Không có dữ liệu tin đăng, bỏ qua.");
            return null;
        }

        // 2. Lấy ID của người bán từ tin đăng
        const sellerId = listing.sellerId;
        if (!sellerId) {
            console.log("Tin đăng không có sellerId, bỏ qua.");
            return null;
        }

        // 3. Tạo nội dung cho thông báo
        const notificationPayload = {
            userId: sellerId, // Thông báo này dành cho người bán
            title: "Đăng tin thành công!",
            content: `Tin đăng "${listing.title || "sản phẩm của bạn"}" đã được đăng thành công và đang hiển thị.`,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            type: "LISTING", // Loại thông báo liên quan đến tin đăng
            category: "Tin đăng", // Category để lọc trên app
            read: false, // Mặc định là chưa đọc
            relatedId: context.params.listingId, // ID của tin đăng liên quan
            // senderId có thể bỏ trống vì đây là thông báo hệ thống
        };

        // 4. Ghi thông báo mới vào collection "notifications"
        console.log(`Đang tạo thông báo đăng tin thành công cho người dùng: ${sellerId}`);
        return admin.firestore().collection("notifications").add(notificationPayload);
    });

    /**
     * TRIGGER: Cập nhật số liệu thống kê của người dùng khi một tin đăng thay đổi.
     * - Tăng/giảm activeListingsCount.
     * - Tăng/giảm completedSalesCount.
     */
    exports.updateUserStatsOnListingChange = functions.firestore
        .document("listings/{listingId}")
        .onWrite(async (change, context) => {
            const listingBefore = change.before.data();
            const listingAfter = change.after.data();
            const db = admin.firestore();

            // Trường hợp 1: Tin đăng mới được TẠO
            if (!change.before.exists && change.after.exists) {
                if (listingAfter.status === "available") {
                    const userRef = db.collection("users").doc(listingAfter.sellerId);
                    console.log(`Tăng activeListingsCount cho user: ${listingAfter.sellerId}`);
                    return userRef.update({
                        activeListingsCount: admin.firestore.FieldValue.increment(1),
                    });
                }
                return null;
            }

            // Trường hợp 2: Tin đăng bị XÓA
            if (change.before.exists && !change.after.exists) {
                const userRef = db.collection("users").doc(listingBefore.sellerId);
                let updates = {};
                if (listingBefore.status === "available") {
                    updates.activeListingsCount = admin.firestore.FieldValue.increment(-1);
                }
                if (listingBefore.status === "sold") {
                    updates.completedSalesCount = admin.firestore.FieldValue.increment(-1);
                }
                if (Object.keys(updates).length > 0) {
                     console.log(`Giảm stats cho user: ${listingBefore.sellerId}`);
                    return userRef.update(updates);
                }
                return null;
            }

            // Trường hợp 3: Tin đăng được CẬP NHẬT
            if (change.before.exists && change.after.exists) {
                if (listingBefore.status === listingAfter.status) {
                    return null; // Trạng thái không đổi, không làm gì cả
                }

                const userRef = db.collection("users").doc(listingAfter.sellerId);
                const updates = {};

                // Chuyển từ available -> sold
                if (listingBefore.status === "available" && listingAfter.status === "sold") {
                    updates.activeListingsCount = admin.firestore.FieldValue.increment(-1);
                    updates.completedSalesCount = admin.firestore.FieldValue.increment(1);
                }
                // Chuyển từ sold -> available (trường hợp hiếm)
                else if (listingBefore.status === "sold" && listingAfter.status === "available") {
                    updates.activeListingsCount = admin.firestore.FieldValue.increment(1);
                    updates.completedSalesCount = admin.firestore.FieldValue.increment(-1);
                }
                // Các trường hợp chuyển sang/khỏi 'paused'
                // ... (bạn có thể thêm logic cho 'paused' nếu cần)

                if (Object.keys(updates).length > 0) {
                    console.log(`Cập nhật stats cho user: ${listingAfter.sellerId}`);
                    return userRef.update(updates);
                }
            }

            return null;
        });
/**
 * (TÙY CHỌN) Hàm HTTPS Callable để chạy một lần, dùng để đồng bộ lại
 * số liệu cho tất cả người dùng hiện có.
 */
exports.recalculateAllUserStats = functions.https.onCall(async (data, context) => {
    // Bảo mật: Nếu cần, hãy bỏ comment dòng dưới để chỉ admin mới được chạy
    // await verifyAdmin(context);

    const db = admin.firestore();
    const usersSnapshot = await db.collection("users").get();

    for (const userDoc of usersSnapshot.docs) {
        const userId = userDoc.id;

        const activeListingsQuery = db.collection("listings")
            .where("sellerId", "==", userId)
            .where("status", "==", "available");

        const soldListingsQuery = db.collection("listings")
            .where("sellerId", "==", userId)
            .where("status", "==", "sold");

        const activeCount = (await activeListingsQuery.get()).size;
        const soldCount = (await soldListingsQuery.get()).size;

        await userDoc.ref.update({
            activeListingsCount: activeCount,
            completedSalesCount: soldCount,
        });
        console.log(`Đã cập nhật stats cho user ${userId}: Active=${activeCount}, Sold=${soldCount}`);
    }

    return { success: true, message: `Đã cập nhật lại số liệu cho ${usersSnapshot.size} người dùng.` };
});
/**
 * Cloud Function: Lấy đề xuất cá nhân hóa cho người dùng.
 */
exports.getPersonalizedRecommendations = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Yêu cầu cần được xác thực.");
    }
    const userId = context.auth.uid;
    const db = admin.firestore();

    try {
        // 1. Lấy 5 danh mục mà người dùng xem gần đây nhất
        const historySnapshot = await db.collection("users").doc(userId).collection("viewHistory")
            .orderBy("viewedAt", "desc")
            .limit(10) // Lấy 10 sản phẩm gần nhất để đa dạng hóa danh mục
            .get();

        if (historySnapshot.empty) {
            console.log(`Không có lịch sử xem cho user ${userId}, trả về mảng rỗng.`);
            return { recommendations: [] };
        }

        const viewedListingIds = historySnapshot.docs.map((doc) => doc.id);
        const recentCategories = [...new Set(historySnapshot.docs.map((doc) => doc.data().categoryId))].slice(0, 5);

        if (recentCategories.length === 0) {
             return { recommendations: [] };
        }

        // 2. Tìm các sản phẩm phổ biến trong các danh mục đó, loại trừ những sản phẩm đã xem
        const listingsQuery = db.collection("listings")
            .where("status", "==", "available")
            .where("category", "in", recentCategories)
            .where(admin.firestore.FieldPath.documentId(), "not-in", viewedListingIds)
            .orderBy(admin.firestore.FieldPath.documentId()) // Firestore yêu cầu orderBy khi dùng not-in
            .limit(20); // Lấy nhiều hơn để sau đó sắp xếp theo views

        const listingsSnapshot = await listingsQuery.get();

        let recommendations = listingsSnapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() }));

        // 3. Sắp xếp lại theo lượt xem ở phía server
        recommendations.sort((a, b) => (b.views || 0) - (a.views || 0));

        // Trả về 10 đề xuất hàng đầu
        return { recommendations: recommendations.slice(0, 10) };
    } catch (error) {
        console.error("Lỗi khi lấy đề xuất cá nhân hóa:", error);
        throw new functions.https.HttpsError("internal", "Không thể lấy đề xuất.");
    }
});
/**
 * TRIGGER: Tự động tăng chatCount trên một listing khi có cuộc trò chuyện mới.
 */
exports.updateListingOnNewChat = functions.firestore
    .document("chats/{chatId}")
    .onCreate(async (snap, context) => {
        const chatData = snap.data();
        const listingId = chatData.listingId;

        // BƯỚC SỬA ĐỔI: Chỉ thực hiện nếu cuộc trò chuyện này được tạo từ một tin đăng
        if (!listingId) {
            console.log(`Chat ${context.params.chatId} được tạo mà không có listingId, bỏ qua việc cập nhật chatCount.`);
            return null; // Kết thúc hàm một cách an toàn
        }

        const listingRef = admin.firestore().collection("listings").doc(listingId);

        try {
            // Sử dụng FieldValue.increment để tăng giá trị một cách an toàn
            await listingRef.update({
                chatCount: admin.firestore.FieldValue.increment(1),
            });
            console.log(`Đã tăng chatCount cho listing ${listingId}`);
            return { success: true };
        } catch (error) {
            console.error(
                `Lỗi khi tăng chatCount cho listing ${listingId}:`,
                error
            );
            return { success: false, error: error.message };
        }
    });
/**
 * TRIGGER: Tự động tăng lượt xem cho một tin đăng một cách thông minh.
 * Chỉ tăng lượt xem nếu người dùng chưa xem tin này trong vòng 30 phút qua.
 */
exports.incrementViewCountOnNewView = functions.firestore
    .document("users/{userId}/viewHistory/{listingId}")
    .onWrite(async (change, context) => {
        const VIEW_TIMEOUT_MINUTES = 30; // Khoảng thời gian chờ (phút)

        // Lấy dữ liệu trước và sau khi ghi
        const dataAfter = change.after.data();
        const dataBefore = change.before.data();

        // Nếu document bị xóa, không làm gì cả
        if (!change.after.exists) {
            return null;
        }

        // Trường hợp 1: Lần xem đầu tiên (document chưa tồn tại trước đó)
        if (!change.before.exists) {
            console.log(`Lần xem đầu tiên cho listing ${context.params.listingId} bởi user ${context.params.userId}. Tăng view.`);
            const listingRef = admin.firestore().collection("listings").doc(context.params.listingId);
            return listingRef.update({ views: admin.firestore.FieldValue.increment(1) });
        }

        // Trường hợp 2: Người dùng xem lại sản phẩm
        const previousViewTime = dataBefore.viewedAt.toDate();
        const newViewTime = dataAfter.viewedAt.toDate();
        const diffMillis = newViewTime.getTime() - previousViewTime.getTime();
        const minutesSinceLastView = diffMillis / (1000 * 60);

        console.log(`User ${context.params.userId} xem lại listing ${context.params.listingId}. Lần xem trước cách đây ${minutesSinceLastView.toFixed(2)} phút.`);

        // Chỉ tăng lượt xem nếu đã qua khoảng thời gian chờ
        if (minutesSinceLastView >= VIEW_TIMEOUT_MINUTES) {
            console.log(`Đã quá ${VIEW_TIMEOUT_MINUTES} phút. Tăng view.`);
            const listingRef = admin.firestore().collection("listings").doc(context.params.listingId);
            return listingRef.update({ views: admin.firestore.FieldValue.increment(1) });
        } else {
            console.log(`Chưa đủ ${VIEW_TIMEOUT_MINUTES} phút. Không tăng view.`);
            return null; // Không làm gì cả
        }
    });
/**
 * TRIGGER: Hoàn tất giao dịch cho đơn hàng "Mua ngay" (tiền mặt).
 * Được gọi từ client khi người dùng xác nhận mua bằng tiền mặt.
 */
exports.processCashOnDeliveryOrder = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Yêu cầu cần được xác thực.");
    }

    const buyerId = context.auth.uid;
    const { listingId } = data;

    if (!listingId) {
        throw new functions.https.HttpsError("invalid-argument", "Thiếu listingId.");
    }

    const db = admin.firestore();
    const listingRef = db.collection("listings").doc(listingId);
    const buyerRef = db.collection("users").doc(buyerId);
    const transactionRef = db.collection("transactions").doc(); // Tạo ID mới

    try {
        await db.runTransaction(async (t) => {
            const listingDoc = await t.get(listingRef);
            const buyerDoc = await t.get(buyerRef);

            if (!listingDoc.exists) {
                throw new functions.https.HttpsError("not-found", "Không tìm thấy tin đăng.");
            }
            if (!buyerDoc.exists) {
                throw new functions.https.HttpsError("not-found", "Không tìm thấy người mua.");
            }

            const listingData = listingDoc.data();
            const buyerData = buyerDoc.data();

            if (listingData.status !== "available") {
                throw new functions.https.HttpsError("failed-precondition", "Sản phẩm không còn khả dụng.");
            }
            if (listingData.sellerId === buyerId) {
                 throw new functions.https.HttpsError("failed-precondition", "Bạn không thể tự mua sản phẩm của mình.");
            }

            // 1. Cập nhật trạng thái tin đăng thành "sold"
            t.update(listingRef, { status: "sold" });

            // 2. Tạo bản ghi giao dịch mới
            const newTransaction = {
                id: transactionRef.id,
                listingId: listingId,
                listingTitle: listingData.title,
                listingImageUrl: listingData.imageUrls ? listingData.imageUrls[0] : null,
                sellerId: listingData.sellerId,
                sellerName: listingData.sellerName,
                buyerId: buyerId,
                buyerName: buyerData.name,
                finalPrice: listingData.price,
                sellerReviewed: false,
                buyerReviewed: false,
                transactionDate: admin.firestore.FieldValue.serverTimestamp(),
                paymentMethod: "cash" // Ghi lại phương thức thanh toán
            };
            t.set(transactionRef, newTransaction);
        });

        console.log(`Giao dịch tiền mặt ${transactionRef.id} được tạo thành công cho listing ${listingId}.`);
        return { success: true, transactionId: transactionRef.id };

    } catch (error) {
        console.error(`Lỗi khi xử lý giao dịch tiền mặt cho listing ${listingId}:`, error);
        throw new functions.https.HttpsError("internal", "Không thể xử lý đơn hàng.", error);
    }
});