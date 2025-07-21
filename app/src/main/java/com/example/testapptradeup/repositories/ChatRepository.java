package com.example.testapptradeup.repositories;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.testapptradeup.models.ChatMessage;
import com.example.testapptradeup.models.Conversation;
import com.example.testapptradeup.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
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

        db.collection("chats").document(chatId)
                .collection("messages").add(message) // Chỉ cần thêm tin nhắn mới
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Gửi tin nhắn thành công, trigger sẽ xử lý phần còn lại.");
                    sendStatus.setValue(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi gửi tin nhắn.", e);
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
    public LiveData<String> findOrCreateChat(String otherUserId, String listingId) {
        MutableLiveData<String> chatIdLiveData = new MutableLiveData<>();
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null || otherUserId == null || currentUserId.equals(otherUserId)) {
            chatIdLiveData.setValue(null);
            return chatIdLiveData;
        }

        // Truy vấn để tìm cuộc trò chuyện có cả 2 thành viên VÀ listingId
        db.collection("chats")
                .whereArrayContains("members", currentUserId)
                .whereEqualTo("listingId", listingId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshots = task.getResult();
                        // Lọc thêm ở client để đảm bảo cả otherUserId cũng có trong members
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            List<String> members = (List<String>) doc.get("members");
                            if (members != null && members.contains(otherUserId)) {
                                chatIdLiveData.postValue(doc.getId());
                                return; // Tìm thấy, thoát khỏi vòng lặp
                            }
                        }

                        // Nếu vòng lặp kết thúc mà không tìm thấy -> tạo mới
                        Log.d(TAG, "Không tìm thấy cuộc trò chuyện, đang tạo mới...");
                        createChat(otherUserId, listingId, chatIdLiveData);

                    } else {
                        Log.e(TAG, "Lỗi khi tìm cuộc trò chuyện: ", task.getException());
                        chatIdLiveData.postValue(null);
                    }
                });
        return chatIdLiveData;
    }

    private void createChat(String otherUserId, String listingId, MutableLiveData<String> chatIdLiveData) {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        DocumentReference newChatRef = db.collection("chats").document();

        Map<String, Object> chatData = new HashMap<>();
        chatData.put("members", Arrays.asList(currentUserId, otherUserId));
        chatData.put("timestamp", FieldValue.serverTimestamp());
        chatData.put("listingId", listingId);
        chatData.put("lastMessage", "Cuộc trò chuyện về sản phẩm đã bắt đầu."); // Thay đổi tin nhắn mặc định

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

    /**
     * Lắng nghe thông tin của một cuộc trò chuyện cụ thể bằng ID.
     * @param chatId ID của cuộc trò chuyện.
     * @return LiveData chứa đối tượng Conversation.
     */
    public LiveData<Conversation> getConversationById(String chatId) {
        MutableLiveData<Conversation> conversationData = new MutableLiveData<>();
        if (chatId == null || chatId.isEmpty()) {
            conversationData.setValue(null);
            return conversationData;
        }

        db.collection("chats").document(chatId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi khi lấy dữ liệu cuộc trò chuyện: ", error);
                        conversationData.setValue(null);
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        Conversation conversation = snapshot.toObject(Conversation.class);
                        if (conversation != null) {
                            conversation.setId(snapshot.getId());
                        }
                        conversationData.setValue(conversation);
                    } else {
                        conversationData.setValue(null);
                    }
                });
        return conversationData;
    }

    /**
     * Xóa một cuộc trò chuyện, bao gồm tất cả tin nhắn con.
     * @param chatId ID của cuộc trò chuyện cần xóa.
     * @return LiveData<Boolean> để báo trạng thái thành công/thất bại.
     */
    public LiveData<Boolean> deleteConversation(String chatId) {
        MutableLiveData<Boolean> deleteStatus = new MutableLiveData<>();
        if (chatId == null || chatId.isEmpty()) {
            deleteStatus.setValue(false);
            return deleteStatus;
        }

        // Bước 1: Lấy tất cả các tin nhắn trong subcollection để xóa
        db.collection("chats").document(chatId).collection("messages").get()
                .addOnSuccessListener(messageSnapshots -> {
                    // Bước 2: Tạo một WriteBatch để thực hiện nhiều thao tác cùng lúc
                    WriteBatch batch = db.batch();

                    // Bước 3: Thêm lệnh xóa cho từng tin nhắn vào batch
                    for (QueryDocumentSnapshot messageDoc : messageSnapshots) {
                        batch.delete(messageDoc.getReference());
                    }

                    // Bước 4: Thêm lệnh xóa cho document cuộc trò chuyện chính vào batch
                    DocumentReference chatRef = db.collection("chats").document(chatId);
                    batch.delete(chatRef);

                    // Bước 5: Thực thi batch
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Đã xóa cuộc trò chuyện và tất cả tin nhắn thành công: " + chatId);
                                deleteStatus.setValue(true);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Lỗi khi commit batch xóa cuộc trò chuyện: ", e);
                                deleteStatus.setValue(false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy danh sách tin nhắn để xóa: ", e);
                    deleteStatus.setValue(false);
                });

        return deleteStatus;
    }
}