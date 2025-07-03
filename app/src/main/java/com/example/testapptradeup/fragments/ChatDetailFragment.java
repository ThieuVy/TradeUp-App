package com.example.testapptradeup.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ChatDetailViewModel.class);

        // Lấy dữ liệu được truyền từ Fragment trước (ví dụ: ChatListFragment)
        if (getArguments() != null) {
            chatId = ChatDetailFragmentArgs.fromBundle(getArguments()).getChatId();
            otherUserName = ChatDetailFragmentArgs.fromBundle(getArguments()).getOtherUserName();
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

        // Cập nhật tên người dùng trên Toolbar
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
}