package com.example.testapptradeup.repositories;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.PagedResult;
import com.example.testapptradeup.models.SearchParams;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryBounds;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ListingRepository {
    private static final String TAG = "ListingRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    public static final int PAGE_SIZE = 15;

    private final MutableLiveData<List<Listing>> featuredListingsData = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<Listing>> recommendedListingsData = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<Listing>> recentListingsData = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading() { return _isLoading; }
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() { return _errorMessage; }

    public ListingRepository() {
        fetchAll();
    }

    public void fetchAll() {
        if (Boolean.TRUE.equals(_isLoading.getValue())) {
            return;
        }
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        final AtomicInteger tasksToComplete = new AtomicInteger(3);
        Runnable onTaskCompleted = () -> {
            if (tasksToComplete.decrementAndGet() == 0) {
                _isLoading.postValue(false);
            }
        };

        db.collection("listings")
                .whereEqualTo("isFeatured", true).whereEqualTo("status", "available").limit(5)
                .get()
                .addOnSuccessListener(result -> featuredListingsData.setValue(result.toObjects(Listing.class)))
                .addOnFailureListener(e -> _errorMessage.setValue("Lỗi tải mục Nổi bật"))
                .addOnCompleteListener(task -> onTaskCompleted.run());

        db.collection("listings")
                .whereEqualTo("status", "available").orderBy("views", Query.Direction.DESCENDING).limit(4)
                .get()
                .addOnSuccessListener(result -> recommendedListingsData.setValue(result.toObjects(Listing.class)))
                .addOnFailureListener(e -> _errorMessage.setValue("Lỗi tải mục Đề xuất"))
                .addOnCompleteListener(task -> onTaskCompleted.run());

        db.collection("listings")
                .whereEqualTo("status", "available").orderBy("timePosted", Query.Direction.DESCENDING).limit(10)
                .get()
                .addOnSuccessListener(result -> {
                    List<Listing> listings = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : result) {
                        Listing listing = doc.toObject(Listing.class);
                        listing.setId(doc.getId());
                        listings.add(listing);
                    }
                    recentListingsData.setValue(listings);
                })
                .addOnFailureListener(e -> _errorMessage.setValue("Lỗi tải tin gần đây"))
                .addOnCompleteListener(task -> onTaskCompleted.run());
    }

    /**
     * Thêm một tin đăng mới vào đầu danh sách "Tin rao gần đây" đang được hiển thị.
     * Thao tác này chỉ diễn ra ở phía client để cập nhật UI ngay lập tức.
     * @param newListing Tin đăng mới cần thêm.
     */
    public void prependLocalListing(Listing newListing) {
        // Lấy danh sách hiện tại từ LiveData.
        List<Listing> currentList = recentListingsData.getValue();
        if (currentList == null) {
            currentList = new ArrayList<>();
        }

        // Tạo một danh sách mới để LiveData có thể nhận biết sự thay đổi.
        List<Listing> updatedList = new ArrayList<>(currentList);

        // Thêm mục mới vào vị trí đầu tiên (index 0).
        updatedList.add(0, newListing);

        // Cập nhật LiveData với danh sách mới.
        recentListingsData.setValue(updatedList);
    }

    public LiveData<PagedResult<Listing>> searchListings(SearchParams params, @Nullable DocumentSnapshot lastVisible) {
        MutableLiveData<PagedResult<Listing>> resultLiveData = new MutableLiveData<>();

        if (params.getUserLocation() != null && params.getMaxDistance() > 0) {
            performGeoQuery(params, resultLiveData);
        } else {
            performStandardQuery(params, lastVisible, resultLiveData);
        }
        return resultLiveData;
    }

    private void performStandardQuery(SearchParams params, @Nullable DocumentSnapshot lastVisible, MutableLiveData<PagedResult<Listing>> resultLiveData) {
        Query query = db.collection("listings").whereEqualTo("status", "available");

        // (Code query cũ giữ nguyên)
        if (params.getCategory() != null && !params.getCategory().isEmpty()) {
            query = query.whereEqualTo("categoryId", params.getCategory());
        }
        if (params.getCondition() != null && !params.getCondition().isEmpty()) {
            query = query.whereEqualTo("condition", params.getCondition());
        }
        if (params.getMinPrice() != null) {
            query = query.whereGreaterThanOrEqualTo("price", params.getMinPrice());
        }
        if (params.getMaxPrice() != null) {
            query = query.whereLessThanOrEqualTo("price", params.getMaxPrice());
        }
        if (params.getQuery() != null && !params.getQuery().isEmpty()) {
            query = query.orderBy("title").startAt(params.getQuery()).endAt(params.getQuery() + '\uf8ff');
        }

        if (params.getSortBy() != null && !params.getSortBy().isEmpty()) {
            if (params.hasPriceFilter() && !params.getSortBy().equals("price")) {
                Log.w("ListingRepository", "Không thể sắp xếp theo trường khác khi đang lọc theo khoảng giá. Bỏ qua sắp xếp.");
            } else {
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
                    Log.e("ListingRepository", "Lỗi truy vấn Firestore: " + e.getMessage(), e);
                    resultLiveData.setValue(new PagedResult<>(null, null, e));
                });
    }

    private void performGeoQuery(SearchParams params, MutableLiveData<PagedResult<Listing>> resultLiveData) {
        final GeoLocation center = new GeoLocation(params.getUserLocation().getLatitude(), params.getUserLocation().getLongitude());
        final double radiusInM = params.getMaxDistance() * 1000.0;

        // <<< SỬA LỖI Ở ĐÂY: Thay đổi tên phương thức >>>
        List<GeoQueryBounds> bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInM);
        final List<Task<QuerySnapshot>> tasks = new ArrayList<>();

        for (GeoQueryBounds b : bounds) {
            Query q = db.collection("listings")
                    .orderBy("geohash")
                    .startAt(b.startHash)
                    .endAt(b.endHash);

            if (params.getCategory() != null && !params.getCategory().isEmpty()) {
                q = q.whereEqualTo("categoryId", params.getCategory());
            }
            if (params.getCondition() != null && !params.getCondition().isEmpty()) {
                q = q.whereEqualTo("condition", params.getCondition());
            }

            tasks.add(q.get());
        }

        Tasks.whenAllSuccess(tasks).addOnSuccessListener(snapshots -> {
            List<Listing> matchingDocs = new ArrayList<>();
            for (Object snapshot : snapshots) {
                for (DocumentSnapshot doc : ((QuerySnapshot)snapshot).getDocuments()) {
                    Listing listing = doc.toObject(Listing.class);
                    if (listing != null && listing.getLatitude() != 0 && listing.getLongitude() != 0) {
                        listing.setId(doc.getId());
                        GeoLocation docLocation = new GeoLocation(listing.getLatitude(), listing.getLongitude());
                        double distanceInM = GeoFireUtils.getDistanceBetween(docLocation, center);
                        if (distanceInM <= radiusInM) {
                            matchingDocs.add(listing);
                        }
                    }
                }
            }

            // Lọc giá ở client-side nếu có
            if (params.hasPriceFilter()) {
                matchingDocs.removeIf(listing ->
                        (params.getMinPrice() != null && listing.getPrice() < params.getMinPrice()) ||
                                (params.getMaxPrice() != null && listing.getPrice() > params.getMaxPrice())
                );
            }

            // Sắp xếp theo khoảng cách
            matchingDocs.sort(Comparator.comparingDouble(l ->
                    GeoFireUtils.getDistanceBetween(new GeoLocation(l.getLatitude(), l.getLongitude()), center)
            ));

            resultLiveData.setValue(new PagedResult<>(matchingDocs, null, null));

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Lỗi Geo-query", e);
            resultLiveData.setValue(new PagedResult<>(null, null, e));
        });
    }

    public LiveData<List<Listing>> getRecentListings() {
        return recentListingsData;
    }

    public LiveData<List<Listing>> getFeaturedListings() {
        return featuredListingsData;
    }

    public LiveData<List<Listing>> getRecommendedListings(int limit) {
        return recommendedListingsData;
    }

    public void incrementViewCount(String listingId) {
        if (listingId == null || listingId.isEmpty()) {
            return;
        }
        db.collection("listings").document(listingId)
                .update("views", FieldValue.increment(1))
                .addOnFailureListener(e -> Log.w(TAG, "Lỗi khi tăng lượt xem cho tin đăng: " + listingId, e));
    }
}