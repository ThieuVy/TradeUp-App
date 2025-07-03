package com.example.testapptradeup.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
import com.example.testapptradeup.adapters.ChatListAdapter;
import com.example.testapptradeup.models.Conversation;
import com.example.testapptradeup.viewmodels.ChatViewModel;
import com.google.android.material.appbar.MaterialToolbar;

public class ChatListFragment extends Fragment implements ChatListAdapter.OnConversationClickListener {

    private ChatViewModel viewModel;
    private NavController navController;
    private RecyclerView recyclerView;
    private ChatListAdapter adapter;
    private TextView emptyTextView;
    private MaterialToolbar toolbar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        initViews(view);
        setupRecyclerView();
        setupListeners();
        observeViewModel();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_chat_list);
        emptyTextView = view.findViewById(R.id.empty_chat_text);
        toolbar = view.findViewById(R.id.toolbar);
    }

    private void setupRecyclerView() {
        adapter = new ChatListAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());
    }

    private void observeViewModel() {
        viewModel.getConversationList().observe(getViewLifecycleOwner(), conversations -> {
            boolean isEmpty = conversations == null || conversations.isEmpty();
            recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            emptyTextView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

            if (!isEmpty) {
                for (Conversation c : conversations) {
                    if (c.getId() == null) {
                        // Log lỗi để kiểm tra
                        android.util.Log.w("ChatListFragment", "Conversation không có ID: " + c.getOtherUserName());
                    }
                }
                adapter.submitList(conversations);
            }
        });
    }

    @Override
    public void onConversationClick(Conversation conversation) {
        if (conversation.getId() == null || conversation.getId().isEmpty()) {
            Toast.makeText(getContext(), "Lỗi: Cuộc trò chuyện không có ID!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (conversation.getOtherUserName() == null) {
            // Có thể thêm xử lý nếu tên người dùng bị null
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy tên người dùng!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mở màn hình chi tiết cuộc trò chuyện
        // Truyền cả 2 tham số BẮT BUỘC: chatId và otherUserName
        ChatListFragmentDirections.ActionChatListFragmentToChatDetailFragment action =
                ChatListFragmentDirections.actionChatListFragmentToChatDetailFragment(
                        conversation.getId(),
                        conversation.getOtherUserName()
                );

        // Bạn có thể set các tham số TÙY CHỌN khác ở đây nếu có (ví dụ: listingId)
        // action.setListingId("some_id");

        navController.navigate(action);
    }
}
