package com.example.testapptradeup.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
import com.vanniktech.emoji.EmojiEditText;
import com.vanniktech.emoji.EmojiPopup;

import java.util.HashMap;
import java.util.Map;

public class ChatDetailFragment extends Fragment {

    private ChatDetailViewModel viewModel;
    private NavController navController;
    private String chatId;
    private String otherUserName;
    private String otherUserId;

    // UI Components
    private RecyclerView recyclerMessages;
    private ChatMessageAdapter adapter;
    private MaterialToolbar toolbar;
    private ImageView btnMoreOptions;
    private EmojiEditText etMessage;
    private ImageButton btnSend, btnAttachImage, btnEmoji;
    private EmojiPopup emojiPopup;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ChatDetailViewModel.class);

        if (getArguments() != null) {
            ChatDetailFragmentArgs args = ChatDetailFragmentArgs.fromBundle(getArguments());
            chatId = args.getChatId();
            otherUserName = args.getOtherUserName();
        }

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            viewModel.sendImageMessage(chatId, selectedImageUri);
                        }
                    }
                }
        );
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
        setupEmojiPopup(view);
        setupListeners();

        if (chatId != null) {
            viewModel.loadChat(chatId);
            observeViewModel();
        } else {
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy cuộc trò chuyện.", Toast.LENGTH_SHORT).show();
            navController.popBackStack();
        }
    }

    private void initViews(View view) {
        recyclerMessages = view.findViewById(R.id.recycler_messages);
        toolbar = view.findViewById(R.id.toolbar_chat);
        btnMoreOptions = view.findViewById(R.id.btn_more_options_chat);
        etMessage = view.findViewById(R.id.edit_text_message);
        btnSend = view.findViewById(R.id.btn_send);
        btnAttachImage = view.findViewById(R.id.btn_attach_image);
        btnEmoji = view.findViewById(R.id.btn_emoji);

        if (otherUserName != null) {
            toolbar.setTitle(otherUserName);
        }
    }

    private void setupEmojiPopup(View rootView) {
        emojiPopup = new EmojiPopup(rootView, etMessage);
    }

    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());
        btnSend.setOnClickListener(v -> sendMessage());
        btnMoreOptions.setOnClickListener(v -> showChatOptionsDialog());
        btnAttachImage.setOnClickListener(v -> openImagePicker());
        btnEmoji.setOnClickListener(v -> emojiPopup.toggle());
    }

    private void setupRecyclerView() {
        adapter = new ChatMessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        recyclerMessages.setLayoutManager(layoutManager);
        recyclerMessages.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getChatData().observe(getViewLifecycleOwner(), conversation -> {
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

        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            adapter.submitList(messages);
            if (messages != null && !messages.isEmpty()) {
                recyclerMessages.smoothScrollToPosition(messages.size() - 1);
            }
        });

        viewModel.getIsSendingImage().observe(getViewLifecycleOwner(), isSending -> {
            btnSend.setEnabled(!isSending);
            btnAttachImage.setEnabled(!isSending);
            btnEmoji.setEnabled(!isSending);
        });

        viewModel.getOtherUserStatus(otherUserId).observe(getViewLifecycleOwner(), status -> {
            if (status != null && status.isOnline()) { // Sửa: gọi isOnline()
                toolbar.setSubtitle("Đang hoạt động");
                toolbar.setSubtitleTextColor(ContextCompat.getColor(requireContext(), R.color.success));
            } else {
                // SỬA LỖI Ở ĐÂY: Gọi phương thức helper mới
                long lastSeen = (status != null) ? status.getLastSeenLong() : 0;

                if (lastSeen > 0) {
                    String timeAgo = DateUtils.getRelativeTimeSpanString(lastSeen, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString();
                    toolbar.setSubtitle("Hoạt động " + timeAgo);
                } else {
                    toolbar.setSubtitle("Không hoạt động");
                }
                toolbar.setSubtitleTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void sendMessage() {
        Editable messageEditable = etMessage.getText();
        if (messageEditable != null) {
            String messageText = messageEditable.toString().trim();
            if (!messageText.isEmpty()) {
                viewModel.sendMessage(chatId, messageText);
                etMessage.setText("");
            }
        }
    }

    @Override
    public void onPause() {
        if (emojiPopup != null) {
            emojiPopup.dismiss();
        }
        super.onPause();
    }

    // ... (Các hàm block/report đã được hoàn thiện) ...
    private void showChatOptionsDialog() {
        if (getContext() == null) return;
        final CharSequence[] options = {"Báo cáo cuộc trò chuyện", "Chặn người dùng này", "Hủy"};

        new AlertDialog.Builder(getContext())
                .setTitle("Tùy chọn")
                .setItems(options, (dialog, item) -> {
                    String selection = options[item].toString();
                    if (selection.equals("Báo cáo cuộc trò chuyện")) {
                        showReportReasonDialog();
                    } else if (selection.equals("Chặn người dùng này")) {
                        confirmAndBlockUser();
                    } else {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void showReportReasonDialog() {
        if (getContext() == null) return;
        final String[] reportReasons = {"Nội dung không phù hợp", "Spam hoặc Lừa đảo", "Quấy rối", "Lý do khác"};

        new AlertDialog.Builder(getContext())
                .setTitle("Chọn lý do báo cáo")
                .setItems(reportReasons, (dialog, which) -> sendConversationReport(reportReasons[which]))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void sendConversationReport(String reason) {
        String reporterId = FirebaseAuth.getInstance().getUid();
        if (reporterId == null || otherUserId == null) {
            Toast.makeText(getContext(), "Lỗi: Không thể xác định người dùng.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> report = new HashMap<>();
        report.put("reporterId", reporterId);
        report.put("reportedUserId", otherUserId);
        report.put("chatId", chatId);
        report.put("reason", reason);
        report.put("timestamp", FieldValue.serverTimestamp());
        report.put("type", "conversation");
        report.put("status", "pending");

        FirebaseFirestore.getInstance().collection("reports").add(report)
                .addOnSuccessListener(docRef -> Toast.makeText(getContext(), "Cảm ơn bạn đã gửi báo cáo.", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Gửi báo cáo thất bại.", Toast.LENGTH_SHORT).show());
    }

    private void confirmAndBlockUser() {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Chặn người dùng")
                .setMessage("Bạn có chắc chắn muốn chặn người dùng này không? Bạn sẽ không thấy tin nhắn của họ nữa và họ cũng không thể nhắn tin cho bạn.")
                .setPositiveButton("Chặn", (dialog, which) -> blockUser())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void blockUser() {
        String blockerId = FirebaseAuth.getInstance().getUid();
        if (blockerId == null || otherUserId == null) {
            Toast.makeText(getContext(), "Lỗi: Không thể thực hiện hành động chặn.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> blockData = new HashMap<>();
        blockData.put("blockerId", blockerId);
        blockData.put("blockedId", otherUserId);
        blockData.put("timestamp", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance().collection("blocked_users").add(blockData)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(getContext(), "Đã chặn người dùng thành công.", Toast.LENGTH_LONG).show();
                    navController.popBackStack();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Chặn người dùng thất bại.", Toast.LENGTH_SHORT).show());
    }
}