package com.hgb7725.botchattyapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.hgb7725.botchattyapp.R;
import com.hgb7725.botchattyapp.adapters.ChatAdapter;
import com.hgb7725.botchattyapp.databinding.ActivityChatBinding;
import com.hgb7725.botchattyapp.models.ChatMessage;
import com.hgb7725.botchattyapp.models.User;
import com.hgb7725.botchattyapp.utilities.CloudinaryConfig;
import com.hgb7725.botchattyapp.utilities.Constants;
import com.hgb7725.botchattyapp.utilities.FileUtils;
import com.hgb7725.botchattyapp.utilities.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class ChatActivity extends BaseActivity {

    private ActivityChatBinding binding;
    private User receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private String conversionId = null;
    private Boolean isReceiverAvailable = false;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_FILE_REQUEST = 2;
    private static final int RECORD_AUDIO_REQUEST = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        loadReceiverDetails();
        init();
        listenMessages();
        
        // Đánh dấu cuộc trò chuyện hiện tại là đã đọc
        markConversationAsRead();
    }

    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages,
                getBitmapFromEncodedString(receiverUser.getImage()),
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void sendMessage() {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.getId());
        message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
        message.put(Constants.KEY_TIMESTAMP, new Date());
        message.put("type", "text");
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);

        if(conversionId != null) {
            updateConversion(binding.inputMessage.getText().toString());
        } else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.getId());
            conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.getName());
            conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.getImage());
            conversion.put(Constants.KEY_LAST_MESSAGE, binding.inputMessage.getText().toString());
            conversion.put(Constants.KEY_TIMESTAMP, new Date());
            conversion.put("lastSenderId", preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put("unreadCount", 1);
            addConversion(conversion);
        }

        binding.inputMessage.setText(null);
    }

    private void listenAvailabilityOfReceiver() {
        database.collection(Constants.KEY_COLLECTION_USERS).document(
                receiverUser.getId()
        ).addSnapshotListener(ChatActivity.this, (value, error) -> {
           if (error != null) {
               return;
           }
           if (value != null) {
               if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                   int availability = Objects.requireNonNull(
                           value.getLong(Constants.KEY_AVAILABILITY)
                   ).intValue();
                   isReceiverAvailable = availability == 1;
               }
               receiverUser.setToken(value.getString(Constants.KEY_FCM_TOKEN));
           }
           if (isReceiverAvailable) {
               binding.textAvailability.setVisibility(View.VISIBLE);
           } else {
               binding.textAvailability.setVisibility(View.GONE);
           }
        });
    }

    private void listenMessages() {
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.getId())
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.getId())
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            int count = chatMessages.size();
            for(DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.setSenderId(documentChange.getDocument().getString(Constants.KEY_SENDER_ID));
                    chatMessage.setReceiverId(documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID));
                    chatMessage.setMessage(documentChange.getDocument().getString(Constants.KEY_MESSAGE));

                    String type = documentChange.getDocument().getString("type");
                    chatMessage.setType(type != null ? type : "text");

                    chatMessage.setFileName(documentChange.getDocument().getString("fileName"));

                    chatMessage.setDateTime(getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP)));
                    chatMessage.setDateObject(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages,
                    Comparator.comparing(ChatMessage::getDateObject));
