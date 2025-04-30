package com.hgb7725.botchattyapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.hgb7725.botchattyapp.R;
import com.hgb7725.botchattyapp.adapters.ChatAdapter;
import com.hgb7725.botchattyapp.cloudinary.CloudinaryService;
import com.hgb7725.botchattyapp.databinding.ActivityChatBinding;
import com.hgb7725.botchattyapp.firebase.ChatFirebaseService;
import com.hgb7725.botchattyapp.models.ChatMessage;
import com.hgb7725.botchattyapp.models.User;
import com.hgb7725.botchattyapp.utilities.Constants;
import com.hgb7725.botchattyapp.utilities.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ChatActivity extends BaseActivity {

    private ActivityChatBinding binding;
    private User receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private ChatFirebaseService chatFirebaseService;
    private CloudinaryService cloudinaryService;
    private Boolean isReceiverAvailable = false;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_FILE_REQUEST = 2;
    private static final int RECORD_AUDIO_REQUEST = 3;
    private static final int PICK_VIDEO_REQUEST = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        loadReceiverDetails();
        init();
        setupListeners();

        // Mark current conversation as read
        chatFirebaseService.markConversationAsRead(receiverUser.getId());
    }

    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();

        // Initialize services
        chatFirebaseService = new ChatFirebaseService(preferenceManager);
        cloudinaryService = new CloudinaryService(this);

        chatAdapter = new ChatAdapter(
                chatMessages,
                getBitmapFromEncodedString(receiverUser.getImage()),
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
    }

    private void setupListeners() {
        // Set up the chat message listener
        chatFirebaseService.setOnMessageReceiveListener(new ChatFirebaseService.OnMessageReceiveListener() {
            @Override
            public void onMessagesReceived(List<ChatMessage> messages, int count) {
                chatMessages.clear();
                chatMessages.addAll(messages);

                if (count == 0) {
                    chatAdapter.notifyDataSetChanged();
                } else {
                    chatAdapter.notifyItemRangeInserted(count, messages.size() - count);
                    binding.chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
                }
                binding.chatRecyclerView.setVisibility(View.VISIBLE);
                binding.progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onConversionIdReceived(String conversionId) {
                // Conversion ID received, no action needed here
            }
        });

        // Listen for messages
        chatFirebaseService.listenForMessages(
                preferenceManager.getString(Constants.KEY_USER_ID),
                receiverUser.getId()
        );

        // Listen for receiver availability
        chatFirebaseService.listenAvailabilityOfReceiver(receiverUser, new ChatFirebaseService.AvailabilityListener() {
            @Override
            public void onAvailabilityChanged(boolean isAvailable) {
                isReceiverAvailable = isAvailable;
                if (isReceiverAvailable) {
                    binding.textAvailability.setVisibility(View.VISIBLE);
                } else {
                    binding.textAvailability.setVisibility(View.GONE);
                }
            }

            @Override
            public void onTokenUpdated(String token) {
                receiverUser.setToken(token);
            }
        });
    }

    private void sendMessage() {
        String message = binding.inputMessage.getText().toString().trim();
        if (!message.isEmpty()) {
            chatFirebaseService.sendTextMessage(
                    receiverUser.getId(),
                    receiverUser.getName(),
                    receiverUser.getImage(),
                    message
            );
            binding.inputMessage.setText(null);
        }
    }

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

        // Add call button click listeners
        binding.imageCall.setOnClickListener(v -> {
            // Start voice call
            Intent intent = new Intent(getApplicationContext(), VoiceCallActivity.class);
            intent.putExtra(Constants.KEY_USER, receiverUser);
            startActivity(intent);
        });

        binding.imageVideoCall.setOnClickListener(v -> {
            // Start video call
            Intent intent = new Intent(getApplicationContext(), VideoCallActivity.class);
            intent.putExtra(Constants.KEY_USER, receiverUser);
            startActivity(intent);
        });

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

        binding.layoutAttachmentOptions.layoutVideo.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_VIDEO_REQUEST);
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

    // Processes the selected media (file, image, and video) based on the request code.
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
            } else if (requestCode == PICK_VIDEO_REQUEST && data != null) {
                Uri videoUri = data.getData();
                // Handle video upload
                handleVideoUpload(videoUri);
            }
        }
    }

    private void handleImageUpload(Uri imageUri) {
        cloudinaryService.uploadImage(imageUri, new CloudinaryService.UploadCallback() {
            @Override
            public void onSuccess(String url, String fileName) {
                chatFirebaseService.sendImageMessage(
                        receiverUser.getId(),
                        receiverUser.getName(),
                        receiverUser.getImage(),
                        url
                );
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ChatActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleFileUpload(Uri fileUri) {
        cloudinaryService.uploadFile(fileUri, new CloudinaryService.UploadCallback() {
            @Override
            public void onSuccess(String url, String fileName) {
                chatFirebaseService.sendFileMessage(
                        receiverUser.getId(),
                        receiverUser.getName(),
                        receiverUser.getImage(),
                        url,
                        fileName
                );
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ChatActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleVideoUpload(Uri videoUri) {
        // Add progress indicator or loading message
        binding.progressBar.setVisibility(View.VISIBLE);
        
        cloudinaryService.uploadVideo(videoUri, new CloudinaryService.UploadCallback() {
            @Override
            public void onSuccess(String url, String fileNameWithDuration) {
                // Format is "filename.mp4|duration"
                String[] parts = fileNameWithDuration.split("\\|");
                String fileName = parts[0];
                String duration = parts.length > 1 ? parts[1] : "00:00";
                
                chatFirebaseService.sendVideoMessage(
                        receiverUser.getId(),
                        receiverUser.getName(),
                        receiverUser.getImage(),
                        url,
                        fileName,
                        duration
                );
                
                binding.progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(ChatActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getReadableDateTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getDefault()); // Keep system timezone
        return sdf.format(date);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Mark user as being in conversation with receiver
        // Save receiver id to preferences for MainActivity to know
        preferenceManager.putString("current_chat_user_id", receiverUser.getId());

        // Mark conversation as read whenever resumed
        chatFirebaseService.markConversationAsRead(receiverUser.getId());
    }

    @Override
    protected void onPause() {
        super.onPause();
        // When leaving chat screen, remove receiver id from preferences
        preferenceManager.putString("current_chat_user_id", null);
    }
}