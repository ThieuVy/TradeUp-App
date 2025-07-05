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

  // Nếu đã có stripeCustomerId trong Firestore, trả về nó
  if (userData && userData.stripeCustomerId) {
    return userData.stripeCustomerId;
  }

  // Nếu chưa có, tạo một Customer mới trên Stripe
  const customer = await stripe.customers.create({
    // Gán email của user vào customer trên Stripe để dễ quản lý
    email: userData.email,
    // Metadata giúp liên kết customer này với Firebase user ID
    metadata: {firebaseUID: userId},
  });

  // Lưu stripeCustomerId mới vào Firestore cho lần sau
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
  // Kiểm tra xem người dùng đã xác thực chưa
  if (!context.auth) {
    throw new functions.https.HttpsError(
        "unauthenticated",
        "The function must be called while authenticated.",
    );
  }

  const userId = context.auth.uid;
  const customerId = await getOrCreateCustomer(userId);

  // Tạo SetupIntent, nó sẽ cho phép lưu thẻ mà không cần thu tiền ngay
  const setupIntent = await stripe.setupIntents.create({
      customer: customerId,
      // *** THÊM PHẦN NÀY VÀO ***
      // Chỉ định các loại phương thức thanh toán bạn muốn hỗ trợ.
      // 'card' là thẻ tín dụng/ghi nợ.
      // Thêm các loại khác nếu cần, ví dụ: 'klarna', 'afterpay_clearpay'
      // Đây là cách thay thế cho `allowsDelayedPaymentMethods`
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

  // Stripe API version phải khớp với version mà Stripe SDK trên Android đang dùng
  const apiVersion = data.apiVersion;

  const key = await stripe.ephemeralKeys.create(
      {customer: customerId},
      {apiVersion: apiVersion},
  );

  return {
    ephemeralKey: key.secret,
  };
});