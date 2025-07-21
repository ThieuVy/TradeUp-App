package com.example.testapptradeup.fragments;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.NotificationAdapter;
import com.example.testapptradeup.models.Notification; // Sửa: Dùng model Notification đã thống nhất
import com.example.testapptradeup.viewmodels.NotificationsViewModel;
import com.google.android.material.tabs.TabLayout;

import java.util.Objects;

/**
 * Fragment này hiển thị danh sách thông báo và xử lý các tương tác của người dùng.
 * Nó triển khai OnNotificationClickListener để nhận sự kiện click từ adapter.
 */
public class NotificationsFragment extends Fragment implements NotificationAdapter.OnNotificationClickListener {

    private NotificationsViewModel notificationsViewModel;
    private RecyclerView recyclerNotifications;
    private LinearLayout layoutEmptyState;
    private ProgressBar progressLoading;
    private NotificationAdapter notificationAdapter;
    private NavController navController;

    // Các thành phần của Popup Menu
    private TabLayout tabLayoutNotifications;
    private ImageView menuMore;
    private CardView popupMenuCard;
    private TextView menuMarkAllRead, menuClearAll, menuSettings;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Chỉ khởi tạo ViewModel ở đây
        notificationsViewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo NavController
        navController = Navigation.findNavController(view);

