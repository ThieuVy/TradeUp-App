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
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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

        // Mỗi tác vụ bất đồng bộ đều có onCompleteListener gọi onTaskCompleted.run()
        // và onFailureListener để set _errorMessage
        // Ví dụ:
        db.collection("listings")
                .whereEqualTo("isFeatured", true).whereEqualTo("status", "available").limit(5)
                .get()
                .addOnSuccessListener(result -> featuredListingsData.setValue(result.toObjects(Listing.class)))
                .addOnFailureListener(e -> _errorMessage.setValue("Lỗi tải mục Nổi bật"))
                .addOnCompleteListener(task -> onTaskCompleted.run());

        db.collection("listings")
                .whereEqualTo("status", "available")
                .orderBy("views", Query.Direction.DESCENDING) // Sắp xếp theo lượt xem giảm dần
                .limit(4)
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

        // Ưu tiên GeoQuery nếu có đủ thông tin
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
        final GeoLocation center = new GeoLocation(Objects.requireNonNull(params.getUserLocation()).getLatitude(), params.getUserLocation().getLongitude());
        final double radiusInM = params.getMaxDistance() * 1000.0;

        List<GeoQueryBounds> bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInM);
        final List<Task<QuerySnapshot>> tasks = new ArrayList<>();

        for (GeoQueryBounds b : bounds) {
            Query q = db.collection("listings")
                    .orderBy("geohash")
                    .startAt(b.startHash)
                    .endAt(b.endHash);

            // Áp dụng các bộ lọc khác nếu có
            if (params.getCategory() != null && !params.getCategory().isEmpty()) q = q.whereEqualTo("categoryId", params.getCategory());
            if (params.getCondition() != null && !params.getCondition().isEmpty()) q = q.whereEqualTo("condition", params.getCondition());

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
                        // Lọc lại chính xác theo bán kính vì GeoHash chỉ là gần đúng
                        double distanceInM = GeoFireUtils.getDistanceBetween(docLocation, center);
                        if (distanceInM <= radiusInM) {
                            matchingDocs.add(listing);
                        }
                    }
                }
            }

            // Lọc giá ở client-side
            if (params.hasPriceFilter()) {
                matchingDocs.removeIf(listing ->
                        (params.getMinPrice() != null && listing.getPrice() < params.getMinPrice()) ||
                                (params.getMaxPrice() != null && listing.getPrice() > params.getMaxPrice())
                );
            }

            // Sắp xếp theo khoảng cách
            matchingDocs.sort(Comparator.comparingDouble(l -> GeoFireUtils.getDistanceBetween(new GeoLocation(l.getLatitude(), l.getLongitude()), center)));

            // TODO: Triển khai phân trang ở client cho GeoQuery nếu cần
            resultLiveData.setValue(new PagedResult<>(matchingDocs, null, null));

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Lỗi Geo-query", e);
            resultLiveData.setValue(new PagedResult<>(null, null, e));
        });
    }

    public LiveData<List<Listing>> getPrioritizedRecentListings(GeoLocation userLocation) {
        MutableLiveData<List<Listing>> prioritizedData = new MutableLiveData<>();

        // Lấy 50 tin mới nhất để có đủ dữ liệu để sắp xếp lại
        db.collection("listings")
                .whereEqualTo("status", "available")
                .orderBy("timePosted", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Listing> listings = snapshots.toObjects(Listing.class);
                    if (userLocation != null) {
                        // Sắp xếp lại danh sách: ưu tiên các tin gần nhất
                        listings.sort(Comparator.comparingDouble(l -> {
                            if (l.getLatitude() != 0 && l.getLongitude() != 0) {
                                return GeoFireUtils.getDistanceBetween(new GeoLocation(l.getLatitude(), l.getLongitude()), userLocation);
                            }
                            return Double.MAX_VALUE; // Đẩy các tin không có vị trí xuống cuối
                        }));
                    }
                    prioritizedData.setValue(listings);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy tin đăng ưu tiên vị trí", e);
                    // Trả về danh sách rỗng nếu có lỗi
                    prioritizedData.setValue(new ArrayList<>());
                });

        return prioritizedData;
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

    /**
     * Lấy thông tin chi tiết của một tin đăng bằng ID.
     * Dùng addSnapshotListener để dữ liệu tự động cập nhật nếu có thay đổi từ server.
     */
    public LiveData<Listing> getListingById(String listingId) {
        MutableLiveData<Listing> listingData = new MutableLiveData<>();
        if (listingId == null || listingId.isEmpty()) {
            listingData.setValue(null);
            return listingData;
        }
        db.collection("listings").document(listingId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi khi lấy chi tiết tin đăng: ", error);
                        listingData.setValue(null);
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        Listing listing = snapshot.toObject(Listing.class);
                        if (listing != null) {
                            listing.setId(snapshot.getId());
                            listingData.setValue(listing);
                        }
                    } else {
                        listingData.setValue(null);
                    }
                });
        return listingData;
    }
    /**
     * Lấy danh sách các tin đăng dựa trên một danh sách các ID.
     * Cần thiết cho tính năng "Yêu thích".
     */
    public LiveData<List<Listing>> getListingsByIds(List<String> ids) {
        MutableLiveData<List<Listing>> listingsData = new MutableLiveData<>();
        if (ids == null || ids.isEmpty()) {
            listingsData.setValue(new ArrayList<>());
            return listingsData;
        }
        db.collection("listings").whereIn(FieldPath.documentId(), ids).get()
                .addOnSuccessListener(queryDocumentSnapshots -> listingsData.setValue(queryDocumentSnapshots.toObjects(Listing.class)))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy danh sách tin đăng bằng IDs: ", e);
                    listingsData.setValue(null);
                });
        return listingsData;
    }
    /**
     * Lấy các tin đăng đang hoạt động của một người dùng cụ thể, có giới hạn số lượng.
     */
    public LiveData<List<Listing>> getActiveListingsByUser(String userId, int limit) {
        MutableLiveData<List<Listing>> data = new MutableLiveData<>();
        db.collection("listings")
                .whereEqualTo("sellerId", userId)
                .whereEqualTo("status", "available")
                .orderBy("timePosted", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(snapshots -> data.setValue(snapshots.toObjects(Listing.class)))
                .addOnFailureListener(e -> data.setValue(Collections.emptyList()));
        return data;
    }

    /**
     * Lấy danh sách tin đăng của tôi (có phân trang).
     */
    public LiveData<PagedResult<Listing>> getMyListings(String userId, @Nullable DocumentSnapshot lastVisible) {
        MutableLiveData<PagedResult<Listing>> resultLiveData = new MutableLiveData<>();
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
                            listing.setId(doc.getId()); // Quan trọng: set ID cho listing
                            listings.add(listing);
                        }
                    }
                    resultLiveData.setValue(new PagedResult<>(listings, newLastVisible, null));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi tải tin đăng của tôi: ", e);
                    // Cung cấp một PagedResult rỗng với lỗi để UI có thể xử lý
                    resultLiveData.setValue(new PagedResult<>(Collections.emptyList(), null, e));
                });

        return resultLiveData;
    }
    /**
     * Xóa một tin đăng.
     */
    public LiveData<Boolean> deleteListing(String listingId) {
        MutableLiveData<Boolean> status = new MutableLiveData<>();
        db.collection("listings").document(listingId).delete()
                .addOnSuccessListener(aVoid -> status.setValue(true))
                .addOnFailureListener(e -> status.setValue(false));
        return status;
    }

    /**
     * Lấy danh sách tin đăng theo bộ lọc.
     */
    public LiveData<List<Listing>> getListingsByFilter(String filterType, @Nullable String categoryId) {
        MutableLiveData<List<Listing>> data = new MutableLiveData<>();
        Query query = db.collection("listings").whereEqualTo("status", "available");

        switch (filterType) {
            case "recommended":
                query = query.orderBy("views", Query.Direction.DESCENDING).limit(20);
                break;
            case "recent":
                query = query.orderBy("timePosted", Query.Direction.DESCENDING).limit(20);
                break;
            case "category":
                if (categoryId != null) {
                    query = query.whereEqualTo("categoryId", categoryId).orderBy("timePosted", Query.Direction.DESCENDING).limit(20);
                }
                break;
        }

        query.get().addOnSuccessListener(snapshots -> data.setValue(snapshots.toObjects(Listing.class))).addOnFailureListener(e -> data.setValue(Collections.emptyList()));

        return data;
    }

    /**
     * Cập nhật một tin đăng đã có trên Firestore.
     * @param listing Đối tượng Listing chứa ID và các trường đã được cập nhật.
     * @return LiveData<Boolean> báo hiệu thành công (true) hoặc thất bại (false).
     */
    public LiveData<Boolean> updateListing(Listing listing) {
        MutableLiveData<Boolean> status = new MutableLiveData<>();
        if (listing == null || listing.getId() == null || listing.getId().isEmpty()) {
            status.setValue(false);
            return status;
        }

        // Sử dụng set() với SetOptions.merge() để chỉ cập nhật các trường có trong
        // đối tượng `listing`, các trường khác trên Firestore sẽ được giữ nguyên.
        // Điều này an toàn hơn là dùng update() với một Map lớn.
        db.collection("listings").document(listing.getId())
                .set(listing, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Cập nhật tin đăng thành công: " + listing.getId());
                    status.setValue(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi cập nhật tin đăng: ", e);
                    status.setValue(false);
                });

        return status;
    }
}