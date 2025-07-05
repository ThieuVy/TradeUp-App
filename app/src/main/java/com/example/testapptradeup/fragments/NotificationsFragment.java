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
import androidx.cardview.widget.CardView;
// Thêm import ContextCompat
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.NotificationAdapter;
import com.example.testapptradeup.models.NotificationItem; // Đảm bảo import NotificationItem
import com.example.testapptradeup.viewmodels.NotificationsViewModel;
import com.google.android.material.tabs.TabLayout;

import java.util.Objects;

public class NotificationsFragment extends Fragment implements NotificationAdapter.OnNotificationClickListener {

    private NotificationsViewModel notificationsViewModel;
    private RecyclerView recyclerNotifications;
    private LinearLayout layoutEmptyState;
    private ProgressBar progressLoading;
    private NotificationAdapter notificationAdapter;

    private TabLayout tabLayoutNotifications;
    private ImageView menuMore;
    private CardView popupMenuCard;
    private TextView menuMarkAllRead, menuClearAll, menuSettings;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        notificationsViewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);

        View root = inflater.inflate(R.layout.fragment_notifications, container, false);

        initViews(root);
        setupRecyclerView();
        setupTabLayout();
        setupClickListeners();
        observeViewModel();

        // Load initial notifications (e.g., "Tất cả")
        notificationsViewModel.loadNotifications("Tất cả");

        return root;
    }

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

    private void setupRecyclerView() {
        notificationAdapter = new NotificationAdapter();
        notificationAdapter.setOnNotificationClickListener(this);

        recyclerNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerNotifications.setAdapter(notificationAdapter);
    }

    private void setupTabLayout() {
        // Xóa các tab cũ nếu có để tránh lặp lại
        tabLayoutNotifications.removeAllTabs();

        // Thêm các tab bằng code
        tabLayoutNotifications.addTab(tabLayoutNotifications.newTab().setText("Tất cả"));
        tabLayoutNotifications.addTab(tabLayoutNotifications.newTab().setText("Tin nhắn"));
        tabLayoutNotifications.addTab(tabLayoutNotifications.newTab().setText("Ưu đãi"));
        tabLayoutNotifications.addTab(tabLayoutNotifications.newTab().setText("Tin đăng")); // Sửa "Danh sách" thành "Tin đăng" cho rõ nghĩa
        tabLayoutNotifications.addTab(tabLayoutNotifications.newTab().setText("Khuyến mãi"));

        tabLayoutNotifications.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String categoryFilter = Objects.requireNonNull(tab.getText()).toString();
                // ViewModel sẽ chịu trách nhiệm lọc và cập nhật LiveData
                // Ánh xạ tên hiển thị sang loại category trong model
                String internalCategory;
                switch(categoryFilter) {
                    case "Tin nhắn": internalCategory = "MESSAGE"; break;
                    case "Ưu đãi":
                    case "Khuyến mãi":
                        internalCategory = "PROMOTION"; break;
                    case "Tin đăng": internalCategory = "LISTING"; break;
                    default: internalCategory = "Tất cả"; break;
                }
                notificationsViewModel.loadNotifications(internalCategory);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Do nothing
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                onTabSelected(tab);
            }
        });
    }

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
            // TODO: Navigate to notification settings screen
        });
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


    private void observeViewModel() {
        notificationsViewModel.getNotifications().observe(getViewLifecycleOwner(), notifications -> {
            if (notifications != null && !notifications.isEmpty()) {
                notificationAdapter.setNotifications(notifications);
                showNotificationsList();
            } else {
                showEmptyState();
            }
        });

        notificationsViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (isLoading) {
                recyclerNotifications.setVisibility(View.GONE);
                layoutEmptyState.setVisibility(View.GONE);
            }
        });

        notificationsViewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                showError(message);
                notificationsViewModel.clearErrorMessage();
            }
        });

        notificationsViewModel.getUnreadCount().observe(getViewLifecycleOwner(), count -> {
            Log.d(TAG, "Unread notifications: " + count);
            // Bạn có thể cập nhật một badge ở đây nếu MainActivity hoặc parent của bạn có một cái
        });
    }

    private void showNotificationsList() {
        recyclerNotifications.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        recyclerNotifications.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
    }

    @Override
    public void onNotificationClick(NotificationItem notification, int position) {
        if (!notification.isRead()) {
            notificationsViewModel.markAsRead(notification.getId());
        }
        handleNotificationAction(notification);
    }

    private void handleNotificationAction(NotificationItem notification) {
        if (notification.getType() != null) { // SỬA: Kiểm tra null cho Enum
            switch (notification.getType()) { // SỬA: Dùng NotificationType enum
                case TRADE:
                    showToast("Điều hướng đến chi tiết giao dịch: " + notification.getActionUrl());
                    break;
                case PRICE_ALERT:
                    showToast("Điều hướng đến biểu đồ giá: " + notification.getActionUrl());
                    break;
                case NEWS:
                    showToast("Mở bài viết tin tức: " + notification.getActionUrl());
                    break;
                case SYSTEM:
                    showToast("Hiển thị thông báo hệ thống: " + notification.getContent());
                    break;
                case PROMOTION:
                    showToast("Mở chi tiết ưu đãi: " + notification.getActionUrl());
                    break;
                case LISTING:
                    showToast("Mở chi tiết danh sách: " + notification.getActionUrl());
                    break;
                case MESSAGE:
                    showToast("Mở cuộc trò chuyện: " + notification.getActionUrl());
                    break;
                case OTHER:
                default:
                    showToast("Không thể xử lý loại thông báo này.");
                    break;
            }
        } else {
            showToast("Loại thông báo không xác định.");
        }
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
    }
}
