package com.hgb7725.botchattyapp.firebase;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.hgb7725.botchattyapp.models.ChatMessage;
import com.hgb7725.botchattyapp.utilities.Constants;
import com.hgb7725.botchattyapp.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class ConversationFirebaseService {
    // Initialize at declaration to avoid "might not have been initialized" warnings
    private FirebaseFirestore database = FirebaseFirestore.getInstance();
    private PreferenceManager preferenceManager;
    private List<ChatMessage> conversations = new ArrayList<>();
    private ConversationListener conversationListener;
    private Context context;
    private String conversionId = null;

    public ConversationFirebaseService(Context context, PreferenceManager preferenceManager) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        if (preferenceManager == null) {
            throw new IllegalArgumentException("PreferenceManager cannot be null");
        }
        
        this.context = context;
        this.preferenceManager = preferenceManager;
        // Database is already initialized above
        // Conversations list is already initialized above
    }

    // Constructor for use in ChatFirebaseService
    public ConversationFirebaseService(PreferenceManager preferenceManager) {
        if (preferenceManager == null) {
            throw new IllegalArgumentException("PreferenceManager cannot be null");
        }
        
        this.preferenceManager = preferenceManager;
    }

    public interface ConversationListener {
        void onConversationsLoaded(List<ChatMessage> conversations);
        void onConversationsUpdated();
    }

    public interface ConversionIdListener {
        void onConversionIdReceived(String conversionId);
    }

    public void setConversationListener(ConversationListener listener) {
        this.conversationListener = listener;
    }

    public List<ChatMessage> getConversations() {
        return conversations;
    }

    public void listenConversations() {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    // Added from ChatFirebaseService
    public void markConversationAsRead(String receiverId) {
        // Find the conversation in Firestore and set unreadCount to 0
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        // Update unreadCount to 0 in Firestore
                        DocumentReference documentReference = task.getResult().getDocuments().get(0).getReference();
                        documentReference.update("unreadCount", 0);
                    }
                });
    }

    // Added from ChatFirebaseService
    public void addConversion(HashMap<String, Object> conversion, ConversionIdListener listener) {
        // Check conversation in both directions (sender -> receiver and receiver -> sender)
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, conversion.get(Constants.KEY_SENDER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, conversion.get(Constants.KEY_RECEIVER_ID))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        // If found in sender -> receiver direction
                        DocumentReference documentReference = task.getResult().getDocuments().get(0).getReference();
                        conversionId = documentReference.getId();
                        documentReference.update(conversion);
                        if (listener != null) {
                            listener.onConversionIdReceived(conversionId);
                        }
                    } else {
                        // If not found in first direction, check reverse
                        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                                .whereEqualTo(Constants.KEY_SENDER_ID, conversion.get(Constants.KEY_RECEIVER_ID))
                                .whereEqualTo(Constants.KEY_RECEIVER_ID, conversion.get(Constants.KEY_SENDER_ID))
                                .get()
                                .addOnCompleteListener(reverseTask -> {
                                    if (reverseTask.isSuccessful() && reverseTask.getResult() != null && !reverseTask.getResult().isEmpty()) {
                                        // If found in receiver -> sender direction
                                        DocumentReference reverseDocRef = reverseTask.getResult().getDocuments().get(0).getReference();
                                        conversionId = reverseDocRef.getId();
                                        reverseDocRef.update(conversion);
                                        if (listener != null) {
                                            listener.onConversionIdReceived(conversionId);
                                        }
                                    } else {
                                        // If not found in either direction, create new
                                        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                                                .add(conversion)
                                                .addOnSuccessListener(documentReference -> {
                                                    conversionId = documentReference.getId();
                                                    if (listener != null) {
                                                        listener.onConversionIdReceived(conversionId);
                                                    }
                                                });
                                    }
                                });
                    }
                });
    }

    // Added from ChatFirebaseService
    public void updateConversion(String receiverId, String receiverName, String receiverImage, String message) {
        if (conversionId == null) return;
        
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversionId);

        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_LAST_MESSAGE, message);
        updates.put(Constants.KEY_TIMESTAMP, new Date());
        updates.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        updates.put(Constants.KEY_RECEIVER_ID, receiverId);
        updates.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
        updates.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
        updates.put(Constants.KEY_RECEIVER_NAME, receiverName);
        updates.put(Constants.KEY_RECEIVER_IMAGE, receiverImage);
        updates.put("lastSenderId", preferenceManager.getString(Constants.KEY_USER_ID));
        updates.put("unreadCount", com.google.firebase.firestore.FieldValue.increment(1));
        
        documentReference.update(updates);
    }

    // Added from ChatFirebaseService
    public void checkForConversion(String senderId, String receiverId, ConversionIdListener listener) {
        checkForConversionRemotely(senderId, receiverId, listener);
        checkForConversionRemotely(receiverId, senderId, listener);
    }

    // Added from ChatFirebaseService
    private void checkForConversionRemotely(String senderId, String receiverId, ConversionIdListener listener) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        conversionId = documentSnapshot.getId();
                        if (listener != null) {
                            listener.onConversionIdReceived(conversionId);
                        }
                    } else {
                        // If not found in first direction, check reverse
                        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                                .whereEqualTo(Constants.KEY_SENDER_ID, receiverId)
                                .whereEqualTo(Constants.KEY_RECEIVER_ID, senderId)
                                .get()
                                .addOnCompleteListener(reverseTask -> {
                                    if (reverseTask.isSuccessful() && reverseTask.getResult() != null && reverseTask.getResult().getDocuments().size() > 0) {
                                        DocumentSnapshot reverseDocSnapshot = reverseTask.getResult().getDocuments().get(0);
                                        conversionId = reverseDocSnapshot.getId();
                                        if (listener != null) {
                                            listener.onConversionIdReceived(conversionId);
                                        }
                                    }
                                });
                    }
                });
    }

    // Added from ChatFirebaseService
    public void setConversionId(String id) {
        this.conversionId = id;
    }

    // Added from ChatFirebaseService
    public String getConversionId() {
        return conversionId;
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            boolean hasChanges = false;
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    
                    // Check if conversation already exists in the list
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
                        
                        // Get the last message sender ID
                        String lastSenderId = documentChange.getDocument().getString("lastSenderId");
                        chatMessage.setLastSenderId(lastSenderId);
                        
                        // Get the unreadCount value from Firestore if available
                        if (documentChange.getDocument().getLong("unreadCount") != null) {
                            chatMessage.setUnreadCount(documentChange.getDocument().getLong("unreadCount").intValue());
                        }
                        
                        conversations.add(chatMessage);
                        hasChanges = true;
                    }
                } else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                    // This is the critical section for real-time updates
                    String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    String lastMessage = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                    Date timestamp = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    String lastSenderId = documentChange.getDocument().getString("lastSenderId");
                    
                    boolean foundConversation = false;
                    for (int i = 0; i < conversations.size(); i++) {
                        if ((conversations.get(i).getSenderId().equals(senderId) && conversations.get(i).getReceiverId().equals(receiverId)) ||
                            (conversations.get(i).getSenderId().equals(receiverId) && conversations.get(i).getReceiverId().equals(senderId))) {
                            
                            foundConversation = true;
                            // Update the conversation data
                            conversations.get(i).setMessage(lastMessage);
                            conversations.get(i).setDateObject(timestamp);
                            conversations.get(i).setLastSenderId(lastSenderId);
                            
                            // Update sender and receiver image/name if needed
                            if (preferenceManager.getString(Constants.KEY_USER_ID).equals(senderId)) {
                                conversations.get(i).setConversionImage(documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE));
                                conversations.get(i).setConversionName(documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME));
                            } else {
                                conversations.get(i).setConversionImage(documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE));
                                conversations.get(i).setConversionName(documentChange.getDocument().getString(Constants.KEY_SENDER_NAME));
                            }
                            
                            // Check if current user is in chat with the sender
                            String currentChatUserId = preferenceManager.getString("current_chat_user_id");
                            boolean isInChatWithSender = (currentChatUserId != null && 
                                                         (currentChatUserId.equals(senderId) || 
                                                          currentChatUserId.equals(receiverId)));
                            
                            // Only update unread count if user is NOT currently chatting with this user
                            if (!isInChatWithSender) {
                                // Update unread message count from Firestore
                                if (documentChange.getDocument().getLong("unreadCount") != null) {
                                    conversations.get(i).setUnreadCount(documentChange.getDocument().getLong("unreadCount").intValue());
                                }
                            } else {
                                // If user is in chat with the sender, reset unread count
                                conversations.get(i).setUnreadCount(0);
                            }
                            
                            hasChanges = true;
                            break;
                        }
                    }
                    
                    // If conversation not found in our list but got a MODIFIED event,
                    // it might be a new conversation that we missed, so add it
                    if (!foundConversation) {
                        // Create a new conversation object
                        ChatMessage chatMessage = new ChatMessage();
                        chatMessage.setSenderId(senderId);
                        chatMessage.setReceiverId(receiverId);
                        chatMessage.setMessage(lastMessage);
                        chatMessage.setDateObject(timestamp);
                        chatMessage.setLastSenderId(lastSenderId);
                        
                        if (preferenceManager.getString(Constants.KEY_USER_ID).equals(senderId)) {
                            chatMessage.setConversionImage(documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE));
                            chatMessage.setConversionName(documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME));
                            chatMessage.setConversionId(documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID));
                        } else {
                            chatMessage.setConversionImage(documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE));
                            chatMessage.setConversionName(documentChange.getDocument().getString(Constants.KEY_SENDER_NAME));
                            chatMessage.setConversionId(documentChange.getDocument().getString(Constants.KEY_SENDER_ID));
                        }
                        
                        if (documentChange.getDocument().getLong("unreadCount") != null) {
                            chatMessage.setUnreadCount(documentChange.getDocument().getLong("unreadCount").intValue());
                        }
                        
                        conversations.add(chatMessage);
                        hasChanges = true;
                    }
                } else if (documentChange.getType() == DocumentChange.Type.REMOVED) {
                    // Handle conversation removal if needed
                    String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    
                    for (int i = 0; i < conversations.size(); i++) {
                        if ((conversations.get(i).getSenderId().equals(senderId) && conversations.get(i).getReceiverId().equals(receiverId)) ||
                            (conversations.get(i).getSenderId().equals(receiverId) && conversations.get(i).getReceiverId().equals(senderId))) {
                            conversations.remove(i);
                            hasChanges = true;
                            break;
                        }
                    }
                }
            }

            if (hasChanges) {
                // Sort conversations by timestamp
                Collections.sort(conversations, (obj1, obj2) ->
                        obj2.getDateObject().compareTo(obj1.getDateObject()));
                
                if (conversationListener != null) {
                    // First notify that conversations were updated (for UI refresh)
                    conversationListener.onConversationsUpdated();
                    // Then provide the full list (for complete refresh if needed)
                    conversationListener.onConversationsLoaded(conversations);
                }
            }
        }
    };
    
    // How to reset the number of unread messages when a user opens a conversation
    public void resetUnreadCountForSender(String senderId) {
        // Find the conversation in the list and reset the number of unread messages
        for (ChatMessage conversation : conversations) {
            if (conversation.getConversionId().equals(senderId)) {
                // Reset unreadCount trong model
                conversation.setUnreadCount(0);
                
                // Reset unreadCount trong Firestore
                updateUnreadCountInFirestore(senderId, preferenceManager.getString(Constants.KEY_USER_ID), 0);
                
                // Notice of change
                if (conversationListener != null) {
                    conversationListener.onConversationsUpdated();
                }
                break;
            }
        }
    }
    
    // update unread message count in Firestore
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
    
    private void showToast(String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Manually refresh conversations from Firebase
     * Used when returning to MainActivity from chat
     */
    public void refreshConversations() {
        // Clear existing conversations to avoid duplicates
        conversations.clear();
        
        // Manually trigger a one-time fetch from Firebase
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        processConversationDocuments(task.getResult().getDocuments());
                    }
                    
                    // Check for conversations where user is receiver
                    database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                            .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                            .get()
                            .addOnCompleteListener(receiverTask -> {
                                if (receiverTask.isSuccessful() && receiverTask.getResult() != null) {
                                    processConversationDocuments(receiverTask.getResult().getDocuments());
                                }
                                
                                // Sort and notify listeners after both queries complete
                                if (!conversations.isEmpty()) {
                                    Collections.sort(conversations, (obj1, obj2) ->
                                            obj2.getDateObject().compareTo(obj1.getDateObject()));
                                    
                                    if (conversationListener != null) {
                                        conversationListener.onConversationsLoaded(conversations);
                                    }
                                }
                            });
                });
    }

    private void processConversationDocuments(List<com.google.firebase.firestore.DocumentSnapshot> documents) {
        for (com.google.firebase.firestore.DocumentSnapshot document : documents) {
            String senderId = document.getString(Constants.KEY_SENDER_ID);
            String receiverId = document.getString(Constants.KEY_RECEIVER_ID);
            
            // Check if conversation already exists in the list
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
                    chatMessage.setConversionImage(document.getString(Constants.KEY_RECEIVER_IMAGE));
                    chatMessage.setConversionName(document.getString(Constants.KEY_RECEIVER_NAME));
                    chatMessage.setConversionId(document.getString(Constants.KEY_RECEIVER_ID));
                } else {
                    chatMessage.setConversionImage(document.getString(Constants.KEY_SENDER_IMAGE));
                    chatMessage.setConversionName(document.getString(Constants.KEY_SENDER_NAME));
                    chatMessage.setConversionId(document.getString(Constants.KEY_SENDER_ID));
                }

                chatMessage.setMessage(document.getString(Constants.KEY_LAST_MESSAGE));
                chatMessage.setDateObject(document.getDate(Constants.KEY_TIMESTAMP));
                
                // Get the last message sender ID
                String lastSenderId = document.getString("lastSenderId");
                chatMessage.setLastSenderId(lastSenderId);
                
                // Get the unreadCount value from Firestore if available
                if (document.getLong("unreadCount") != null) {
                    chatMessage.setUnreadCount(document.getLong("unreadCount").intValue());
                }
                
                conversations.add(chatMessage);
            }
        }
    }
}