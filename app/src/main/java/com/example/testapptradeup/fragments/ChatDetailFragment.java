package com.example.testapptradeup.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.ChatMessageAdapter;
import com.example.testapptradeup.viewmodels.ChatDetailViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ChatDetailFragment extends Fragment {

    private ChatDetailViewModel viewModel;
    private NavController navController;
    private String chatId;
    private String otherUserName;

    // UI Components
    private RecyclerView recyclerMessages;
    private ChatMessageAdapter adapter;
    private EditText etMessage;
    private ImageButton btnSend;
    private MaterialToolbar toolbar;
    private String otherUserId;
    private ImageView btnMoreOptions;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ChatDetailViewModel.class);

        if (getArguments() != null) {
            chatId = ChatDetailFragmentArgs.fromBundle(getArguments()).getChatId();
            otherUserName = ChatDetailFragmentArgs.fromBundle(getArguments()).getOtherUserName();
            // Lấy otherUserId từ conversation trong ViewModel hoặc từ argument nếu có
            // Tạm thời sẽ để logic lấy ID này trong onViewCreated
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        initViews(view);
        setupRecyclerView();
        setupListeners();

        if (chatId != null) {
            viewModel.loadMessages(chatId); // Báo ViewModel tải tin nhắn cho cuộc trò chuyện này
            observeViewModel();
        } else {
            // Xử lý trường hợp không có chatId -> quay lại
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy cuộc trò chuyện.", Toast.LENGTH_SHORT).show();
            navController.popBackStack();
        }
    }

    private void initViews(View view) {
        recyclerMessages = view.findViewById(R.id.recycler_messages);
        etMessage = view.findViewById(R.id.edit_text_message);
        btnSend = view.findViewById(R.id.btn_send);
        toolbar = view.findViewById(R.id.toolbar_chat);
        // === THÊM MỚI: Ánh xạ nút more options ===
        btnMoreOptions = view.findViewById(R.id.btn_more_options_chat);

        if (otherUserName != null) {
            toolbar.setTitle(otherUserName);
        }
    }

    private void setupRecyclerView() {
        adapter = new ChatMessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true); // Quan trọng: Giúp RecyclerView bắt đầu từ dưới cùng
        recyclerMessages.setLayoutManager(layoutManager);
        recyclerMessages.setAdapter(adapter);
    }

    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());
        btnSend.setOnClickListener(v -> sendMessage());

        // === THÊM MỚI: Bắt sự kiện cho nút more options ===
        btnMoreOptions.setOnClickListener(v -> showChatOptionsDialog());
    }

    private void showChatOptionsDialog() {
        if (getContext() == null) return;

        final CharSequence[] options = {"Báo cáo cuộc trò chuyện", "Chặn người dùng", "Hủy"};

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Tùy chọn");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Báo cáo cuộc trò chuyện")) {
                showReportReasonDialog();
            } else if (options[item].equals("Chặn người dùng")) {
                // TODO: Triển khai logic chặn người dùng
                Toast.makeText(getContext(), "Chức năng chặn đang được phát triển.", Toast.LENGTH_SHORT).show();
            } else if (options[item].equals("Hủy")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void showReportReasonDialog() {
        if (getContext() == null || chatId == null) return;

        final String[] reportReasons = {"Nội dung không phù hợp", "Spam hoặc Lừa đảo", "Quấy rối", "Lý do khác"};

        new AlertDialog.Builder(getContext())
                .setTitle("Báo cáo cuộc trò chuyện")
                .setItems(reportReasons, (dialog, which) -> {
                    String reason = reportReasons[which];
                    sendConversationReport(reason);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void observeViewModel() {
        // Lắng nghe danh sách tin nhắn từ ViewModel
        viewModel.messages.observe(getViewLifecycleOwner(), messages -> {
            adapter.submitList(messages);
            // Tự động cuộn xuống tin nhắn mới nhất
            if (messages != null && !messages.isEmpty()) {
                recyclerMessages.smoothScrollToPosition(messages.size() - 1);
            }
        });
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        if (!messageText.isEmpty()) {
            viewModel.sendMessage(chatId, messageText);
            etMessage.setText(""); // Xóa nội dung trong ô nhập sau khi gửi
        }
    }

    private void sendConversationReport(String reason) {
        String reporterId = FirebaseAuth.getInstance().getUid();
        if (reporterId == null) {
            Toast.makeText(getContext(), "Bạn cần đăng nhập để thực hiện hành động này.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> report = new HashMap<>();
        report.put("reporterId", reporterId);
        report.put("chatId", chatId); // Thêm ID của cuộc trò chuyện để dễ dàng truy vết
        report.put("reason", reason);
        report.put("timestamp", FieldValue.serverTimestamp());
        report.put("type", "conversation"); // Phân loại báo cáo
        report.put("status", "pending"); // Trạng thái ban đầu của báo cáo

        FirebaseFirestore.getInstance().collection("reports").add(report)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(getContext(), "Cảm ơn bạn đã gửi báo cáo.", Toast.LENGTH_LONG).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Gửi báo cáo thất bại. Vui lòng thử lại.", Toast.LENGTH_SHORT).show()
                );
    }
}