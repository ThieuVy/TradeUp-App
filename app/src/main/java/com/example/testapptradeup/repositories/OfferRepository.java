package com.example.testapptradeup.repositories;

import android.util.Log;
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
import java.util.List;

public class OfferRepository {
    private static final String TAG = "OfferRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference offersCollection = db.collection("offers");
    private final CollectionReference listingsCollection = db.collection("listings");
    private final CollectionReference transactionsCollection = db.collection("transactions");

    /**
     * Tạo một đề nghị mới và tăng số lượng offers của tin đăng.
     * Sử dụng WriteBatch để đảm bảo tính toàn vẹn dữ liệu.
     * @param offer Đối tượng Offer cần tạo.
     * @return LiveData<Boolean> báo hiệu thành công/thất bại.
     */
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

    /**
     * Lấy tất cả đề nghị cho một tin đăng cụ thể.
     * @param listingId ID của tin đăng.
     * @return LiveData chứa danh sách Offer.
     */
    public LiveData<List<Offer>> getOffersForListing(String listingId) {
        MutableLiveData<List<Offer>> offersData = new MutableLiveData<>();
        offersCollection.whereEqualTo("listingId", listingId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    offersData.setValue(queryDocumentSnapshots.toObjects(Offer.class));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy offers: ", e);
                    offersData.setValue(null);
                });
        return offersData;
    }

    /**
     * Chấp nhận một đề nghị. Hành động này sẽ:
     * 1. Cập nhật trạng thái của đề nghị thành "accepted".
     * 2. Cập nhật trạng thái của tất cả đề nghị khác cho cùng tin đăng thành "rejected".
     * 3. Cập nhật trạng thái của tin đăng thành "sold".
     * 4. Tạo một document giao dịch mới.
     * @param offer Đề nghị được chấp nhận.
     * @param listing Tin đăng liên quan.
     * @return LiveData<Boolean> báo hiệu thành công/thất bại.
     */
    public LiveData<Boolean> acceptOffer(Offer offer, Listing listing) {
        MutableLiveData<Boolean> success = new MutableLiveData<>();

        offersCollection.whereEqualTo("listingId", offer.getListingId())
                .whereEqualTo("status", "pending") // Chỉ xử lý các offer đang chờ
                .get()
                .addOnSuccessListener(otherOffersSnapshot -> {
                    WriteBatch batch = db.batch();

                    // Cập nhật offer được chấp nhận
                    DocumentReference acceptedOfferRef = offersCollection.document(offer.getId());
                    batch.update(acceptedOfferRef, "status", "accepted");

                    // Cập nhật tin đăng thành "sold" và isSold = true
                    DocumentReference listingRef = listingsCollection.document(listing.getId());
                    batch.update(listingRef, "status", "sold", "isSold", true);

                    // Tạo document giao dịch mới
                    DocumentReference transactionRef = transactionsCollection.document();
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
                    newTransaction.setSellerReviewed(false);
                    newTransaction.setBuyerReviewed(false);
                    batch.set(transactionRef, newTransaction);

                    // Từ chối tất cả các offer khác
                    for (QueryDocumentSnapshot doc : otherOffersSnapshot) {
                        if (!doc.getId().equals(offer.getId())) {
                            batch.update(doc.getReference(), "status", "rejected");
                        }
                    }

                    batch.commit().addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Chấp nhận offer và xử lý giao dịch thành công.");
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
}