package com.example.testapptradeup.repositories;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.testapptradeup.models.ChatMessage;
import com.example.testapptradeup.models.Conversation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRepository {
    private static final String TAG = "ChatRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String currentUserId = FirebaseAuth.getInstance().getUid();

    /**
     * Lắng nghe danh sách các cuộc trò chuyện của người dùng hiện tại trong thời gian thực.
     * @return LiveData chứa danh sách Conversation.
     */
    public LiveData<List<Conversation>> getConversations() {
        MutableLiveData<List<Conversation>> conversationsData = new MutableLiveData<>();
        if (currentUserId == null) {
            conversationsData.setValue(new ArrayList<>());
            return conversationsData;
        }

        db.collection("chats")
                .whereArrayContains("members", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi lắng nghe conversations: ", error);
                        conversationsData.setValue(new ArrayList<>());
                        return;
                    }
                    if (snapshots == null) {
                        conversationsData.setValue(new ArrayList<>());
                        return;
                    }

                    List<Conversation> conversations = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Conversation convo = doc.toObject(Conversation.class);
                        convo.setId(doc.getId());
                        conversations.add(convo);
                    }
                    conversationsData.setValue(conversations);
                });

        return conversationsData;
    }

    // ========== PHẦN CODE ĐƯỢC HOÀN THIỆN ==========

    /**
     * Lắng nghe danh sách tin nhắn trong một cuộc trò chuyện cụ thể trong thời gian thực.
     * @param chatId ID của cuộc trò chuyện.
     * @return LiveData chứa danh sách ChatMessage.
     */
    public LiveData<List<ChatMessage>> getMessagesForChat(String chatId) {
        MutableLiveData<List<ChatMessage>> messagesData = new MutableLiveData<>();
        if (chatId == null || chatId.isEmpty()) {
            messagesData.setValue(new ArrayList<>());
            return messagesData;
        }

        db.collection("chats").document(chatId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi lắng nghe messages: ", error);
                        messagesData.setValue(new ArrayList<>());
                        return;
                    }
                    if (snapshots == null) {
                        messagesData.setValue(new ArrayList<>());
                        return;
                    }

                    List<ChatMessage> messages = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        ChatMessage message = doc.toObject(ChatMessage.class);
                        message.setMessageId(doc.getId());
                        messages.add(message);
                    }
                    messagesData.setValue(messages);
                });
        return messagesData;
    }

    /**
     * Gửi một tin nhắn mới và cập nhật thông tin cuộc trò chuyện.
     * @param chatId ID của cuộc trò chuyện.
     * @param message Đối tượng ChatMessage cần gửi.
     */
    public void sendMessage(String chatId, ChatMessage message) {
        if (chatId == null || message == null || message.getText().isEmpty()) {
            return;
        }

        // 1. Thêm tin nhắn mới vào sub-collection "messages"
        db.collection("chats").document(chatId).collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Gửi tin nhắn thành công: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi gửi tin nhắn", e));

        // 2. Cập nhật thông tin của cuộc trò chuyện cha
        Map<String, Object> chatUpdates = new HashMap<>();
        chatUpdates.put("lastMessage", message.getText());
        chatUpdates.put("timestamp", FieldValue.serverTimestamp()); // Dùng server timestamp để đồng bộ
        chatUpdates.put("lastMessageSenderId", message.getSenderId());

        db.collection("chats").document(chatId)
                .update(chatUpdates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Cập nhật cuộc trò chuyện thành công."))
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi cập nhật cuộc trò chuyện", e));
    }
    // ========== KẾT THÚC PHẦN CODE HOÀN THIỆN ==========
}