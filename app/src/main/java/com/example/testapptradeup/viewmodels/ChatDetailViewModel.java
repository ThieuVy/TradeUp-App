package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.ChatMessage;
import com.example.testapptradeup.repositories.ChatRepository;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;

public class ChatDetailViewModel extends ViewModel {
    private final ChatRepository chatRepository;
    private final String currentUserId;
    public LiveData<List<ChatMessage>> messages;

    public ChatDetailViewModel() {
        this.chatRepository = new ChatRepository();
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    public void loadMessages(String chatId) {
        messages = chatRepository.getMessagesForChat(chatId);
    }

    public void sendMessage(String chatId, String text) {
        if (text == null || text.trim().isEmpty() || currentUserId == null) {
            return;
        }
        ChatMessage message = new ChatMessage(currentUserId, text.trim(), null);
        chatRepository.sendMessage(chatId, message);
    }
}