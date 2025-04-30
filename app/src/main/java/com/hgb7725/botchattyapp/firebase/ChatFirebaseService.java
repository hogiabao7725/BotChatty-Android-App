package com.hgb7725.botchattyapp.firebase;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.hgb7725.botchattyapp.models.ChatMessage;
import com.hgb7725.botchattyapp.models.User;
import com.hgb7725.botchattyapp.utilities.Constants;
import com.hgb7725.botchattyapp.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class ChatFirebaseService {
    private final FirebaseFirestore database;
    private final PreferenceManager preferenceManager;
    private String conversionId = null;
    private final List<ChatMessage> chatMessages;
    private EventListener<QuerySnapshot> eventListener;
    private OnMessageReceiveListener onMessageReceiveListener;

    public ChatFirebaseService(PreferenceManager preferenceManager) {
        this.preferenceManager = preferenceManager;
        this.database = FirebaseFirestore.getInstance();
        this.chatMessages = new ArrayList<>();
    }

    public interface OnMessageReceiveListener {
        void onMessagesReceived(List<ChatMessage> messages, int count);
        void onConversionIdReceived(String conversionId);
    }

    public void setOnMessageReceiveListener(OnMessageReceiveListener listener) {
        this.onMessageReceiveListener = listener;
    }

    public void sendTextMessage(String receiverId, String receiverName, String receiverImage, String message) {
        HashMap<String, Object> messageData = new HashMap<>();
        messageData.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        messageData.put(Constants.KEY_RECEIVER_ID, receiverId);
        messageData.put(Constants.KEY_MESSAGE, message);
        messageData.put(Constants.KEY_TIMESTAMP, new Date());
        messageData.put("type", "text");
        
        database.collection(Constants.KEY_COLLECTION_CHAT).add(messageData);

        if(conversionId != null) {
            updateConversion(receiverId, receiverName, receiverImage, message);
        } else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID, receiverId);
            conversion.put(Constants.KEY_RECEIVER_NAME, receiverName);
            conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverImage);
            conversion.put(Constants.KEY_LAST_MESSAGE, message);
            conversion.put(Constants.KEY_TIMESTAMP, new Date());
            conversion.put("lastSenderId", preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put("unreadCount", 1);
            addConversion(conversion);
        }
    }

    public void sendFileMessage(String receiverId, String receiverName, String receiverImage, String fileUrl, String fileName) {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiverId);
        message.put(Constants.KEY_MESSAGE, fileUrl);
        message.put("fileName", fileName);
        message.put("type", "file");
        message.put(Constants.KEY_TIMESTAMP, new Date());

        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);

        if (conversionId != null) {
            updateConversion(receiverId, receiverName, receiverImage, "File: " + fileName);
        } else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID, receiverId);
            conversion.put(Constants.KEY_RECEIVER_NAME, receiverName);
            conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverImage);
            conversion.put(Constants.KEY_LAST_MESSAGE, "File: " + fileName);
            conversion.put(Constants.KEY_TIMESTAMP, new Date());
            conversion.put("lastSenderId", preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put("unreadCount", 1);
            addConversion(conversion);
        }
    }

    public void sendImageMessage(String receiverId, String receiverName, String receiverImage, String imageUrl) {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiverId);
        message.put(Constants.KEY_MESSAGE, imageUrl);
        message.put(Constants.KEY_TIMESTAMP, new Date());
        message.put("type", "image");

        database.collection(Constants.KEY_COLLECTION_CHAT).add(message)
            .addOnSuccessListener(documentReference -> {
                HashMap<String, Object> conversion = new HashMap<>();
                conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
                conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
                conversion.put(Constants.KEY_RECEIVER_ID, receiverId);
                conversion.put(Constants.KEY_RECEIVER_NAME, receiverName);
                conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverImage);
                conversion.put(Constants.KEY_LAST_MESSAGE, "Image");
                conversion.put(Constants.KEY_TIMESTAMP, new Date());
                conversion.put("lastSenderId", preferenceManager.getString(Constants.KEY_USER_ID));
                conversion.put("unreadCount", com.google.firebase.firestore.FieldValue.increment(1));
                // Include both cases: conversation exists or not
                addConversion(conversion);
            });
    }

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

    public void listenForMessages(String senderId, String receiverId) {
        // Clear previous messages to avoid duplicates
        chatMessages.clear();
        
        eventListener = (value, error) -> {
            if (error != null) {
                return;
            }
            if (value != null) {
                int count = chatMessages.size();
                for(DocumentChange documentChange : value.getDocumentChanges()) {
                    if (documentChange.getType() == DocumentChange.Type.ADDED || 
                        documentChange.getType() == DocumentChange.Type.MODIFIED) {
                        ChatMessage chatMessage = new ChatMessage();
                        chatMessage.setSenderId(documentChange.getDocument().getString(Constants.KEY_SENDER_ID));
                        chatMessage.setReceiverId(documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID));
                        chatMessage.setMessage(documentChange.getDocument().getString(Constants.KEY_MESSAGE));

                        String type = documentChange.getDocument().getString("type");
                        chatMessage.setType(type != null ? type : "text");

                        chatMessage.setFileName(documentChange.getDocument().getString("fileName"));

                        Date timestamp = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                        if (timestamp != null) {
                            chatMessage.setDateObject(timestamp);
                        }
                        
                        // Check if we already have this message (by comparing all fields)
                        boolean isDuplicate = false;
                        for (ChatMessage existingMessage : chatMessages) {
                            if (existingMessage.getSenderId().equals(chatMessage.getSenderId()) &&
                                existingMessage.getReceiverId().equals(chatMessage.getReceiverId()) &&
                                existingMessage.getMessage().equals(chatMessage.getMessage()) &&
                                existingMessage.getDateObject().equals(chatMessage.getDateObject())) {
                                isDuplicate = true;
                                break;
                            }
                        }
                        
                        if (!isDuplicate) {
                            chatMessages.add(chatMessage);
                        }
                    }
                }
                Collections.sort(chatMessages,
                        Comparator.comparing(ChatMessage::getDateObject));
                
                if (onMessageReceiveListener != null) {
                    onMessageReceiveListener.onMessagesReceived(chatMessages, count);
                }
            }
            if (conversionId == null) {
                checkForConversion(senderId, receiverId);
            }
        };

        // Use a single query with an OR condition to listen for messages in both directions
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, senderId)
                .addSnapshotListener(eventListener);
    }

    public void listenAvailabilityOfReceiver(User receiverUser, AvailabilityListener listener) {
        database.collection(Constants.KEY_COLLECTION_USERS).document(
                receiverUser.getId()
        ).addSnapshotListener((value, error) -> {
           if (error != null) {
               return;
           }
           if (value != null) {
               if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                   int availability = value.getLong(Constants.KEY_AVAILABILITY).intValue();
                   boolean isAvailable = availability == 1;
                   listener.onAvailabilityChanged(isAvailable);
               }
               
               String token = value.getString(Constants.KEY_FCM_TOKEN);
               if (token != null) {
                   listener.onTokenUpdated(token);
               }
           }
        });
    }

    public interface AvailabilityListener {
        void onAvailabilityChanged(boolean isAvailable);
        void onTokenUpdated(String token);
    }

    private void addConversion(HashMap<String, Object> conversion) {
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
                        if (onMessageReceiveListener != null) {
                            onMessageReceiveListener.onConversionIdReceived(conversionId);
                        }
                    } else {
                        // If not found in first direction, check reverse
                        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                                .whereEqualTo(Constants.KEY_SENDER_ID, conversion.get(Constants.KEY_RECEIVER_ID))
                                .whereEqualTo(Constants.KEY_RECEIVER_ID, conversion.get(Constants.KEY_SENDER_ID))
                                .get()
                                .addOnCompleteListener(reverseTask -> {
                                    if (reverseTask.isSuccessful() && reverseTask.getResult() != null && reverseTask.getResult().isEmpty()) {
                                        // If found in receiver -> sender direction
                                        DocumentReference reverseDocRef = reverseTask.getResult().getDocuments().get(0).getReference();
                                        conversionId = reverseDocRef.getId();
                                        reverseDocRef.update(conversion);
                                        if (onMessageReceiveListener != null) {
                                            onMessageReceiveListener.onConversionIdReceived(conversionId);
                                        }
                                    } else {
                                        // If not found in either direction, create new
                                        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                                                .add(conversion)
                                                .addOnSuccessListener(documentReference -> {
                                                    conversionId = documentReference.getId();
                                                    if (onMessageReceiveListener != null) {
                                                        onMessageReceiveListener.onConversionIdReceived(conversionId);
                                                    }
                                                });
                                    }
                                });
                    }
                });
    }

    private void updateConversion(String receiverId, String receiverName, String receiverImage, String message) {
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

    private void checkForConversion(String senderId, String receiverId) {
        if (chatMessages.size() != 0) {
            checkForConversionRemotely(senderId, receiverId);
            checkForConversionRemotely(receiverId, senderId);
        }
    }

    private void checkForConversionRemotely(String senderId, String receiverId) {
        // Check both directions of the conversation
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        conversionId = documentSnapshot.getId();
                        if (onMessageReceiveListener != null) {
                            onMessageReceiveListener.onConversionIdReceived(conversionId);
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
                                        if (onMessageReceiveListener != null) {
                                            onMessageReceiveListener.onConversionIdReceived(conversionId);
                                        }
                                    }
                                });
                    }
                });
    }

    public void setConversionId(String id) {
        this.conversionId = id;
    }

    public String getConversionId() {
        return conversionId;
    }

    public List<ChatMessage> getChatMessages() {
        return chatMessages;
    }
}