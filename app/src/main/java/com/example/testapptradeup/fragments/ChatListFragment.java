package com.example.testapptradeup.fragments;

import android.app.AlertDialog;
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
import androidx.recyclerview.widget.ItemTouchHelper;
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
        viewModel = new ViewModelProvider(requireActivity()).get(ChatViewModel.class);
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

        // Thêm logic vuốt để xóa
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Conversation conversationToDelete = adapter.getCurrentList().get(position);

                // Hiển thị dialog xác nhận
                new AlertDialog.Builder(requireContext())
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc chắn muốn xóa cuộc trò chuyện này không? Hành động này không thể hoàn tác.")
                        .setPositiveButton("Xóa", (dialog, which) -> viewModel.deleteConversation(conversationToDelete.getId()))
                        .setNegativeButton("Hủy", (dialog, which) -> {
                            // Hoàn tác lại item nếu người dùng hủy
                            adapter.notifyItemChanged(position);
                        })
                        .setCancelable(false)
                        .show();
            }
        };
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);
    }

    private void setupListeners() {
        // Gán sự kiện cho nút back trên toolbar
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());
    }

    private void observeViewModel() {
        viewModel.getConversationList().observe(getViewLifecycleOwner(), conversations -> {
            boolean isEmpty = conversations == null || conversations.isEmpty();

            // Xử lý ẩn/hiện UI
            recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            emptyTextView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

            if (!isEmpty) {
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
        ChatListFragmentDirections.ActionChatListToChatDetail action =
                ChatListFragmentDirections.actionChatListToChatDetail(
                        conversation.getId(),
                        conversation.getOtherUserName()
                );

        // Bạn có thể set các tham số TÙY CHỌN khác ở đây nếu có (ví dụ: listingId)
        // action.setListingId("some_id");

        navController.navigate(action);
    }
}
