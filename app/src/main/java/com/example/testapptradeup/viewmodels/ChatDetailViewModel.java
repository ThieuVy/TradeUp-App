package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.ChatMessage;
import com.example.testapptradeup.models.Conversation; // Thêm import
import com.example.testapptradeup.repositories.ChatRepository;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;
import java.util.Objects;

public class ChatDetailViewModel extends ViewModel {
    private final ChatRepository chatRepository;
    private final String currentUserId;

    // Trigger để bắt đầu tải dữ liệu cho một cuộc trò chuyện
    private final MutableLiveData<String> chatIdTrigger = new MutableLiveData<>();

    // LiveData cho dữ liệu cuộc trò chuyện (để lấy otherUserId)
    private final LiveData<Conversation> chatData;

    // LiveData cho danh sách tin nhắn
    private final LiveData<List<ChatMessage>> messages;

    public ChatDetailViewModel() {
        this.chatRepository = new ChatRepository();
        this.currentUserId = FirebaseAuth.getInstance().getUid();

        // Sử dụng switchMap để tự động tải dữ liệu khi chatIdTrigger thay đổi
        chatData = Transformations.switchMap(chatIdTrigger, chatRepository::getConversationById);
        messages = Transformations.switchMap(chatIdTrigger, chatRepository::getMessagesForChat);
    }

    /**
     * Fragment gọi hàm này để bắt đầu tải/lắng nghe dữ liệu cho một cuộc trò chuyện.
     * @param chatId ID của cuộc trò chuyện.
     */
    public void loadChat(String chatId) {
        if (!Objects.equals(chatId, chatIdTrigger.getValue())) {
            chatIdTrigger.setValue(chatId);
        }
    }

    /**
     * Trả về LiveData chứa thông tin cuộc trò chuyện.
     */
    public LiveData<Conversation> getChatData() {
        return chatData;
    }

    /**
     * Trả về LiveData chứa danh sách tin nhắn.
     */
    public LiveData<List<ChatMessage>> getMessages() {
        return messages;
    }

    /**
     * Gửi một tin nhắn mới.
     * @param chatId ID của cuộc trò chuyện.
     * @param text Nội dung tin nhắn.
     */
    public void sendMessage(String chatId, String text) {
        if (text == null || text.trim().isEmpty() || currentUserId == null) {
            return;
        }
        ChatMessage message = new ChatMessage(currentUserId, text.trim(), null);
        chatRepository.sendMessage(chatId, message);
    }
}