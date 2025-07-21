package com.example.testapptradeup.viewmodels;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.Transformations;

import com.example.testapptradeup.models.ChatMessage;
import com.example.testapptradeup.models.Conversation;
import com.example.testapptradeup.repositories.ChatRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Objects;

public class ChatViewModel extends ViewModel {
    private final ChatRepository chatRepository;
    @Nullable
    private final String currentUserId;

    // LiveData cho danh sách tất cả cuộc trò chuyện của người dùng
    private final LiveData<List<Conversation>> conversationList;

    // LiveData để lấy tin nhắn cho một cuộc trò chuyện cụ thể đang được xem
    private final MutableLiveData<String> selectedChatId = new MutableLiveData<>();
    private final LiveData<List<ChatMessage>> messagesForSelectedChat;

    public ChatViewModel() {
        this.chatRepository = new ChatRepository();
        this.currentUserId = FirebaseAuth.getInstance().getUid();

        // Lấy danh sách cuộc trò chuyện từ repository
        this.conversationList = chatRepository.getConversations();

        // Sử dụng switchMap để tự động lắng nghe tin nhắn khi selectedChatId thay đổi
        // Khi UI gọi setSelectedChatId(newId), switchMap sẽ tự hủy lắng nghe chatId cũ
        // và bắt đầu lắng nghe chatId mới.
        this.messagesForSelectedChat = Transformations.switchMap(selectedChatId, chatId -> {
            if (chatId == null || chatId.isEmpty()) {
                // Trả về LiveData rỗng nếu không có chat nào được chọn
                MutableLiveData<List<ChatMessage>> emptyList = new MutableLiveData<>();
                emptyList.setValue(new ArrayList<>());
                return emptyList;
            }
            return chatRepository.getMessagesForChat(chatId);
        });
    }

    /**
     * Trả về LiveData chứa danh sách các cuộc trò chuyện để UI (vd: ConversationListFragment) lắng nghe.
     */
    public LiveData<List<Conversation>> getConversationList() {
        return conversationList;
    }

    /**
     * Trả về LiveData chứa danh sách các tin nhắn của cuộc trò chuyện đang được chọn
     * để UI (vd: ChatFragment) lắng nghe.
     */
    public LiveData<List<ChatMessage>> getMessagesForSelectedChat() {
        return messagesForSelectedChat;
    }

    /**
     * Đặt ID của cuộc trò chuyện đang được xem.
     * Thao tác này sẽ trigger switchMap để bắt đầu lấy tin nhắn cho cuộc trò chuyện đó.
     * @param chatId ID của cuộc trò chuyện. Có thể là null để dừng lắng nghe.
     */
    public void setSelectedChatId(@Nullable String chatId) {
        // Chỉ cập nhật nếu ID mới khác ID cũ để tránh trigger không cần thiết
        if (!Objects.equals(selectedChatId.getValue(), chatId)) {
            selectedChatId.setValue(chatId);
        }
    }

    /**
     * Xây dựng và gửi một tin nhắn mới.
     * @param text Nội dung tin nhắn.
     * @return LiveData<Boolean> để UI biết khi nào gửi xong (thành công/thất bại) và có thể
     *         hiển thị trạng thái (ví dụ: dấu tick, dấu chấm than).
     */
    public LiveData<Boolean> sendMessage(String text) {
        String chatId = selectedChatId.getValue();
        // Kiểm tra điều kiện đầu vào
        if (currentUserId == null || chatId == null || chatId.isEmpty() || text == null || text.trim().isEmpty()) {
            MutableLiveData<Boolean> failedStatus = new MutableLiveData<>();
            failedStatus.setValue(false);
            return failedStatus;
        }

        // Tạo đối tượng ChatMessage
        ChatMessage message = new ChatMessage();
        message.setText(text.trim());
        message.setSenderId(currentUserId);
        message.setTimestamp(new Date()); // Timestamp phía client, sẽ được ghi đè bởi server timestamp trong repo

        // Gọi repository để gửi tin nhắn
        return chatRepository.sendMessage(chatId, message);
    }
    /**
     * Yêu cầu repository xóa một cuộc trò chuyện.
     *
     * @param chatId ID của cuộc trò chuyện cần xóa.
     */
    public void deleteConversation(String chatId) {
        chatRepository.deleteConversation(chatId);
    }
}