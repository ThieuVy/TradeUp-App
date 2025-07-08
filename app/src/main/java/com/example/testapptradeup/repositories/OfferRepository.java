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

import java.util.List;

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

    public LiveData<Boolean> acceptOffer(Offer offer, Listing listing) {
        MutableLiveData<Boolean> success = new MutableLiveData<>();

        // Bước 1: Lấy tất cả các offer khác của tin đăng để từ chối chúng
        offersCollection.whereEqualTo("listingId", offer.getListingId())
                .get()
                .addOnSuccessListener(otherOffersSnapshot -> {
                    // Bước 2: Tạo một WriteBatch để thực hiện các thao tác ghi một cách nguyên tử
                    WriteBatch batch = db.batch();

                    // --- Thao tác 1: Cập nhật offer được chấp nhận ---
                    DocumentReference acceptedOfferRef = offersCollection.document(offer.getId());
                    batch.update(acceptedOfferRef, "status", "accepted");

                    // --- Thao tác 2: Cập nhật tin đăng thành "đã bán" ---
                    DocumentReference listingRef = listingsCollection.document(listing.getId());
                    batch.update(listingRef, "status", "sold", "sold", true);

                    // --- Thao tác 3: Tạo một document giao dịch mới ---
                    DocumentReference transactionRef = transactionsCollection.document();
                    Transaction newTransaction = getTransaction(offer, listing, transactionRef);
                    // Không cần set transactionDate, @ServerTimestamp sẽ tự động gán
                    batch.set(transactionRef, newTransaction);

                    // --- Thao tác 4: Từ chối tất cả các offer khác ---
                    for (QueryDocumentSnapshot doc : otherOffersSnapshot) {
                        // Chỉ từ chối những offer không phải là cái vừa được chấp nhận
                        if (!doc.getId().equals(offer.getId())) {
                            batch.update(doc.getReference(), "status", "rejected");
                        }
                    }

                    // --- Bước 5: Commit toàn bộ batch ---
                    batch.commit().addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Chấp nhận offer và cập nhật các trạng thái liên quan thành công.");
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

    @NonNull
    private static Transaction getTransaction(Offer offer, Listing listing, DocumentReference transactionRef) {
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
        return newTransaction;
    }
}