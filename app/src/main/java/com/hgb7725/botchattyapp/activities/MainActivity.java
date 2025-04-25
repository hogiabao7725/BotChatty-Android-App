package com.hgb7725.botchattyapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.hgb7725.botchattyapp.R;
import com.hgb7725.botchattyapp.databinding.ActivityMainBinding;
import com.hgb7725.botchattyapp.listeners.ConversionListener;
import com.hgb7725.botchattyapp.models.ChatMessage;
import com.hgb7725.botchattyapp.models.User;
import com.hgb7725.botchattyapp.utilities.Constants;
import com.hgb7725.botchattyapp.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import com.hgb7725.botchattyapp.adapters.RencentConversationsAdapter;

public class MainActivity extends BaseActivity implements ConversionListener {

    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private List<ChatMessage> conversations;
    private RencentConversationsAdapter conversationsAdapter;
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        init();
        loadUserDetails();
        getToken();
        setListeners();
        listenConversations();
    }

    private void init() {
        conversations = new ArrayList<>();
        conversationsAdapter = new RencentConversationsAdapter(conversations, this);
        binding.conversationsRecyclerView.setAdapter(conversationsAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void setListeners() {
        binding.imageSignOut.setOnClickListener(v -> signOut());
        binding.fabNewChat.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), UsersActivity.class)));
    }

    private void loadUserDetails() {
        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
    }

    private void listenConversations() {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    
                    // Kiểm tra xem conversation đã tồn tại trong danh sách chưa
                    boolean conversationExists = false;
                    for (ChatMessage existingChat : conversations) {
                        if ((existingChat.getSenderId().equals(senderId) && existingChat.getReceiverId().equals(receiverId)) ||
                            (existingChat.getSenderId().equals(receiverId) && existingChat.getReceiverId().equals(senderId))) {
                            conversationExists = true;
                            break;
                        }
                    }
                    
                    if (!conversationExists) {
                        ChatMessage chatMessage = new ChatMessage();
                        chatMessage.setSenderId(senderId);
                        chatMessage.setReceiverId(receiverId);

                        if (preferenceManager.getString(Constants.KEY_USER_ID).equals(senderId)) {
                            chatMessage.setConversionImage(documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE));
                            chatMessage.setConversionName(documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME));
                            chatMessage.setConversionId(documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID));
                        } else {
                            chatMessage.setConversionImage(documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE));
                            chatMessage.setConversionName(documentChange.getDocument().getString(Constants.KEY_SENDER_NAME));
                            chatMessage.setConversionId(documentChange.getDocument().getString(Constants.KEY_SENDER_ID));
                        }

                        chatMessage.setMessage(documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE));
                        chatMessage.setDateObject(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                        
                        // Lấy ID người gửi tin nhắn cuối cùng
                        String lastSenderId = documentChange.getDocument().getString("lastSenderId");
                        chatMessage.setLastSenderId(lastSenderId);
                        
                        // Lấy giá trị unreadCount từ Firestore nếu có
                        if (documentChange.getDocument().getLong("unreadCount") != null) {
                            chatMessage.setUnreadCount(documentChange.getDocument().getLong("unreadCount").intValue());
                        }
                        
                        conversations.add(chatMessage);
                    }
                } else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                    String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    String lastMessage = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                    Date timestamp = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    String lastSenderId = documentChange.getDocument().getString("lastSenderId");
                    
                    for (int i = 0; i < conversations.size(); i++) {
                        if ((conversations.get(i).getSenderId().equals(senderId) && conversations.get(i).getReceiverId().equals(receiverId)) ||
                            (conversations.get(i).getSenderId().equals(receiverId) && conversations.get(i).getReceiverId().equals(senderId))) {
                            conversations.get(i).setMessage(lastMessage);
                            conversations.get(i).setDateObject(timestamp);
                            conversations.get(i).setLastSenderId(lastSenderId);
                            
                            // Cập nhật sender và receiver image/name nếu cần
                            if (preferenceManager.getString(Constants.KEY_USER_ID).equals(senderId)) {
                                conversations.get(i).setConversionImage(documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE));
                                conversations.get(i).setConversionName(documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME));
                            } else {
                                conversations.get(i).setConversionImage(documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE));
                                conversations.get(i).setConversionName(documentChange.getDocument().getString(Constants.KEY_SENDER_NAME));
                            }
                            
                            // Cập nhật số tin nhắn chưa đọc từ Firestore
                            if (documentChange.getDocument().getLong("unreadCount") != null) {
                                conversations.get(i).setUnreadCount(documentChange.getDocument().getLong("unreadCount").intValue());
                            }
                            break;
                        }
                    }
                }
            }
            Collections.sort(conversations, (obj1, obj2) ->
                    obj2.getDateObject().compareTo(obj1.getDateObject()));
            conversationsAdapter.notifyDataSetChanged();
            binding.conversationsRecyclerView.smoothScrollToPosition(0);
            binding.conversationsRecyclerView.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    };

    private void getToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                //.addOnSuccessListener(unused -> showToast("Token updated Successfully"))
                .addOnFailureListener(e -> showToast("Unable to update token"));
    }

    private void signOut() {
        showToast("Signing Out ...");
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Unable to Sign Out."));
    }

    @Override
    public void onConversionClicked(User user) {
        // Reset số tin nhắn chưa đọc khi người dùng nhấp vào cuộc trò chuyện
        resetUnreadCountForSender(user.getId());
        
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
    }
    
    // Phương thức kiểm tra xem người dùng đang ở trong cuộc trò chuyện với người gửi hay không
    private boolean isInChatScreenWith(String senderId) {
        // Lưu trạng thái cuộc trò chuyện hiện tại
        String currentChatUserId = preferenceManager.getString("current_chat_user_id");
        return currentChatUserId != null && currentChatUserId.equals(senderId);
    }
    
    // Phương thức cập nhật số tin nhắn chưa đọc trong Firestore
    private void updateUnreadCountInFirestore(String senderId, String receiverId, int unreadCount) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        DocumentReference documentReference = task.getResult().getDocuments().get(0).getReference();
                        documentReference.update("unreadCount", unreadCount);
                    }
                });
    }
    
    // Phương thức reset số tin nhắn chưa đọc khi người dùng mở cuộc trò chuyện
    private void resetUnreadCountForSender(String senderId) {
        // Tìm cuộc trò chuyện trong danh sách và reset số tin nhắn chưa đọc
        for (ChatMessage conversation : conversations) {
            if (conversation.getConversionId().equals(senderId)) {
                // Reset unreadCount trong model
                conversation.setUnreadCount(0);
                
                // Reset unreadCount trong Firestore
                updateUnreadCountInFirestore(senderId, preferenceManager.getString(Constants.KEY_USER_ID), 0);
                
                // Thông báo adapter để cập nhật UI
                conversationsAdapter.notifyDataSetChanged();
                break;
            }
        }
    }
}