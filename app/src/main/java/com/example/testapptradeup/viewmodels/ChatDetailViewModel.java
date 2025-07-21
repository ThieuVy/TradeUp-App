package com.example.testapptradeup.viewmodels;

import android.app.Application;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.example.testapptradeup.models.ChatItem;
import com.example.testapptradeup.models.ChatMessage;
import com.example.testapptradeup.models.Conversation;
import com.example.testapptradeup.models.DateSeparator;
import com.example.testapptradeup.models.UserStatus;
import com.example.testapptradeup.repositories.ChatRepository;
import com.example.testapptradeup.repositories.CloudinaryRepository;
import com.example.testapptradeup.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChatDetailViewModel extends AndroidViewModel {
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final CloudinaryRepository cloudinaryRepository;
    private final String currentUserId;
    private final MutableLiveData<String> chatIdTrigger = new MutableLiveData<>();
    private final LiveData<Conversation> chatData;
    private final LiveData<List<ChatItem>> messages;
    private final MutableLiveData<Boolean> isSendingImage = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private static final long TIME_GAP_THRESHOLD_MS = 30 * 60 * 1000; // 30 phút

    public ChatDetailViewModel(@NonNull Application application) {
        super(application);
        this.chatRepository = new ChatRepository();
        this.userRepository = new UserRepository();
        this.cloudinaryRepository = new CloudinaryRepository();
        this.currentUserId = FirebaseAuth.getInstance().getUid();

        chatData = Transformations.switchMap(chatIdTrigger, chatRepository::getConversationById);

        LiveData<List<ChatMessage>> rawMessages = Transformations.switchMap(chatIdTrigger, chatRepository::getMessagesForChat);
        messages = Transformations.map(rawMessages, this::addDateSeparators);
    }

    private List<ChatItem> addDateSeparators(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        List<ChatItem> itemsWithSeparators = new ArrayList<>();
        long lastTimestamp = 0;

        for (ChatMessage message : messages) {
            if (message.getTimestamp() != null) {
                long currentTimestamp = message.getTimestamp().getTime();
                if (lastTimestamp == 0 || (currentTimestamp - lastTimestamp) > TIME_GAP_THRESHOLD_MS) {
                    itemsWithSeparators.add(new DateSeparator(message.getTimestamp()));
                }
                itemsWithSeparators.add(message);
                lastTimestamp = currentTimestamp;
            } else {
                itemsWithSeparators.add(message);
            }
        }
        return itemsWithSeparators;
    }

    public void loadChat(String chatId) {
        if (!Objects.equals(chatId, chatIdTrigger.getValue())) {
            chatIdTrigger.setValue(chatId);
        }
    }

    public LiveData<Conversation> getChatData() {
        return chatData;
    }

    public LiveData<List<ChatItem>> getMessages() {
        return messages;
    }

    public LiveData<Boolean> getIsSendingImage() {
        return isSendingImage;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<UserStatus> getOtherUserStatus(String otherUserId) {
        return userRepository.getUserStatus(otherUserId);
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
        cloudinaryRepository.uploadProfileImage(imageUri, getApplication())
                .observeForever(result -> {
                    if (result.isSuccess()) {
                        String imageUrl = result.getData();
                        ChatMessage message = new ChatMessage(currentUserId, "[Hình ảnh]", imageUrl);
                        chatRepository.sendMessage(chatId, message);
                    } else {
                        errorMessage.setValue("Lỗi khi tải ảnh lên.");
                    }
                    isSendingImage.setValue(false);
                });
    }
}