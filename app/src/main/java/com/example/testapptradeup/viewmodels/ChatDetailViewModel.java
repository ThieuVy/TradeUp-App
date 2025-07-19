package com.example.testapptradeup.viewmodels;

import android.app.Application;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.example.testapptradeup.models.ChatMessage;
import com.example.testapptradeup.models.Conversation;
import com.example.testapptradeup.repositories.ChatRepository;
import com.example.testapptradeup.repositories.CloudinaryRepository;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;
import java.util.Objects;

public class ChatDetailViewModel extends AndroidViewModel {
    private final ChatRepository chatRepository;
    private final CloudinaryRepository cloudinaryRepository;
    private final String currentUserId;
    private final MutableLiveData<String> chatIdTrigger = new MutableLiveData<>();
    private final LiveData<Conversation> chatData;
    private final LiveData<List<ChatMessage>> messages;
    private final MutableLiveData<Boolean> isSendingImage = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ChatDetailViewModel(@NonNull Application application) {
        super(application);
        this.chatRepository = new ChatRepository();
        this.cloudinaryRepository = new CloudinaryRepository();
        this.currentUserId = FirebaseAuth.getInstance().getUid();

        chatData = Transformations.switchMap(chatIdTrigger, chatRepository::getConversationById);
        messages = Transformations.switchMap(chatIdTrigger, chatRepository::getMessagesForChat);
    }

    public void loadChat(String chatId) {
        if (!Objects.equals(chatId, chatIdTrigger.getValue())) {
            chatIdTrigger.setValue(chatId);
        }
    }

    public LiveData<Conversation> getChatData() {
        return chatData;
    }

    public LiveData<List<ChatMessage>> getMessages() {
        return messages;
    }

    public LiveData<Boolean> getIsSendingImage() {
        return isSendingImage;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void sendMessage(String chatId, String text) {
        if (text == null || text.trim().isEmpty() || currentUserId == null) {
            return;
        }
        ChatMessage message = new ChatMessage(currentUserId, text.trim(), null);
        chatRepository.sendMessage(chatId, message);
    }

    public void sendImageMessage(String chatId, Uri imageUri) {
        if (imageUri == null || currentUserId == null) {
            return;
        }
        isSendingImage.setValue(true);

        // Bước 1: Tải ảnh lên Cloudinary
        cloudinaryRepository.uploadProfileImage(imageUri, getApplication())
                .observeForever(result -> {
                    if (result.isSuccess()) {
                        String imageUrl = result.getData();
                        // Bước 2: Tạo tin nhắn với URL ảnh và gửi đi
                        ChatMessage message = new ChatMessage(currentUserId, "[Hình ảnh]", imageUrl);
                        chatRepository.sendMessage(chatId, message);
                    } else {
                        errorMessage.setValue("Lỗi khi tải ảnh lên.");
                        // TODO: Xử lý lỗi (ví dụ: hiển thị Toast thông qua một LiveData khác)
                    }
                    isSendingImage.setValue(false);
                });
    }
}