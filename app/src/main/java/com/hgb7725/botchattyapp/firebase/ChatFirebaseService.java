package com.hgb7725.botchattyapp.firebase;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ChatFirebaseService {
    private final FirebaseFirestore database;
    private final PreferenceManager preferenceManager;
    private final List<ChatMessage> chatMessages;
    private EventListener<QuerySnapshot> eventListener;
    private OnMessageReceiveListener onMessageReceiveListener;
    private final ConversationFirebaseService conversationService;

    public ChatFirebaseService(PreferenceManager preferenceManager) {
        this.preferenceManager = preferenceManager;
        this.database = FirebaseFirestore.getInstance();
        this.chatMessages = new ArrayList<>();
        this.conversationService = new ConversationFirebaseService(preferenceManager);
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

        if(getConversionId() != null) {
            conversationService.updateConversion(receiverId, receiverName, receiverImage, message);
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
            conversationService.addConversion(conversion, conversionId -> {
                setConversionId(conversionId);
                if (onMessageReceiveListener != null) {
                    onMessageReceiveListener.onConversionIdReceived(conversionId);
                }
            });
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

        if (getConversionId() != null) {
            conversationService.updateConversion(receiverId, receiverName, receiverImage, "File: " + fileName);
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
            conversationService.addConversion(conversion, conversionId -> {
                setConversionId(conversionId);
                if (onMessageReceiveListener != null) {
                    onMessageReceiveListener.onConversionIdReceived(conversionId);
                }
            });
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
                conversationService.addConversion(conversion, conversionId -> {
                    setConversionId(conversionId);
                    if (onMessageReceiveListener != null) {
                        onMessageReceiveListener.onConversionIdReceived(conversionId);
                    }
                });
            });
    }

    public void sendVideoMessage(String receiverId, String receiverName, String receiverImage, String videoUrl, String fileName, String duration) {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiverId);
        message.put(Constants.KEY_MESSAGE, videoUrl);
        message.put("fileName", fileName + "|" + duration); // Store both filename and duration
        message.put("type", "video");
        message.put(Constants.KEY_TIMESTAMP, new Date());

        database.collection(Constants.KEY_COLLECTION_CHAT).add(message)
            .addOnSuccessListener(documentReference -> {
                HashMap<String, Object> conversion = new HashMap<>();
                conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
                conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
                conversion.put(Constants.KEY_RECEIVER_ID, receiverId);
                conversion.put(Constants.KEY_RECEIVER_NAME, receiverName);
                conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverImage);
                conversion.put(Constants.KEY_LAST_MESSAGE, "Video");
                conversion.put(Constants.KEY_TIMESTAMP, new Date());
                conversion.put("lastSenderId", preferenceManager.getString(Constants.KEY_USER_ID));
                conversion.put("unreadCount", com.google.firebase.firestore.FieldValue.increment(1));
                
                // Include both cases: conversation exists or not
                conversationService.addConversion(conversion, conversionId -> {
                    setConversionId(conversionId);
                    if (onMessageReceiveListener != null) {
                        onMessageReceiveListener.onConversionIdReceived(conversionId);
                    }
                });
            });
    }

    public void markConversationAsRead(String receiverId) {
        // Delegate to ConversationFirebaseService
        conversationService.markConversationAsRead(receiverId);
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
                            // Thêm mã này để chuyển đổi timestamp thành định dạng thời gian
                            chatMessage.setDateTime(getReadableDateTime(timestamp));
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
            if (getConversionId() == null && chatMessages.size() > 0) {
                conversationService.checkForConversion(senderId, receiverId, conversionId -> {
                    setConversionId(conversionId);
                    if (onMessageReceiveListener != null) {
                        onMessageReceiveListener.onConversionIdReceived(conversionId);
                    }
                });
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
               // Show online status only when both conditions are true:
               // 1. User is online (availability == 1)
               // 2. User has enabled online status display (online_status_visible == true)
               Boolean isOnlineStatusVisible = value.getBoolean(Constants.KEY_ONLINE_STATUS_VISIBLE);
               if (isOnlineStatusVisible == null) {
                   isOnlineStatusVisible = true;
               }
               
               if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                   int availability = value.getLong(Constants.KEY_AVAILABILITY).intValue();
                   // Chỉ báo online khi cả hai điều kiện đều đúng
                   boolean isAvailable = (availability == 1) && isOnlineStatusVisible;
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

    public void setConversionId(String id) {
        conversationService.setConversionId(id);
    }

    public String getConversionId() {
        return conversationService.getConversionId();
    }

    public List<ChatMessage> getChatMessages() {
        return chatMessages;
    }

    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }
}