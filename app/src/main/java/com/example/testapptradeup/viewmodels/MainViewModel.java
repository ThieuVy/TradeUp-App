package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.testapptradeup.models.Listing; // <<< THÊM IMPORT NÀY
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.utils.Event;

/**
 * ViewModel được chia sẻ ở cấp Activity để giao tiếp giữa các Fragment.
 */
public class MainViewModel extends ViewModel {

    // --- Logic cho sự kiện cập nhật/sửa tin đăng (đã có) ---
    private final MutableLiveData<Event<Boolean>> _listingUpdatedEvent = new MutableLiveData<>();
    private final MutableLiveData<User> _currentUser = new MutableLiveData<>();

    public LiveData<Event<Boolean>> getListingUpdatedEvent() {
        return _listingUpdatedEvent;
    }

    public void postListingUpdateEvent() {
        _listingUpdatedEvent.setValue(new Event<>(true));
    }

    public LiveData<User> getCurrentUser() {
        return _currentUser;
    }

    public void setCurrentUser(User user) {
        _currentUser.setValue(user);
    }


    // --- THÊM LOGIC XỬ LÝ SỰ KIỆN ĐĂNG TIN MỚI VÀO ĐÂY ---

    // LiveData private, có thể thay đổi, để chứa tin đăng mới.
    private final MutableLiveData<Listing> _newListingPosted = new MutableLiveData<>();

    /**
     * Cung cấp LiveData công khai, chỉ đọc để các Fragment có thể observe.
     * Fragment nào lắng nghe LiveData này sẽ nhận được đối tượng Listing mới.
     */
    public LiveData<Listing> getNewListingPosted() {
        return _newListingPosted;
    }

    /**
     * Được gọi bởi PostFragment sau khi đăng tin thành công để gửi sự kiện.
     * Đây chính là phương thức đang bị thiếu.
     */
    public void onNewListingPosted(Listing newListing) {
        _newListingPosted.setValue(newListing);
    }

    /**
     * Được gọi bởi Fragment nhận sự kiện (ví dụ: HomeFragment) sau khi đã xử lý xong,
     * để tránh sự kiện được xử lý lại (ví dụ: khi xoay màn hình).
     */
    public void onNewListingEventHandled() {
        _newListingPosted.setValue(null);
    }
}