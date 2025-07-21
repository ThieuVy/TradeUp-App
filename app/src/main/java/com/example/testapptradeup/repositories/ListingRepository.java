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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ListingRepository {
    private static final String TAG = "ListingRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    public static final int PAGE_SIZE = 15;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() { return _errorMessage; }

    private final CollectionReference listingsCollection = db.collection("listings");
    private final CollectionReference usersCollection = db.collection("users");

    private final MutableLiveData<List<Listing>> featuredListingsData = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<Listing>> recommendedListingsData = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<Listing>> recentListingsData = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading() { return _isLoading; }

    public ListingRepository() {

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

    private void mapIdsToList(List<Listing> listings, List<QueryDocumentSnapshot> documents) {
        if (listings == null || documents == null || listings.size() != documents.size()) return;
        for (int i = 0; i < documents.size(); i++) {
            listings.get(i).setId(documents.get(i).getId());
        }
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
            query = query.whereEqualTo("category", params.getCategory());
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
            // Firestore yêu cầu trường sắp xếp phải là trường đầu tiên trong các điều kiện bất đẳng thức (nếu có).
            // Ở đây, chúng ta đang lọc theo khoảng giá (`>=` và `<=`) trên trường "price".
            // Do đó, nếu sắp xếp theo trường khác "price", truy vấn sẽ thất bại.
            if (params.hasPriceFilter() && !params.getSortBy().equals("price")) {
                Log.w(TAG, "Không thể sắp xếp theo trường khác khi đang lọc theo khoảng giá. Bỏ qua sắp xếp.");
            } else {
                Query.Direction direction = params.isSortAscending() ? Query.Direction.ASCENDING : Query.Direction.DESCENDING;
                query = query.orderBy(params.getSortBy(), direction);
            }
        } else if (params.getQuery() == null || params.getQuery().isEmpty()) {
            // Nếu không có từ khóa tìm kiếm và không có tùy chọn sắp xếp, mặc định sắp xếp theo mới nhất
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
                    // Gọi constructor 3 tham số cho trường hợp thành công
                    resultLiveData.setValue(new PagedResult<>(listings, newLastVisible, null));
                })
                .addOnFailureListener(e -> {
                    Log.e("ListingRepository", "Lỗi truy vấn Firestore: " + e.getMessage(), e);
                    // Gọi constructor 3 tham số cho trường hợp thất bại
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

            // Sắp xếp kết quả theo khoảng cách tăng dần
            matchingDocs.sort(Comparator.comparingDouble(l -> GeoFireUtils.getDistanceBetween(new GeoLocation(l.getLatitude(), l.getLongitude()), center)));
            resultLiveData.setValue(new PagedResult<>(matchingDocs, null, null));

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Lỗi Geo-query", e);
            // <<< SỬA LỖI Ở ĐÂY >>>
            resultLiveData.setValue(new PagedResult<>(null, null, e));
        });
    }

    // File: repositories/ListingRepository.java

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
        MutableLiveData<List<Listing>> data = new MutableLiveData<>();
        db.collection("listings")
                .whereEqualTo("status", "available")
                .orderBy("timePosted", Query.Direction.DESCENDING)
                .limit(20)
                .addSnapshotListener((snapshots, error) -> {
                    if (handleError(error, "Lỗi tải tin đăng gần đây")) return;
                    if (snapshots != null) {
                        List<Listing> listings = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Listing listing = doc.toObject(Listing.class);
                            listing.setId(doc.getId()); // Gán ID trực tiếp
                            listings.add(listing);
                        }
                        data.setValue(listings);
                    }
                });
        return data;
    }

    public LiveData<List<Listing>> getFeaturedListings() {
        MutableLiveData<List<Listing>> data = new MutableLiveData<>();
        db.collection("listings")
                .whereEqualTo("featured", true)
                .whereEqualTo("status", "available")
                .orderBy("timePosted", Query.Direction.DESCENDING)
                .limit(5)
                .addSnapshotListener((snapshots, error) -> {
                    if (handleError(error, "Lỗi tải mục Nổi bật")) return;
                    if (snapshots != null) {
                        List<Listing> listings = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Listing listing = doc.toObject(Listing.class);
                            listing.setId(doc.getId()); // Gán ID trực tiếp
                            listings.add(listing);
                        }
                        data.setValue(listings);
                    }
                });
        return data;
    }

    public LiveData<List<Listing>> getRecommendedListings(int limit) {
        MutableLiveData<List<Listing>> data = new MutableLiveData<>();
        db.collection("listings")
                .whereEqualTo("status", "available")
                .orderBy("views", Query.Direction.DESCENDING)
                .limit(limit)
                .addSnapshotListener((snapshots, error) -> {
                    if (handleError(error, "Lỗi tải mục Đề xuất")) return;
                    if (snapshots != null) {
                        List<Listing> listings = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Listing listing = doc.toObject(Listing.class);
                            listing.setId(doc.getId()); // Gán ID trực tiếp
                            listings.add(listing);
                        }
                        data.setValue(listings);
                    }
                });
        return data;
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
                    if (handleError(error, "Lỗi khi lấy chi tiết tin đăng")) {
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
        db.collection("listings").whereIn(FieldPath.documentId(), ids)
                .addSnapshotListener((snapshots, error) -> {
                    if (handleError(error, "Lỗi khi lấy danh sách yêu thích")) {
                        listingsData.postValue(new ArrayList<>());
                        return;
                    }
                    if (snapshots != null) {
                        List<Listing> listings = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Listing listing = doc.toObject(Listing.class);
                            listing.setId(doc.getId()); // Gán ID trực tiếp
                            listings.add(listing);
                        }
                        listingsData.postValue(listings);
                    }
                });
        return listingsData;
    }

    /**
     * Lấy các tin đăng đang hoạt động của một người dùng cụ thể, có giới hạn số lượng.
     */
    public LiveData<List<Listing>> getActiveListingsByUser(String userId, int limit) {
        MutableLiveData<List<Listing>> data = new MutableLiveData<>();
        if (userId == null || userId.isEmpty()) {
            data.setValue(Collections.emptyList());
            return data;
        }

        Query query = db.collection("listings")
                .whereEqualTo("sellerId", userId)
                .whereEqualTo("status", "available")
                .orderBy("timePosted", Query.Direction.DESCENDING);

        // Chỉ áp dụng giới hạn nếu limit > 0
        if (limit > 0) {
            query = query.limit(limit);
        }

        query.get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots != null) {
                        data.setValue(snapshots.toObjects(Listing.class));
                    } else {
                        data.setValue(Collections.emptyList());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy tin đăng của người dùng: " + userId, e);
                    data.setValue(Collections.emptyList());
                });

        return data;
    }

    /**
     * Lấy danh sách tin đăng của tôi (có phân trang và bộ lọc trạng thái).
     * @param userId ID của người dùng.
     * @param status Trạng thái cần lọc ("available", "sold", "paused"). Nếu là "all" hoặc null, không lọc theo trạng thái.
     * @param lastVisible Document cuối cùng của trang trước để phân trang.
     */
    public LiveData<PagedResult<Listing>> getMyListings(String userId, @Nullable String status, @Nullable DocumentSnapshot lastVisible) {
        MutableLiveData<PagedResult<Listing>> resultLiveData = new MutableLiveData<>();
        Query query = db.collection("listings").whereEqualTo("sellerId", userId);

        if (status != null && !status.equals("all") && !status.isEmpty()) {
            query = query.whereEqualTo("status", status);
        }

        query = query.orderBy("timePosted", Query.Direction.DESCENDING);

        if (lastVisible != null) {
            query = query.startAfter(lastVisible);
        }

        query.limit(PAGE_SIZE).addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Log.e(TAG, "Lỗi khi tải tin đăng của tôi: ", error);
                resultLiveData.postValue(new PagedResult<>(null, null, error));
                return;
            }

            if (snapshots != null) {
                DocumentSnapshot newLastVisible = snapshots.isEmpty() ? null : snapshots.getDocuments().get(snapshots.size() - 1);
                List<Listing> listings = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    Listing listing = doc.toObject(Listing.class);
                    listing.setId(doc.getId()); // Gán ID trực tiếp
                    listings.add(listing);
                }
                resultLiveData.postValue(new PagedResult<>(listings, newLastVisible, null));
            }
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
    public LiveData<List<Listing>> getListingsByFilter(String filterType, @Nullable String id) {
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
                if (id != null && !id.isEmpty()) {
                    Log.d(TAG, "Đang lọc sản phẩm với categoryId chuẩn: " + id);
                    query = query.whereEqualTo("category", id)
                            .orderBy("timePosted", Query.Direction.DESCENDING)
                            .limit(20);
                } else {
                    Log.w(TAG, "Lọc theo danh mục nhưng ID không hợp lệ.");
                    data.setValue(Collections.emptyList());
                    return data;
                }
                break;
            case "user":
                // Sử dụng tham số 'id' đã được đổi tên và truyền vào
                if (id != null && !id.isEmpty()) {
                    query = query.whereEqualTo("sellerId", id)
                            .orderBy("timePosted", Query.Direction.DESCENDING);
                } else {
                    data.setValue(Collections.emptyList());
                    return data;
                }
                break;
            default:
                data.setValue(Collections.emptyList());
                return data;
        }

        query.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Log.e(TAG, "Lỗi khi lọc tin đăng: ", error);
                data.postValue(Collections.emptyList());
                return;
            }
            if (snapshots != null) {
                Log.d(TAG, "Query thành công, tìm thấy " + snapshots.size() + " sản phẩm.");
                List<Listing> listings = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    Listing listing = doc.toObject(Listing.class);
                    listing.setId(doc.getId());
                    listings.add(listing);
                }
                data.postValue(listings);
            } else {
                data.postValue(Collections.emptyList());
            }
        });

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
        db.collection("listings").document(listing.getId())
                .set(listing, SetOptions.merge())
                .addOnSuccessListener(aVoid -> status.setValue(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi cập nhật tin đăng: ", e);
                    status.setValue(false);
                });
        return status;
    }

    private boolean handleError(@Nullable FirebaseFirestoreException error, String message) {
        if (error != null) {
            Log.e(TAG, message, error);
            _errorMessage.postValue(message);
            return true;
        }
        return false;
    }
}