        // Gọi các hàm thiết lập
        initViews(view);
        setupRecyclerView();
        setupTabLayout();
        setupClickListeners();
        observeViewModel();
    }

    /**
     * Ánh xạ tất cả các biến UI với các View tương ứng trong layout XML.
     */
    private void initViews(View root) {
        recyclerNotifications = root.findViewById(R.id.recycler_notifications);
        layoutEmptyState = root.findViewById(R.id.layout_empty_state);
        progressLoading = root.findViewById(R.id.progress_loading);
        tabLayoutNotifications = root.findViewById(R.id.tab_layout_notifications);
        menuMore = root.findViewById(R.id.menu_more);
        popupMenuCard = root.findViewById(R.id.popup_menu_card);
        menuMarkAllRead = root.findViewById(R.id.menu_mark_all_read);
        menuClearAll = root.findViewById(R.id.menu_clear_all);
        menuSettings = root.findViewById(R.id.menu_settings);
    }

    /**
     * Thiết lập RecyclerView và Adapter.
     * Fragment này (`this`) được truyền vào làm listener cho các sự kiện click.
     */
    private void setupRecyclerView() {
        notificationAdapter = new NotificationAdapter(this);
        recyclerNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerNotifications.setAdapter(notificationAdapter);
    }

    /**
     * Thiết lập các tab lọc và listener cho chúng.
     */
    private void setupTabLayout() {
        tabLayoutNotifications.removeAllTabs();
        tabLayoutNotifications.addTab(tabLayoutNotifications.newTab().setText("Tất cả"));
        tabLayoutNotifications.addTab(tabLayoutNotifications.newTab().setText("Tin nhắn"));
        tabLayoutNotifications.addTab(tabLayoutNotifications.newTab().setText("Ưu đãi"));
        tabLayoutNotifications.addTab(tabLayoutNotifications.newTab().setText("Tin đăng"));
        tabLayoutNotifications.addTab(tabLayoutNotifications.newTab().setText("Khuyến mãi"));

        tabLayoutNotifications.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String categoryFilter = Objects.requireNonNull(tab.getText()).toString();
                String internalCategory;
                switch(categoryFilter) {
                    case "Tin nhắn": internalCategory = "MESSAGE"; break;
                    case "Ưu đãi": internalCategory = "OFFER"; break; // Đổi thành OFFER cho đúng với Enum
                    case "Khuyến mãi": internalCategory = "PROMOTION"; break;
                    case "Tin đăng": internalCategory = "LISTING"; break;
                    default: internalCategory = "Tất cả"; break;
                }
                notificationsViewModel.loadNotifications(internalCategory);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) { onTabSelected(tab); }
        });
    }

    /**
     * Thiết lập các sự kiện click cho menu "..."
     */
    private void setupClickListeners() {
        menuMore.setOnClickListener(v -> togglePopupMenu());

        menuMarkAllRead.setOnClickListener(v -> {
            notificationsViewModel.markAllAsRead();
            showToast("Đã đánh dấu tất cả là đã đọc");
            popupMenuCard.setVisibility(View.GONE);
        });

        menuClearAll.setOnClickListener(v -> {
            showClearAllConfirmationDialog();
            popupMenuCard.setVisibility(View.GONE);
        });

        menuSettings.setOnClickListener(v -> {
            showToast("Mở cài đặt thông báo");
            popupMenuCard.setVisibility(View.GONE);
            // TODO: Điều hướng đến màn hình cài đặt
        });
    }

    /**
     * Lắng nghe các thay đổi từ ViewModel để cập nhật giao diện.
     */
    private void observeViewModel() {
        notificationsViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (isLoading) {
                recyclerNotifications.setVisibility(View.GONE);
                layoutEmptyState.setVisibility(View.GONE);
            }
        });

        notificationsViewModel.getNotifications().observe(getViewLifecycleOwner(), notifications -> {
            notificationAdapter.submitList(notifications);
            if (notifications == null || notifications.isEmpty()) {
                // Chỉ hiện empty state khi đã load xong
                if (notificationsViewModel.getIsLoading().getValue() != null && !notificationsViewModel.getIsLoading().getValue()) {
                    showEmptyState();
                }
            } else {
                showNotificationsList();
            }
        });

        notificationsViewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                showError(message);
                notificationsViewModel.clearErrorMessage();
            }
        });

        notificationsViewModel.getUnreadCount().observe(getViewLifecycleOwner(), count -> {
            Log.d(TAG, "Số thông báo chưa đọc: " + count);
            // Cập nhật badge tại đây nếu có
        });
    }

    /**
     * Đây là phương thức quan trọng nhất, được gọi từ Adapter khi người dùng nhấn vào một thông báo.
     * @param notification Đối tượng thông báo đã được click.
     */
    @Override
    public void onNotificationClick(Notification notification) {
        Log.d(TAG, "Clicked notification: " + notification.getTitle() + ", Type: " + notification.getType());

        // Bước 1: Luôn đánh dấu là đã đọc (nếu chưa đọc)
        if (!notification.isRead()) {
            notificationsViewModel.markAsRead(notification.getId());
        }

        // Bước 2: Thực hiện hành động điều hướng
        handleNotificationAction(notification);
    }

    /**
     * Hàm này chứa logic chính để quyết định sẽ điều hướng đi đâu
     * dựa vào loại thông báo.
     */
    private void handleNotificationAction(Notification notification) {
        if (notification.getType() == null || notification.getRelatedId() == null || notification.getRelatedId().isEmpty()) {
            Log.w(TAG, "Không thể điều hướng: Loại thông báo hoặc ID liên quan là null/rỗng.");
            showToast("Không có hành động cho thông báo này.");
            return;
        }

        try {
            switch (notification.getType()) {
                // Xóa "case PROMOTION:" khỏi nhóm này
                case LISTING:
                case OFFER:
                    NotificationsFragmentDirections.ActionNotificationsFragmentToProductDetailFragment detailAction =
                            NotificationsFragmentDirections.actionNotificationsFragmentToProductDetailFragment(notification.getRelatedId());
                    navController.navigate(detailAction);
                    break;

                case MESSAGE:
                    // Cần lấy tên của người gửi từ notification.title hoặc một trường dữ liệu khác
                    String otherUserName = notification.getTitle().replace("Tin nhắn mới từ ", "");

                    NotificationsFragmentDirections.ActionNotificationsFragmentToChatDetailFragment chatAction =
                            NotificationsFragmentDirections.actionNotificationsFragmentToChatDetailFragment(notification.getRelatedId(), otherUserName);
                    navController.navigate(chatAction);
                    break;

                // Logic cho PROMOTION và SYSTEM bây giờ đã đúng
                case PROMOTION:
                case SYSTEM:
                default:
                    // Đối với các loại khác, hiển thị dialog là hợp lý
                    new AlertDialog.Builder(requireContext())
                            .setTitle(notification.getTitle())
                            .setMessage(notification.getContent())
                            .setPositiveButton("Đã hiểu", null)
                            .show();
                    break;
            }
        } catch (Exception e) {
            // Bắt các lỗi có thể xảy ra khi điều hướng (ví dụ: action không tồn tại)
            Log.e(TAG, "Lỗi điều hướng: " + e.getMessage());
            showError("Đã xảy ra lỗi khi mở thông báo.");
        }
    }

    // --- CÁC HÀM HELPER ĐƯỢC VIẾT ĐẦY ĐỦ ---

    private void showNotificationsList() {
        recyclerNotifications.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        recyclerNotifications.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
    }

    private void togglePopupMenu() {
        if (popupMenuCard.getVisibility() == View.VISIBLE) {
            popupMenuCard.setVisibility(View.GONE);
        } else {
            popupMenuCard.setVisibility(View.VISIBLE);
        }
    }

    private void showClearAllConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa tất cả thông báo không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    notificationsViewModel.clearAllNotifications();
                    showToast("Đã xóa tất cả thông báo");
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Dọn dẹp các tham chiếu đến View để tránh rò rỉ bộ nhớ
        recyclerNotifications = null;
        layoutEmptyState = null;
        progressLoading = null;
        notificationAdapter = null;
        tabLayoutNotifications = null;
        menuMore = null;
        popupMenuCard = null;
        menuMarkAllRead = null;
        menuClearAll = null;
        menuSettings = null;
        navController = null;
    }
}