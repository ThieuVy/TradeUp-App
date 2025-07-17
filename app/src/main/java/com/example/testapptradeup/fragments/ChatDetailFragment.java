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
            ChatDetailFragmentArgs args = ChatDetailFragmentArgs.fromBundle(getArguments());
            chatId = args.getChatId();
            otherUserName = args.getOtherUserName();
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
            // SỬA LỖI: Chỉ cần gọi một hàm để bắt đầu tải
            viewModel.loadChat(chatId);
            observeViewModel();
        } else {
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy cuộc trò chuyện.", Toast.LENGTH_SHORT).show();
            navController.popBackStack();
        }
    }

    private void initViews(View view) {
        recyclerMessages = view.findViewById(R.id.recycler_messages);
        etMessage = view.findViewById(R.id.edit_text_message);
        btnSend = view.findViewById(R.id.btn_send);
        toolbar = view.findViewById(R.id.toolbar_chat);
        btnMoreOptions = view.findViewById(R.id.btn_more_options_chat);
        if (otherUserName != null) {
            toolbar.setTitle(otherUserName);
        }
    }

    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());
        btnSend.setOnClickListener(v -> sendMessage());
        btnMoreOptions.setOnClickListener(v -> showChatOptionsDialog());
    }

    private void setupRecyclerView() {
        adapter = new ChatMessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        recyclerMessages.setLayoutManager(layoutManager);
        recyclerMessages.setAdapter(adapter);
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
        if (getContext() == null) return;

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
        // Lắng nghe dữ liệu cuộc trò chuyện để lấy ID người dùng còn lại
        viewModel.getChatData().observe(getViewLifecycleOwner(), conversation -> {
            // ĐOẠN CODE NÀY BÂY GIỜ SẼ HỢP LỆ
            if (conversation != null && conversation.getMembers() != null) {
                String currentUid = FirebaseAuth.getInstance().getUid();
                if (currentUid != null) {
                    conversation.getMembers().stream()
                            .filter(memberId -> !memberId.equals(currentUid))
                            .findFirst()
                            .ifPresent(id -> this.otherUserId = id);
                }
            }
        });

        // Lắng nghe danh sách tin nhắn để cập nhật RecyclerView
        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            adapter.submitList(messages);
            if (messages != null && !messages.isEmpty()) {
                recyclerMessages.smoothScrollToPosition(messages.size() - 1);
            }
        });
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        if (!messageText.isEmpty()) {
            viewModel.sendMessage(chatId, messageText);
            etMessage.setText("");
        }
    }

    private void sendConversationReport(String reason) {
        String reporterId = FirebaseAuth.getInstance().getUid();
        if (reporterId == null) {
            Toast.makeText(getContext(), "Bạn cần đăng nhập để báo cáo.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (otherUserId == null) {
            Toast.makeText(getContext(), "Lỗi: Không xác định được người dùng cần báo cáo.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> report = new HashMap<>();
        report.put("reporterId", reporterId);
        report.put("reportedUserId", otherUserId); // Báo cáo người dùng đối thoại
        report.put("chatId", chatId); // Thêm ID cuộc trò chuyện để dễ truy vết
        report.put("reason", reason);
        report.put("timestamp", FieldValue.serverTimestamp());
        report.put("type", "conversation"); // Phân loại báo cáo
        report.put("status", "pending");

        FirebaseFirestore.getInstance().collection("reports").add(report)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(getContext(), "Cảm ơn bạn đã gửi báo cáo.", Toast.LENGTH_LONG).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Gửi báo cáo thất bại. Vui lòng thử lại.", Toast.LENGTH_SHORT).show()
                );
    }
}