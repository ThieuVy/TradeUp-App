
package com.example.testapptradeup.repositories;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.PagedResult;
import com.example.testapptradeup.models.SearchParams;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ListingRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    public static final int PAGE_SIZE = 15;
    private Object lastVisible;

    public LiveData<List<Listing>> getFeaturedListings() {
        MutableLiveData<List<Listing>> data = new MutableLiveData<>();
        db.collection("listings")
                .whereEqualTo("isFeatured", true) // Giả sử có trường isFeatured
                .whereEqualTo("status", "available")
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Listing> listings = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        listings.add(doc.toObject(Listing.class));
                    }
                    data.setValue(listings);
                })
                .addOnFailureListener(e -> data.setValue(null));
        return data;
    }

    public LiveData<List<Listing>> getRecommendedListings(int limit) {
        MutableLiveData<List<Listing>> data = new MutableLiveData<>();
        db.collection("listings")
                .whereEqualTo("status", "available")
                .orderBy("views", Query.Direction.DESCENDING) // Ví dụ: Đề xuất theo lượt xem
                .limit(limit)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Listing> listings = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        listings.add(doc.toObject(Listing.class));
                    }
                    data.setValue(listings);
                })
                .addOnFailureListener(e -> data.setValue(null));
        return data;
    }

    public LiveData<List<Listing>> getRecentListings(int limit) {
        MutableLiveData<List<Listing>> data = new MutableLiveData<>();
        db.collection("listings")
                .whereEqualTo("status", "available")
                .orderBy("timePosted", Query.Direction.DESCENDING) // Sắp xếp theo mới nhất
                .limit(limit)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Listing> listings = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        listings.add(doc.toObject(Listing.class));
                    }
                    data.setValue(listings);
                })
                .addOnFailureListener(e -> data.setValue(null));
        return data;
    }

    public LiveData<PagedResult<Listing>> getMyListings(String userId, @Nullable DocumentSnapshot lastVisible) {
        MutableLiveData<PagedResult<Listing>> resultLiveData = new MutableLiveData<>();

        if (userId == null || userId.isEmpty()) {
            resultLiveData.setValue(new PagedResult<>(new ArrayList<>(), null, new IllegalArgumentException("User ID is null or empty.")));
            return resultLiveData;
        }

        Query query = db.collection("listings")
                .whereEqualTo("sellerId", userId)
                .orderBy("timePosted", Query.Direction.DESCENDING);

        if (lastVisible != null) {
            query = query.startAfter(lastVisible);
        }

        query.limit(PAGE_SIZE).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Listing> listings = new ArrayList<>();
                    DocumentSnapshot newLastVisible = null;
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        newLastVisible = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Listing listing = doc.toObject(Listing.class);
                            listing.setId(doc.getId());
                            listings.add(listing);
                        }
                    }
                    resultLiveData.setValue(new PagedResult<>(listings, newLastVisible, null));
                })
                .addOnFailureListener(e -> resultLiveData.setValue(new PagedResult<>(null, null, e)));

        return resultLiveData;
    }

    public LiveData<List<Listing>> getListingsByIds(List<String> listingIds) {
        MutableLiveData<List<Listing>> listingsData = new MutableLiveData<>();
        if (listingIds == null || listingIds.isEmpty()) {
            listingsData.setValue(new ArrayList<>());
            return listingsData;
        }

        db.collection("listings")
                .whereIn(FieldPath.documentId(), listingIds)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Listing> listings = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Listing listing = document.toObject(Listing.class);
                        listing.setId(document.getId());
                        listings.add(listing);
                    }
                    listingsData.setValue(listings);
                }).addOnFailureListener(e -> listingsData.setValue(null));
        return new MutableLiveData<>();
    }

    public LiveData<Boolean> deleteListing(String listingId) {
        MutableLiveData<Boolean> deleteStatus = new MutableLiveData<>();
        db.collection("listings").document(listingId).delete()
                .addOnSuccessListener(aVoid -> deleteStatus.setValue(true))
                .addOnFailureListener(e -> deleteStatus.setValue(false));
        return new MutableLiveData<>();
    }
    public LiveData<Listing> getListingById(String listingId) {
        // ... Logic để lấy một document listing từ Firestore bằng ID
        return null;
    }

    // Phương thức mới để lấy danh sách sản phẩm theo bộ lọc
    public LiveData<List<Listing>> getListingsByFilter(String filterType, @Nullable String categoryId) {
        MutableLiveData<List<Listing>> listingsData = new MutableLiveData<>();

        Query query = db.collection("listings").whereEqualTo("status", "available");

        switch (filterType) {
            case "recommended":
                query = query.orderBy("views", Query.Direction.DESCENDING);
                break;
            case "category":
                if (categoryId != null && !categoryId.isEmpty()) {
                    query = query.whereEqualTo("categoryId", categoryId)
                            .orderBy("timePosted", Query.Direction.DESCENDING);
                }
                break;
            case "recent":
            default:
                query = query.orderBy("timePosted", Query.Direction.DESCENDING);
                break;
        }

        // Có thể thêm .limit() nếu muốn giới hạn kết quả ban đầu
        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            // ... (Logic chuyển đổi snapshots thành List<Listing> và post lên LiveData)
        }).addOnFailureListener(e -> listingsData.setValue(null));

        return listingsData;
    }
    public LiveData<List<Listing>> getActiveListingsByUser(String userId, int limit) {
        MutableLiveData<List<Listing>> data = new MutableLiveData<>();
        if (userId == null || userId.isEmpty()) {
            data.setValue(new ArrayList<>());
            return data;
        }

        db.collection("listings")
                .whereEqualTo("sellerId", userId)
                .whereEqualTo("status", "available")
                .orderBy("timePosted", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null) {
                        // Trực tiếp chuyển đổi kết quả thành List<Listing>
                        List<Listing> listings = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Listing listing = doc.toObject(Listing.class);
                            listing.setId(doc.getId()); // Đừng quên set ID
                            listings.add(listing);
                        }
                        data.setValue(listings);
                    } else {
                        data.setValue(new ArrayList<>());
                    }
                })
                .addOnFailureListener(e -> {
                    // Xử lý lỗi, trả về danh sách rỗng hoặc null tùy vào yêu cầu
                    data.setValue(new ArrayList<>());
                });
        return data;
    }

    public LiveData<PagedResult<Listing>> searchListings(SearchParams params, @Nullable DocumentSnapshot lastVisible) {
        MutableLiveData<PagedResult<Listing>> resultLiveData = new MutableLiveData<>();
        Query query = db.collection("listings").whereEqualTo("status", "available");

        // ========== SỬA LỖI Ở ĐÂY ==========
        // QUAN TRỌNG: Phải orderBy TRƯỚC khi dùng startAt/endAt
        if (params.getQuery() != null && !params.getQuery().isEmpty()) {
            query = query.orderBy("title").startAt(params.getQuery()).endAt(params.getQuery() + '\uf8ff');
        }
        // ===================================

        if (params.getCategory() != null && !params.getCategory().isEmpty()) {
            query = query.whereEqualTo("categoryId", params.getCategory());
        }
        if (params.getMinPrice() != null && params.getMinPrice() > 0) {
            query = query.whereGreaterThanOrEqualTo("price", params.getMinPrice());
        }
        if (params.getMaxPrice() != null && params.getMaxPrice() > 0) {
            query = query.whereLessThanOrEqualTo("price", params.getMaxPrice());
        }
        if (params.getCondition() != null && !params.getCondition().isEmpty()) {
            query = query.whereEqualTo("condition", params.getCondition());
        }

        if (params.getSortBy() != null && !params.getSortBy().isEmpty()) {
            // Tránh lỗi khi orderBy("title") đã được gọi ở trên
            if (!params.getSortBy().equals("title")) {
                Query.Direction direction = params.isSortAscending() ? Query.Direction.ASCENDING : Query.Direction.DESCENDING;
                query = query.orderBy(params.getSortBy(), direction);
            }
        } else if (params.getQuery() == null || params.getQuery().isEmpty()) {
            query = query.orderBy("timePosted", Query.Direction.DESCENDING);
        }

        if (lastVisible != null) {
            query = query.startAfter(lastVisible);
        }

        query.limit(PAGE_SIZE).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // TODO: Xử lý lọc khoảng cách ở phía client nếu cần
                    // Firestore không hỗ trợ truy vấn geo-location phức tạp trực tiếp
                    // Bạn cần lấy tất cả kết quả trong một vùng lớn rồi lọc lại trên client
                    // Hoặc sử dụng dịch vụ như GeoFire.

                    List<Listing> listings = new ArrayList<>();
                    DocumentSnapshot newLastVisible = null;
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        newLastVisible = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Listing listing = doc.toObject(Listing.class);
                            listing.setId(doc.getId());
                            listings.add(listing);
                        }
                    }
                    resultLiveData.setValue(new PagedResult<>(listings, newLastVisible, null));
                })
                .addOnFailureListener(e -> {
                    Log.e("ListingRepository", "Lỗi truy vấn Firestore. Có thể bạn cần tạo Composite Index trong Firebase Console. Lỗi: " + e.getMessage(), e);
                    resultLiveData.setValue(new PagedResult<>(null, null, e));
                });
        return resultLiveData;
    }
}