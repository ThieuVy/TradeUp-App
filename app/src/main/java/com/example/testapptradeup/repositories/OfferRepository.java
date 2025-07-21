package com.example.testapptradeup.repositories;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.Offer;
import com.example.testapptradeup.models.OfferWithListing;
import com.example.testapptradeup.models.Transaction;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class OfferRepository {
    private static final String TAG = "OfferRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference offersCollection = db.collection("offers");
    private final CollectionReference listingsCollection = db.collection("listings");
    private final CollectionReference transactionsCollection = db.collection("transactions");
    private final FirebaseFunctions functions = FirebaseFunctions.getInstance();

    public LiveData<Boolean> createOffer(Offer offer) {
        MutableLiveData<Boolean> success = new MutableLiveData<>();
        DocumentReference offerRef = offersCollection.document();
        offer.setId(offerRef.getId());

        // CHỈ THỰC HIỆN MỘT HÀNH ĐỘNG DUY NHẤT
        offerRef.set(offer)
                .addOnSuccessListener(aVoid -> success.setValue(true))
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
     * Chấp nhận một đề nghị.
     * Cập nhật trạng thái của offer và listing, đồng thời từ chối các offer khác.
     * @param offer Đề nghị được chấp nhận.
     * @return LiveData báo hiệu thành công/thất bại.
     */
    public LiveData<Boolean> acceptOffer(Offer offer) {
        MutableLiveData<Boolean> success = new MutableLiveData<>();

        offersCollection.whereEqualTo("listingId", offer.getListingId())
                .get()
                .addOnSuccessListener(otherOffersSnapshot -> {
                    WriteBatch batch = db.batch();

                    // 1. Cập nhật offer được chấp nhận
                    DocumentReference acceptedOfferRef = offersCollection.document(offer.getId());
                    batch.update(acceptedOfferRef, "status", "accepted");

                    // 2. === SỬA ĐỔI QUAN TRỌNG ===
                    // Cập nhật tin đăng thành "pending_payment" (chờ thanh toán), KHÔNG phải "sold"
                    DocumentReference listingRef = listingsCollection.document(offer.getListingId());
                    batch.update(listingRef, "status", "pending_payment");
                    // Không chuyển isSold thành true ở bước này

                    // 3. Từ chối tất cả các offer khác
                    for (QueryDocumentSnapshot doc : otherOffersSnapshot) {
                        if (!doc.getId().equals(offer.getId())) {
                            batch.update(doc.getReference(), "status", "rejected");
                        }
                    }

                    // 4. Thực thi tất cả các thao tác trong một lần
                    batch.commit().addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Chấp nhận offer và chuyển tin đăng sang trạng thái chờ thanh toán thành công.");
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

    /**
     * === HÀM MỚI ===
     * Hoàn tất giao dịch sau khi thanh toán thành công.
     * @param listingId ID của tin đăng.
     * @param acceptedOfferId ID của đề nghị đã được chấp nhận và thanh toán.
     * @return LiveData báo hiệu thành công/thất bại.
     */
    public LiveData<Boolean> completeTransaction(String listingId, String acceptedOfferId) {
        MutableLiveData<Boolean> success = new MutableLiveData<>();

        DocumentReference listingRef = listingsCollection.document(listingId);
        DocumentReference offerRef = offersCollection.document(acceptedOfferId);

        // Sử dụng get() để lấy dữ liệu một lần, đảm bảo dữ liệu mới nhất
        Tasks.whenAllSuccess(listingRef.get(), offerRef.get()).addOnSuccessListener(results -> {
            DocumentSnapshot listingSnapshot = (DocumentSnapshot) results.get(0);
            DocumentSnapshot offerSnapshot = (DocumentSnapshot) results.get(1);

            if (!listingSnapshot.exists() || !offerSnapshot.exists()) {
                Log.e(TAG, "Không tìm thấy tin đăng hoặc đề nghị để hoàn tất giao dịch.");
                success.setValue(false);
                return;
            }

            Listing listing = listingSnapshot.toObject(Listing.class);
            Offer offer = offerSnapshot.toObject(Offer.class);

            // Gán lại ID cho an toàn
            Objects.requireNonNull(listing).setId(listingSnapshot.getId());

            WriteBatch batch = db.batch();

            // 1. Cập nhật tin đăng thành "sold"
            batch.update(listingRef, "status", "sold", "isSold", true);

            // 2. Tạo một document giao dịch mới
            DocumentReference transactionRef = transactionsCollection.document();
            Transaction newTransaction = createTransactionFromData(Objects.requireNonNull(offer), listing, transactionRef);
            batch.set(transactionRef, newTransaction);

            // 3. Commit batch
            batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Hoàn tất giao dịch thành công cho listing: " + listingId);
                        success.setValue(true);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Lỗi khi commit batch hoàn tất giao dịch", e);
                        success.setValue(false);
                    });

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Lỗi khi lấy dữ liệu cho việc hoàn tất giao dịch", e);
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

        offersCollection.whereEqualTo("buyerId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(offerSnapshots -> {
                    if (offerSnapshots.isEmpty()) {
                        resultLiveData.setValue(new ArrayList<>());
                        return;
                    }

                    List<Offer> offers = offerSnapshots.toObjects(Offer.class);
                    List<String> listingIds = offers.stream()
                            .map(Offer::getListingId)
                            .distinct()
                            .collect(Collectors.toList());

                    if (listingIds.isEmpty()) {
                        resultLiveData.setValue(new ArrayList<>());
                        return;
                    }

                    listingsCollection.whereIn(FieldPath.documentId(), listingIds)
                            .get()
                            .addOnSuccessListener(listingSnapshots -> {
                                List<Listing> listings = listingSnapshots.toObjects(Listing.class);
                                List<OfferWithListing> combinedList = new ArrayList<>();
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
    private Transaction createTransactionFromData(Offer offer, Listing listing, DocumentReference transactionRef) {
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
        // transactionDate sẽ được tự động gán bởi @ServerTimestamp trên model
        return newTransaction;
    }
    /**
     * THÊM MỚI: Hoàn tất giao dịch cho trường hợp "Mua ngay".
     * @param listingId ID của tin đăng.
     * @return LiveData báo hiệu thành công/thất bại.
     */
    public LiveData<Boolean> completeTransactionForBuyNow(String listingId) {
        MutableLiveData<Boolean> successLiveData = new MutableLiveData<>();
        Map<String, Object> data = new HashMap<>();
        data.put("listingId", listingId);

        functions.getHttpsCallable("processCashOnDeliveryOrder")
                .call(data)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Lấy kết quả từ function
                        Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
                        if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                            successLiveData.postValue(true);
                        } else {
                            successLiveData.postValue(false);
                        }
                    } else {
                        Log.e(TAG, "Lỗi khi gọi processCashOnDeliveryOrder: ", task.getException());
                        successLiveData.postValue(false);
                    }
                });
        return successLiveData;
    }
}