package com.example.testapptradeup.repositories;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.testapptradeup.models.ChatMessage;
import com.example.testapptradeup.models.Conversation;
import com.example.testapptradeup.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatRepository {
    private static final String TAG = "ChatRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String currentUserId = FirebaseAuth.getInstance().getUid();

    /**
     * Lắng nghe danh sách các cuộc trò chuyện của người dùng hiện tại,
     * đồng thời lấy thông tin của người dùng đối diện.
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
                        Log.e(TAG, "Lắng nghe danh sách cuộc trò chuyện thất bại: ", error);
                        conversationsData.postValue(new ArrayList<>());
                        return;
                    }
                    if (snapshots == null || snapshots.isEmpty()) {
                        conversationsData.postValue(new ArrayList<>());
                        return;
                    }

                    List<Conversation> tempConversations = new ArrayList<>();
                    // Dùng AtomicInteger để đếm số lượng tác vụ bất đồng bộ (lấy thông tin user)
                    AtomicInteger pendingUserFetches = new AtomicInteger(snapshots.size());

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Conversation convo = doc.toObject(Conversation.class);
                        convo.setId(doc.getId());

                        List<String> members = (List<String>) doc.get("members");
                        if (members != null) {
                            // Tìm ID của người còn lại
                            String otherUserId = members.stream()
                                    .filter(id -> !id.equals(currentUserId))
                                    .findFirst()
                                    .orElse(null);

                            if (otherUserId != null) {
                                // Thực hiện truy vấn con để lấy thông tin của người đó
                                db.collection("users").document(otherUserId).get()
                                        .addOnSuccessListener(userDoc -> {
                                            if (userDoc.exists()) {
                                                User otherUser = userDoc.toObject(User.class);
                                                if (otherUser != null) {
                                                    // Gán thông tin lấy được vào đối tượng Conversation
                                                    convo.setOtherUserId(otherUserId);
                                                    convo.setOtherUserName(otherUser.getName());
                                                    convo.setOtherUserAvatarUrl(otherUser.getProfileImageUrl());
                                                }
                                            }
                                            // Dù lấy được hay không, vẫn thêm vào list và giảm bộ đếm
                                            tempConversations.add(convo);
                                            if (pendingUserFetches.decrementAndGet() == 0) {
                                                // Khi tất cả các tác vụ con đã xong, post giá trị lên LiveData
                                                conversationsData.postValue(tempConversations);
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Lấy thông tin người dùng thất bại cho " + otherUserId, e);
                                            // Vẫn thêm convo vào và giảm bộ đếm
                                            tempConversations.add(convo);
                                            if (pendingUserFetches.decrementAndGet() == 0) {
                                                conversationsData.postValue(tempConversations);
                                            }
                                        });
                            } else {
                                // Xử lý trường hợp chat với chính mình hoặc dữ liệu lỗi
                                tempConversations.add(convo);
                                if (pendingUserFetches.decrementAndGet() == 0) {
                                    conversationsData.postValue(tempConversations);
                                }
                            }
                        }
                    }
                });

        return conversationsData;
    }

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
                .limitToLast(50) // Giới hạn 50 tin nhắn cuối để tối ưu hiệu năng
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
     * Gửi một tin nhắn mới và cập nhật thông tin cuộc trò chuyện một cách an toàn (atomic).
     * @param chatId ID của cuộc trò chuyện.
     * @param message Đối tượng ChatMessage cần gửi.
     * @return LiveData<Boolean> để báo trạng thái thành công/thất bại.
     */
    public LiveData<Boolean> sendMessage(String chatId, ChatMessage message) {
        MutableLiveData<Boolean> sendStatus = new MutableLiveData<>();
        if (chatId == null || message == null || message.getText() == null || message.getText().trim().isEmpty()) {
            sendStatus.setValue(false);
            return sendStatus;
        }

        // Tạo một reference cho tin nhắn mới trong sub-collection
        DocumentReference newMessageRef = db.collection("chats").document(chatId)
                .collection("messages").document();
        message.setMessageId(newMessageRef.getId());

        // Tạo một reference đến document cuộc trò chuyện cha
        DocumentReference chatDocRef = db.collection("chats").document(chatId);

        // Tạo một WriteBatch
        WriteBatch batch = db.batch();

        // 1. Thêm tin nhắn mới vào batch
        batch.set(newMessageRef, message);

        // 2. Cập nhật cuộc trò chuyện cha vào batch
        Map<String, Object> chatUpdates = new HashMap<>();
        chatUpdates.put("lastMessage", message.getText());
        chatUpdates.put("timestamp", FieldValue.serverTimestamp());
        chatUpdates.put("lastMessageSenderId", message.getSenderId());
        batch.update(chatDocRef, chatUpdates);

        // 3. Thực thi batch
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Gửi tin nhắn và cập nhật cuộc trò chuyện thành công.");
                    sendStatus.setValue(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi thực thi batch gửi tin nhắn.", e);
                    sendStatus.setValue(false);
                });

        return sendStatus;
    }
    /**
     * Tìm một cuộc trò chuyện hiện có giữa người dùng hiện tại và một người dùng khác.
     * Nếu không tồn tại, tạo một cuộc trò chuyện mới.
     * @param otherUserId ID của người dùng cần chat cùng.
     * @return LiveData chứa ID của cuộc trò chuyện (chatId).
     */
    public LiveData<String> findOrCreateChat(String otherUserId) {
        MutableLiveData<String> chatIdLiveData = new MutableLiveData<>();
        if (currentUserId == null || otherUserId == null || currentUserId.equals(otherUserId)) {
            chatIdLiveData.setValue(null); // Không thể tự chat với chính mình
            return chatIdLiveData;
        }

        // Tạo một danh sách members để truy vấn
        List<String> membersToFind = Arrays.asList(currentUserId, otherUserId);

        // Truy vấn để tìm cuộc trò chuyện có chính xác 2 thành viên này
        db.collection("chats")
                .whereEqualTo("members", membersToFind)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshots = task.getResult();
                        if (snapshots != null && !snapshots.isEmpty()) {
                            // Đã tìm thấy cuộc trò chuyện, trả về ID của nó
                            String existingChatId = snapshots.getDocuments().get(0).getId();
                            Log.d(TAG, "Đã tìm thấy cuộc trò chuyện có sẵn: " + existingChatId);
                            chatIdLiveData.postValue(existingChatId);
                        } else {
                            // Không tìm thấy, tiến hành tạo mới
                            Log.d(TAG, "Không tìm thấy cuộc trò chuyện, đang tạo mới...");
                            createChat(otherUserId, chatIdLiveData);
                        }
                    } else {
                        Log.e(TAG, "Lỗi khi tìm cuộc trò chuyện: ", task.getException());
                        chatIdLiveData.postValue(null);
                    }
                });

        return chatIdLiveData;
    }

    private void createChat(String otherUserId, MutableLiveData<String> chatIdLiveData) {
        DocumentReference newChatRef = db.collection("chats").document();

        Map<String, Object> chatData = new HashMap<>();
        chatData.put("members", Arrays.asList(currentUserId, otherUserId));
        chatData.put("timestamp", FieldValue.serverTimestamp());
        chatData.put("lastMessage", "Hãy bắt đầu cuộc trò chuyện!"); // Tin nhắn khởi tạo

        newChatRef.set(chatData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Tạo cuộc trò chuyện mới thành công: " + newChatRef.getId());
                    chatIdLiveData.postValue(newChatRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi tạo cuộc trò chuyện mới: ", e);
                    chatIdLiveData.postValue(null);
                });
    }
}