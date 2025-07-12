package com.example.testapptradeup.repositories;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.Offer;
import com.example.testapptradeup.models.Transaction;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.FieldPath;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.example.testapptradeup.models.OfferWithListing;

public class OfferRepository {
    private static final String TAG = "OfferRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference offersCollection = db.collection("offers");
    private final CollectionReference listingsCollection = db.collection("listings");
    private final CollectionReference transactionsCollection = db.collection("transactions");

    public LiveData<Boolean> createOffer(Offer offer) {
        MutableLiveData<Boolean> success = new MutableLiveData<>();
        WriteBatch batch = db.batch();

        DocumentReference offerRef = offersCollection.document();
        offer.setId(offerRef.getId());
        batch.set(offerRef, offer);

        DocumentReference listingRef = listingsCollection.document(offer.getListingId());
        batch.update(listingRef, "offersCount", FieldValue.increment(1));

        batch.commit().addOnSuccessListener(aVoid -> success.setValue(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi tạo offer: ", e);
                    success.setValue(false);
                });
        return success;
    }

    public LiveData<List<Offer>> getOffersForListing(String listingId) {
        MutableLiveData<List<Offer>> offersData = new MutableLiveData<>();
        offersCollection.whereEqualTo("listingId", listingId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> offersData.setValue(queryDocumentSnapshots.toObjects(Offer.class)))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy offers: ", e);
                    offersData.setValue(null);
                });
        return offersData;
    }

    /**
     * THÊM MỚI: Cập nhật trạng thái của một offer (dùng cho Reject, Counter, v.v.)
     * @param offerId ID của offer cần cập nhật.
     * @param status Trạng thái mới ("rejected", "countered", etc.).
     * @return LiveData báo hiệu thành công/thất bại.
     */
    public LiveData<Boolean> updateOfferStatus(String offerId, String status) {
        MutableLiveData<Boolean> success = new MutableLiveData<>();
        if (offerId == null || status == null) {
            success.setValue(false);
            return success;
        }
        offersCollection.document(offerId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> success.setValue(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi cập nhật trạng thái offer: ", e);
                    success.setValue(false);
                });
        return success;
    }

    /**
     * SỬA LỖI BUG-002: Chấp nhận một đề nghị và tạo một giao dịch bằng WriteBatch.
     * @param offer Đề nghị được chấp nhận.
     * @param listing Tin đăng liên quan.
     * @return LiveData báo hiệu thành công/thất bại.
     */
    public LiveData<Boolean> acceptOffer(Offer offer, Listing listing) {
        MutableLiveData<Boolean> success = new MutableLiveData<>();

        // 1. Truy vấn tất cả các offer khác của tin đăng này để từ chối chúng.
        offersCollection.whereEqualTo("listingId", offer.getListingId())
                .get()
                .addOnSuccessListener(otherOffersSnapshot -> {
                    WriteBatch batch = db.batch();

                    // 1. Cập nhật trạng thái offer
                    DocumentReference acceptedOfferRef = offersCollection.document(offer.getId());
                    batch.update(acceptedOfferRef, "status", "accepted"); // Giữ nguyên

                    // 2. Cập nhật tin đăng thành "pending_payment" thay vì "sold"
                    DocumentReference listingRef = listingsCollection.document(listing.getId());
                    // THAY ĐỔI DÒNG NÀY:
                    batch.update(listingRef, "status", "pending_payment");
                    // BỎ CÁC DÒNG NÀY:
                    // batch.update(listingRef, "status", "sold", "isSold", true);
                    // DocumentReference transactionRef = transactionsCollection.document();
                    // Transaction newTransaction = createTransactionFromOffer(offer, listing, transactionRef);
                    // batch.set(transactionRef, newTransaction);

                    // 3. Từ chối các offer khác (giữ nguyên)
                    for (QueryDocumentSnapshot doc : otherOffersSnapshot) {
                        if (!doc.getId().equals(offer.getId())) {
                            batch.update(doc.getReference(), "status", "rejected");
                        }
                    }


                    // 6. Thực thi tất cả các thao tác trong một lần
                    batch.commit().addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Chấp nhận offer và tạo giao dịch thành công.");
                        success.setValue(true);
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Lỗi khi commit batch chấp nhận offer", e);
                        success.setValue(false);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy danh sách các offer khác để từ chối", e);
                    success.setValue(false);
                });

        return success;
    }

    public LiveData<List<OfferWithListing>> getOffersSentByUserWithListingInfo(String userId) {
        MutableLiveData<List<OfferWithListing>> resultLiveData = new MutableLiveData<>();
        if (userId == null || userId.isEmpty()) {
            resultLiveData.setValue(new ArrayList<>());
            return resultLiveData;
        }

        // Bước 1: Lấy tất cả các offer do người dùng gửi
        offersCollection.whereEqualTo("buyerId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(offerSnapshots -> {
                    if (offerSnapshots.isEmpty()) {
                        resultLiveData.setValue(new ArrayList<>());
                        return;
                    }

                    List<Offer> offers = offerSnapshots.toObjects(Offer.class);

                    // Bước 2: Thu thập các listingId duy nhất
                    List<String> listingIds = offers.stream()
                            .map(Offer::getListingId)
                            .distinct()
                            .collect(Collectors.toList());

                    if (listingIds.isEmpty()) {
                        resultLiveData.setValue(new ArrayList<>());
                        return;
                    }

                    // Bước 3: Lấy thông tin các listing tương ứng
                    listingsCollection.whereIn(FieldPath.documentId(), listingIds)
                            .get()
                            .addOnSuccessListener(listingSnapshots -> {
                                List<Listing> listings = listingSnapshots.toObjects(Listing.class);
                                List<OfferWithListing> combinedList = new ArrayList<>();

                                // Bước 4: Kết hợp dữ liệu
                                for (Offer offer : offers) {
                                    listings.stream()
                                            .filter(l -> l.getId().equals(offer.getListingId()))
                                            .findFirst().ifPresent(correspondingListing -> combinedList.add(new OfferWithListing(offer, correspondingListing)));
                                }
                                resultLiveData.setValue(combinedList);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Lỗi khi lấy thông tin listing cho offers", e);
                                resultLiveData.setValue(null);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy offers của người dùng", e);
                    resultLiveData.setValue(null);
                });

        return resultLiveData;
    }

    /**
     * Helper method to create a Transaction object from an Offer and a Listing.
     */
    @NonNull
    private Transaction createTransactionFromOffer(Offer offer, Listing listing, DocumentReference transactionRef) {
        Transaction newTransaction = new Transaction();
        newTransaction.setId(transactionRef.getId());
        newTransaction.setListingId(listing.getId());
        newTransaction.setListingTitle(listing.getTitle());
        newTransaction.setListingImageUrl(listing.getPrimaryImageUrl());
        newTransaction.setSellerId(listing.getSellerId());
        newTransaction.setSellerName(listing.getSellerName());
        newTransaction.setBuyerId(offer.getBuyerId());
        newTransaction.setBuyerName(offer.getBuyerName());
        newTransaction.setFinalPrice(offer.getOfferPrice());
        newTransaction.setSellerReviewed(false); // Mặc định là chưa đánh giá
        newTransaction.setBuyerReviewed(false);  // Mặc định là chưa đánh giá
        // transactionDate sẽ được tự động gán bởi @ServerTimestamp trên model
        return newTransaction;
    }

    public LiveData<List<Offer>> getOffersSentByUser(String userId) {
        MutableLiveData<List<Offer>> offersData = new MutableLiveData<>();
        if (userId == null || userId.isEmpty()) {
            offersData.setValue(new ArrayList<>());
            return offersData;
        }

        offersCollection.whereEqualTo("buyerId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots ->
                        offersData.setValue(queryDocumentSnapshots.toObjects(Offer.class))
                )
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy offers của người dùng: ", e);
                    offersData.setValue(null);
                });
        return offersData;
    }
}