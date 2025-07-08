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
 * Xóa tài khoản người dùng khỏi Authentication và Firestore.
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

            // (Tùy chọn nâng cao) Bước 3: Xóa các tin đăng của người dùng này
            // Sử dụng batch write để xóa nhiều document một cách hiệu quả.
            const listingsQuery = db.collection("listings").where("sellerId", "==", uid);
            const listingsSnapshot = await listingsQuery.get();

            const batch = db.batch();
            listingsSnapshot.forEach((doc) => {
                batch.delete(doc.ref);
            });
            await batch.commit();
            console.log(`Successfully deleted ${listingsSnapshot.size} listings for user:`, uid);

            // TODO: Có thể thêm logic để xóa các reviews, offers, chats... của người dùng này.

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