//                    (obj1, obj2) -> obj1.getDateObject().compareTo(obj2.getDateObject()));
            if (count == 0) {
                chatAdapter.notifyDataSetChanged();
            } else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
        if (conversionId == null) {
            checkForConversion();
        }
    };

    private Bitmap getBitmapFromEncodedString(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private void loadReceiverDetails() {
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(receiverUser.getName());
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.layoutSend.setOnClickListener(v -> sendMessage());

        // Add attachment button click listener
        binding.imageAttachment.setOnClickListener(v -> toggleAttachmentOptions());

        // Add attachment options click listeners
        binding.layoutAttachmentOptions.layoutImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
            toggleAttachmentOptions();
        });

        binding.layoutAttachmentOptions.layoutDocument.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, PICK_FILE_REQUEST);
            toggleAttachmentOptions();
        });

        binding.layoutAttachmentOptions.layoutAudio.setOnClickListener(v -> {
            // Implement audio recording logic here
            Toast.makeText(this, "Audio recording coming soon!", Toast.LENGTH_SHORT).show();
            toggleAttachmentOptions();
        });
    }

    private void toggleAttachmentOptions() {
        if (binding.layoutAttachmentOptions.getRoot().getVisibility() == View.VISIBLE) {
            binding.layoutAttachmentOptions.getRoot().setVisibility(View.GONE);
        } else {
            binding.layoutAttachmentOptions.getRoot().setVisibility(View.VISIBLE);
        }
    }

    // Processes the selected media (file and image) based on the request code.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null) {
                Uri imageUri = data.getData();
                // Handle image upload
                handleImageUpload(imageUri);
            } else if (requestCode == PICK_FILE_REQUEST && data != null) {
                Uri fileUri = data.getData();
                // Handle file upload
                handleFileUpload(fileUri);
            }
        }
    }

    private void handleImageUpload(Uri imageUri) {
        // Implement image upload logic here
//        Toast.makeText(this, "Image upload coming soon!", Toast.LENGTH_SHORT).show();
        uploadImageToCloudinary(imageUri);
    }

    private void handleFileUpload(Uri fileUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            byte[] fileBytes = new byte[inputStream.available()];
            inputStream.read(fileBytes);
            String base64File = Base64.encodeToString(fileBytes, Base64.NO_WRAP);

            String originalFileName = FileUtils.getFileNameFromUri(this, fileUri);
            String fileName = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");

            RequestQueue requestQueue = Volley.newRequestQueue(this);
            String uploadUrl = CloudinaryConfig.getUploadUrl("raw");

            JSONObject params = new JSONObject();
            String mimeType = getContentResolver().getType(fileUri);
            if (mimeType == null) {
                mimeType = "application/octet-stream"; // fallback for unknown
            }
            params.put("file", "data:" + mimeType + ";base64," + base64File);
            params.put("upload_preset", CloudinaryConfig.PRESET_FILE);
            params.put("public_id", fileName);

            Toast.makeText(this, "Uploading: " + CloudinaryConfig.PRESET_FILE, Toast.LENGTH_SHORT).show();

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, uploadUrl, params,
                    response -> {
                        try {
                            String fileUrl = response.getString("secure_url");
                            sendFileMessage(fileUrl, fileName);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    },
                    error -> {
                        String errorMsg = "Unknown error";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            errorMsg = new String(error.networkResponse.data);
                        } else if (error.getMessage() != null) {
                            errorMsg = error.getMessage();
                        }
                        Log.e("FILE_UPLOAD", "Upload failed: " + errorMsg);
                        Toast.makeText(this, "Upload failed:\n" + errorMsg, Toast.LENGTH_LONG).show();
                    }
            );
            requestQueue.add(request);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Upload error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getReadableDateTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getDefault()); // Giữ nguyên múi giờ của hệ thống
        return sdf.format(date);
    }

    private void addConversion(HashMap<String, Object> conversion) {
        // Kiểm tra conversation theo cả 2 chiều (sender -> receiver và receiver -> sender)
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, conversion.get(Constants.KEY_SENDER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, conversion.get(Constants.KEY_RECEIVER_ID))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        // Nếu tìm thấy conversation theo chiều sender -> receiver
                        DocumentReference documentReference = task.getResult().getDocuments().get(0).getReference();
                        conversionId = documentReference.getId();
                        documentReference.update(conversion);
                    } else {
                        // Nếu không tìm thấy theo chiều đầu tiên, kiểm tra chiều ngược lại
                        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                                .whereEqualTo(Constants.KEY_SENDER_ID, conversion.get(Constants.KEY_RECEIVER_ID))
                                .whereEqualTo(Constants.KEY_RECEIVER_ID, conversion.get(Constants.KEY_SENDER_ID))
                                .get()
                                .addOnCompleteListener(reverseTask -> {
                                    if (reverseTask.isSuccessful() && reverseTask.getResult() != null && !reverseTask.getResult().isEmpty()) {
                                        // Nếu tìm thấy conversation theo chiều receiver -> sender
                                        DocumentReference reverseDocRef = reverseTask.getResult().getDocuments().get(0).getReference();
                                        conversionId = reverseDocRef.getId();
                                        reverseDocRef.update(conversion);
                                    } else {
                                        // Nếu không tìm thấy conversation theo cả 2 chiều, tạo mới
                                        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                                                .add(conversion)
                                                .addOnSuccessListener(documentReference -> conversionId = documentReference.getId());
                                    }
                                });
                    }
                });
    }

    private void updateConversion(String message) {
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversionId);

        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_LAST_MESSAGE, message);
        updates.put(Constants.KEY_TIMESTAMP, new Date());
        updates.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        updates.put(Constants.KEY_RECEIVER_ID, receiverUser.getId());
        updates.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
        updates.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
        updates.put(Constants.KEY_RECEIVER_NAME, receiverUser.getName());
        updates.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.getImage());
        updates.put("lastSenderId", preferenceManager.getString(Constants.KEY_USER_ID));
        updates.put("unreadCount", com.google.firebase.firestore.FieldValue.increment(1));
        
        documentReference.update(updates);
    }

    private void checkForConversion() {
        if (chatMessages.size() != 0) {
            checkForConversionRemotely(
                    preferenceManager.getString(Constants.KEY_USER_ID),
                    receiverUser.getId()
            );
            checkForConversionRemotely(
                    receiverUser.getId(),
                    preferenceManager.getString(Constants.KEY_USER_ID)
            );
        }
    }

    private void checkForConversionRemotely(String senderId, String receiverId) {
        // Kiểm tra cả 2 chiều của conversation
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        conversionId = documentSnapshot.getId();
                    } else {
                        // Nếu không tìm thấy theo chiều thứ nhất, kiểm tra chiều ngược lại
                        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                                .whereEqualTo(Constants.KEY_SENDER_ID, receiverId)
                                .whereEqualTo(Constants.KEY_RECEIVER_ID, senderId)
                                .get()
                                .addOnCompleteListener(reverseTask -> {
                                    if (reverseTask.isSuccessful() && reverseTask.getResult() != null && reverseTask.getResult().getDocuments().size() > 0) {
                                        DocumentSnapshot reverseDocSnapshot = reverseTask.getResult().getDocuments().get(0);
                                        conversionId = reverseDocSnapshot.getId();
                                    }
                                });
                    }
                });
    }

    private final OnCompleteListener<QuerySnapshot> conversionOnCompleteListener = task -> {
        if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversionId = documentSnapshot.getId();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
        
        // Đánh dấu người dùng đang ở trong cuộc trò chuyện với người nhận
        // Lưu id người nhận vào preferences để MainActivity biết
        preferenceManager.putString("current_chat_user_id", receiverUser.getId());
        
        // Đánh dấu cuộc trò chuyện là đã đọc mỗi khi resume
        markConversationAsRead();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Khi rời khỏi màn hình chat, xóa id người nhận khỏi preferences
        preferenceManager.putString("current_chat_user_id", null);
    }
    
    // Phương thức đánh dấu cuộc trò chuyện là đã đọc
    private void markConversationAsRead() {
        // Tìm cuộc trò chuyện trong Firestore và đặt unreadCount về 0
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.getId())
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        // Cập nhật unreadCount về 0 trong Firestore
                        DocumentReference documentReference = task.getResult().getDocuments().get(0).getReference();
                        documentReference.update("unreadCount", 0);
                    }
                });
    }

    private void sendImageMessage(String imageUrl) {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.getId());
        message.put(Constants.KEY_MESSAGE, imageUrl);
        message.put(Constants.KEY_TIMESTAMP, new Date());
        message.put("type", "image");

        database.collection(Constants.KEY_COLLECTION_CHAT).add(message)
            .addOnSuccessListener(documentReference -> {
                HashMap<String, Object> conversion = new HashMap<>();
                conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
                conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
                conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.getId());
                conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.getName());
                conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.getImage());
                conversion.put(Constants.KEY_LAST_MESSAGE, "Image");
                conversion.put(Constants.KEY_TIMESTAMP, new Date());
                conversion.put("lastSenderId", preferenceManager.getString(Constants.KEY_USER_ID));
                conversion.put("unreadCount", com.google.firebase.firestore.FieldValue.increment(1));
                // Bao gồm cả trường hợp conversation đã tồn tại hoặc chưa
                addConversion(conversion);
            });
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            byte[] imageBytes = new byte[inputStream.available()];
            inputStream.read(imageBytes);
            String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

            RequestQueue requestQueue = Volley.newRequestQueue(this);
            String uploadUrl = CloudinaryConfig.getUploadUrl("image");

            JSONObject params = new JSONObject();
            params.put("file", "data:image/jpeg;base64," + base64Image);
            params.put("upload_preset", CloudinaryConfig.PRESET_IMAGE);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, uploadUrl, params,
                    response -> {
                        try {
                            String imageUrl = response.getString("secure_url");
                            sendImageMessage(imageUrl);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    },
                    error -> {
                        String errorMsg = "Unknown error";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            errorMsg = new String(error.networkResponse.data);
                        } else if (error.getMessage() != null) {
                            errorMsg = error.getMessage();
                        }
                        Log.e("CLOUDINARY_UPLOAD", "Upload failed: " + errorMsg, error);
                        Toast.makeText(this, "Upload failed:\n" + errorMsg, Toast.LENGTH_LONG).show();
                    }
            );

            requestQueue.add(request);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Upload error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadFileToCloudinary(Uri fileUri, String fileName) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            byte[] fileBytes = new byte[inputStream.available()];
            inputStream.read(fileBytes);
            String base64File = Base64.encodeToString(fileBytes, Base64.NO_WRAP);

            RequestQueue requestQueue = Volley.newRequestQueue(this);
            String uploadUrl = CloudinaryConfig.getUploadUrl("raw");

            JSONObject params = new JSONObject();
            params.put("file", "data:application/octet-stream;base64," + base64File);
            params.put("upload_preset", CloudinaryConfig.PRESET_FILE);
            params.put("public_id", fileName);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, uploadUrl, params,
                    response -> {
                        try {
                            String fileUrl = response.getString("secure_url");
                            sendFileMessage(fileUrl, fileName);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    },
                    error -> {
                        String errorMsg = "Unknown error";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            errorMsg = new String(error.networkResponse.data);
                        } else if (error.getMessage() != null) {
                            errorMsg = error.getMessage();
                        }
                        Log.e("UPLOAD_FILE", "Upload failed: " + errorMsg, error);
                        Toast.makeText(this, "Upload failed:\n" + errorMsg, Toast.LENGTH_LONG).show();
                    }
            );

            requestQueue.add(request);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Upload error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sendFileMessage(String fileUrl, String fileName) {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.getId());
        message.put(Constants.KEY_MESSAGE, fileUrl);
        message.put("fileName", fileName);
        message.put("type", "file");
        message.put(Constants.KEY_TIMESTAMP, new Date());

        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);

        if (conversionId != null) {
            updateConversion("File: " + fileName);
        } else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.getId());
            conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.getName());
            conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.getImage());
            conversion.put(Constants.KEY_LAST_MESSAGE, "File: " + fileName);
            conversion.put(Constants.KEY_TIMESTAMP, new Date());
            conversion.put("lastSenderId", preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put("unreadCount", 1);
            addConversion(conversion);
        }
    }

}