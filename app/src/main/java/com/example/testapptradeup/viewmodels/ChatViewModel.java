package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.Conversation;
import com.example.testapptradeup.repositories.ChatRepository;
import java.util.List;

public class ChatViewModel extends ViewModel {
    private final ChatRepository chatRepository;
    // Sửa: Đổi tên biến cho nhất quán
    public final LiveData<List<Conversation>> conversationList;

    public ChatViewModel() {
        this.chatRepository = new ChatRepository();
        this.conversationList = chatRepository.getConversations();
    }

    // Getter để Fragment có thể truy cập
    public LiveData<List<Conversation>> getConversationList() {
        return conversationList;
    }
}