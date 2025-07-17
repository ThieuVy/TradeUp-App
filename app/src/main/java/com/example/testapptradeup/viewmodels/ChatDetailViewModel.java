package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.ChatMessage;
import com.example.testapptradeup.models.Conversation; // Import Conversation
import com.example.testapptradeup.repositories.ChatRepository;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;
import java.util.Objects;

public class ChatDetailViewModel extends ViewModel {
    private final ChatRepository chatRepository;
    private final String currentUserId;

    private final MutableLiveData<String> chatIdTrigger = new MutableLiveData<>();

    // LiveData cho dữ liệu cuộc trò chuyện (để lấy otherUserId)
    private final LiveData<Conversation> chatData;

    private final LiveData<List<ChatMessage>> messages;

    public ChatDetailViewModel() {
        this.chatRepository = new ChatRepository();
        this.currentUserId = FirebaseAuth.getInstance().getUid();

        // Sử dụng switchMap để tự động tải dữ liệu khi chatIdTrigger thay đổi
        chatData = Transformations.switchMap(chatIdTrigger, chatRepository::getConversationById);
        messages = Transformations.switchMap(chatIdTrigger, chatRepository::getMessagesForChat);
    }

    public void loadChat(String chatId) {
        if (!Objects.equals(chatId, chatIdTrigger.getValue())) {
            chatIdTrigger.setValue(chatId);
        }
    }

    // Getter cho chatData
    /**
     * Trả về LiveData chứa thông tin cuộc trò chuyện.
     * Fragment sẽ observe LiveData này để lấy danh sách members.
     */
    public LiveData<Conversation> getChatData() {
        return chatData;
    }

    public LiveData<List<ChatMessage>> getMessages() {
        return messages;
    }

    public void sendMessage(String chatId, String text) {
        if (text == null || text.trim().isEmpty() || currentUserId == null) {
            return;
        }
        ChatMessage message = new ChatMessage(currentUserId, text.trim(), null);
        chatRepository.sendMessage(chatId, message);
    }
}