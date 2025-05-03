package com.hgb7725.botchattyapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.hgb7725.botchattyapp.R;
import com.hgb7725.botchattyapp.adapters.FileItemAdapter;
import com.hgb7725.botchattyapp.adapters.MediaPreviewAdapter;
import com.hgb7725.botchattyapp.databinding.ActivityUserProfileBinding;
import com.hgb7725.botchattyapp.firebase.MediaFirebaseService;
import com.hgb7725.botchattyapp.firebase.UserFirebaseService;
import com.hgb7725.botchattyapp.firebase.UserRelationshipService;
import com.hgb7725.botchattyapp.models.FileItem;
import com.hgb7725.botchattyapp.models.MediaItem;
import com.hgb7725.botchattyapp.models.User;
import com.hgb7725.botchattyapp.utilities.BlockStatusChecker;
import com.hgb7725.botchattyapp.utilities.Constants;
import com.hgb7725.botchattyapp.utilities.FileUtils;
import com.hgb7725.botchattyapp.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class UserProfileActivity extends AppCompatActivity {

    private ActivityUserProfileBinding binding;
    private User user;
    private PreferenceManager preferenceManager;
    private UserFirebaseService userFirebaseService;
    private UserRelationshipService relationshipService;
    private MediaFirebaseService mediaFirebaseService;
    private BlockStatusChecker blockStatusChecker;
    private String userId;
    private static final String TAG = "UserProfileActivity";
    private static final int MAX_PREVIEW_MEDIA = 4;
    private static final int MAX_PREVIEW_FILES = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        preferenceManager = new PreferenceManager(getApplicationContext());
        userFirebaseService = new UserFirebaseService(this, preferenceManager);
        relationshipService = new UserRelationshipService(this, preferenceManager);
        mediaFirebaseService = new MediaFirebaseService(this, preferenceManager);
        blockStatusChecker = new BlockStatusChecker(this, preferenceManager);
        
        // Show loading state
        showLoading(true);
        
        // Get the user ID from intent
        userId = getIntent().getStringExtra(Constants.KEY_USER_ID);
        
        if (userId != null) {
            loadUserDetails();
            loadRelationshipStatus();
            loadNickname();
            loadSharedMedia();
            loadSharedFiles(); // Add this line to load shared files
        } else {
            Toast.makeText(this, "User ID not available", Toast.LENGTH_SHORT).show();
            finish();
        }
        
        setListeners();
    }
    
    private void loadUserDetails() {
        // Use the UserFirebaseService to get user data by ID
        userFirebaseService.getUserById(userId, new UserFirebaseService.UserProfileListener() {
            @Override
            public void onUserDataLoaded(User userData) {
                user = userData;
                displayUserInfo();
                showLoading(false);
            }

            @Override
            public void onFailure(String errorMessage) {
                showLoading(false);
                Toast.makeText(UserProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                // Go back to previous screen if we can't load user data
                finish();
            }
        });
    }
    
    private void loadRelationshipStatus() {
        relationshipService.getRelationshipStatus(userId, new UserRelationshipService.RelationshipStatusListener() {
            @Override
            public void onStatusLoaded(boolean isBlocked, boolean isMuted) {
                // Update UI to reflect the current relationship status
                binding.switchBlock.setChecked(isBlocked);
                binding.switchMute.setChecked(isMuted);
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to load relationship status: " + errorMessage);
                // Default to unblocked/unmuted if we can't load status
                binding.switchBlock.setChecked(false);
                binding.switchMute.setChecked(false);
            }
        });
    }
    
    private void loadNickname() {
        relationshipService.getNickname(userId, new UserRelationshipService.NicknameListener() {
            @Override
            public void onNicknameLoaded(String nickname) {
                // Update the nickname field with the current nickname (if any)
                if (nickname != null && !nickname.isEmpty()) {
                    binding.editTextNickname.setText(nickname);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to load nickname: " + errorMessage);
                // Leave the nickname field empty if we can't load the nickname
            }
        });
    }
    
    private void loadSharedMedia() {
        // Load a preview of shared media
        mediaFirebaseService.fetchMediaPreview(userId, MAX_PREVIEW_MEDIA + 1, new MediaFirebaseService.MediaFetchListener() {
            @Override
            public void onMediaFetched(List<MediaItem> mediaItems) {
                if (mediaItems.isEmpty()) {
                    // Hide the shared media section if there are no media items
                    binding.sharedMediaLabel.setVisibility(View.GONE);
                    binding.viewAllMedia.setVisibility(View.GONE);
                    binding.recyclerViewSharedMedia.setVisibility(View.GONE);
                } else {
                    // Show the shared media section
                    binding.sharedMediaLabel.setVisibility(View.VISIBLE);
                    binding.viewAllMedia.setVisibility(View.VISIBLE);
                    binding.recyclerViewSharedMedia.setVisibility(View.VISIBLE);
                    
                    // Determine if we need a "View More" button
                    boolean showMoreButton = mediaItems.size() > MAX_PREVIEW_MEDIA;
                    int totalCount = mediaItems.size();
                    
                    // If we have more items than MAX_PREVIEW_MEDIA, limit the list
                    List<MediaItem> previewItems = mediaItems;
                    if (showMoreButton) {
                        previewItems = new ArrayList<>(mediaItems.subList(0, MAX_PREVIEW_MEDIA));
                        // Add the last item for the "View More" button thumbnail
                        previewItems.add(mediaItems.get(MAX_PREVIEW_MEDIA));
                    }
                    
                    // Set up the RecyclerView
                    binding.recyclerViewSharedMedia.setLayoutManager(
                            new LinearLayoutManager(UserProfileActivity.this, LinearLayoutManager.HORIZONTAL, false));
                    
                    MediaPreviewAdapter adapter = new MediaPreviewAdapter(
                            UserProfileActivity.this, previewItems, showMoreButton, totalCount);
                    
                    adapter.setOnMoreButtonClickListener(() -> {
                        openSharedMediaGallery();
                    });
                    
                    binding.recyclerViewSharedMedia.setAdapter(adapter);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Failed to load shared media: " + errorMessage);
                // Hide the shared media section if there's an error
                binding.sharedMediaLabel.setVisibility(View.GONE);
                binding.viewAllMedia.setVisibility(View.GONE);
                binding.recyclerViewSharedMedia.setVisibility(View.GONE);
            }
        });
    }
    
    private void loadSharedFiles() {
        // Load a preview of shared files
        mediaFirebaseService.fetchFilesPreview(userId, MAX_PREVIEW_FILES, new MediaFirebaseService.FileFetchListener() {
            @Override
            public void onFilesFetched(List<FileItem> fileItems) {
                if (fileItems.isEmpty()) {
                    // Hide the shared files section if there are no files
                    binding.sharedFilesLabel.setVisibility(View.GONE);
                    binding.viewAllFiles.setVisibility(View.GONE);
                    binding.containerSharedFiles.setVisibility(View.GONE);
                } else {
                    // Show the shared files section
                    binding.sharedFilesLabel.setVisibility(View.VISIBLE);
                    binding.viewAllFiles.setVisibility(View.VISIBLE);
                    binding.containerSharedFiles.setVisibility(View.VISIBLE);
                    
                    // Clear existing views in the container
                    binding.containerSharedFiles.removeAllViews();
                    
                    // Add file items to the container
                    for (int i = 0; i < Math.min(fileItems.size(), MAX_PREVIEW_FILES); i++) {
                        FileItem fileItem = fileItems.get(i);
                        View fileItemView = getLayoutInflater().inflate(R.layout.item_file, binding.containerSharedFiles, false);
                        
                        // Get views from the inflated layout
                        TextView fileName = fileItemView.findViewById(R.id.textFileName);
                        ImageView fileIcon = fileItemView.findViewById(R.id.imageFileIcon);
                        ImageView downloadIcon = fileItemView.findViewById(R.id.imageDownload);
                        
                        // Set file name and icon
                        fileName.setText(fileItem.getFileName());
                        fileIcon.setImageResource(FileUtils.getFileIconRes(fileItem.getFileName()));
                        
                        // Set click listeners - using UserProfileActivity.this for context
                        final String fileUrl = fileItem.getFileUrl();
                        final String fName = fileItem.getFileName();
                        
                        downloadIcon.setOnClickListener(v -> 
                            FileUtils.confirmAndDownloadFile(UserProfileActivity.this, fileUrl, fName)
                        );
                        
                        fileItemView.setOnClickListener(v -> 
                            FileUtils.confirmAndDownloadFile(UserProfileActivity.this, fileUrl, fName)
                        );
                        
                        // Add to the container
                        binding.containerSharedFiles.addView(fileItemView);
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Failed to load shared files: " + errorMessage);
                // Hide the shared files section if there's an error
                binding.sharedFilesLabel.setVisibility(View.GONE);
                binding.viewAllFiles.setVisibility(View.GONE);
                binding.containerSharedFiles.setVisibility(View.GONE);
            }
        });
    }
    
    private void openSharedMediaGallery() {
        Intent intent = new Intent(this, SharedMediaActivity.class);
        intent.putExtra(Constants.KEY_USER_ID, userId);
        intent.putExtra("userName", user.getName());
        startActivity(intent);
    }
    
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.GONE);
        }
    }
    
    private void displayUserInfo() {
        // Display user information in the UI
        if (user != null) {
            binding.textName.setText(user.getName());
            
            // Check if email is available before setting it
            String email = user.getEmail();
            if (email != null && !email.isEmpty()) {
                binding.textEmail.setText(email);
                binding.textEmail.setVisibility(View.VISIBLE);
            } else {
                binding.textEmail.setVisibility(View.GONE);
            }
            
            // Load profile image if available
            String imageBase64 = user.getImage();
            if (imageBase64 != null && !imageBase64.isEmpty()) {
                byte[] bytes = Base64.decode(imageBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                binding.imageProfile.setImageBitmap(bitmap);
            }
        }
    }
    
    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        
        // Set listeners for the action buttons and switches
        binding.layoutMuteNotifications.setOnClickListener(v -> {
            boolean newMuteStatus = !binding.switchMute.isChecked();
            binding.switchMute.setChecked(newMuteStatus);
            
            // Update mute status in Firebase
            updateMuteStatus(newMuteStatus);
        });
        
        binding.switchMute.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                // Only update when user directly clicks the switch, not when programmatically changed
                updateMuteStatus(isChecked);
            }
        });
        
        binding.layoutBlockUser.setOnClickListener(v -> {
            boolean newBlockStatus = !binding.switchBlock.isChecked();
            binding.switchBlock.setChecked(newBlockStatus);
            
            // Update block status in Firebase
            updateBlockStatus(newBlockStatus);
        });
        
        binding.switchBlock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                // Only update when user directly clicks the switch, not when programmatically changed
                updateBlockStatus(isChecked);
            }
        });
        
        // Set click listener for the nickname EditText to enable it for editing when clicked
        binding.editTextNickname.setOnClickListener(v -> {
            binding.editTextNickname.setFocusable(true);
            binding.editTextNickname.setFocusableInTouchMode(true);
            binding.editTextNickname.requestFocus();
        });
        
        // Set listener for the save nickname button
        binding.buttonSaveNickname.setOnClickListener(v -> {
            String nickname = binding.editTextNickname.getText().toString().trim();
            updateNickname(nickname);
        });
        
        // Set listener for the delete nickname button
        binding.buttonDeleteNickname.setOnClickListener(v -> {
            deleteNickname();
        });
        
        binding.layoutReport.setOnClickListener(v -> {
            Toast.makeText(this, "Report user functionality will be implemented soon", Toast.LENGTH_SHORT).show();
        });
        
        // Also allow clicking on the entire nickname layout to focus on the EditText
        binding.layoutSetNickname.setOnClickListener(v -> {
            binding.editTextNickname.setFocusable(true);
            binding.editTextNickname.setFocusableInTouchMode(true);
            binding.editTextNickname.requestFocus();
        });
        
        // Set listener for View All button in Shared Media section
        binding.viewAllMedia.setOnClickListener(v -> {
            openSharedMediaGallery();
        });

        // Add listener for View All Files button
        binding.viewAllFiles.setOnClickListener(v -> {
            openSharedFilesActivity();
        });
    }
    
    private void updateBlockStatus(boolean isBlocked) {
        // Show loading while updating
        showLoading(true);
        
        relationshipService.updateBlockStatus(userId, isBlocked, new UserRelationshipService.RelationshipOperationListener() {
            @Override
            public void onSuccess() {
                showLoading(false);
                // Show feedback to user
                String message = isBlocked 
                        ? "User blocked. They cannot message you, but can still see your messages." 
                        : "User unblocked";
                Toast.makeText(UserProfileActivity.this, message, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                showLoading(false);
                // Revert the switch if the operation failed
                binding.switchBlock.setChecked(!isBlocked);
                Toast.makeText(UserProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateMuteStatus(boolean isMuted) {
        // Show loading while updating
        showLoading(true);
        
        relationshipService.updateMuteStatus(userId, isMuted, new UserRelationshipService.RelationshipOperationListener() {
            @Override
            public void onSuccess() {
                showLoading(false);
                // Show feedback to user
                String message = isMuted ? "Notifications muted" : "Notifications unmuted";
                Toast.makeText(UserProfileActivity.this, message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                showLoading(false);
                // Revert the switch if the operation failed
                binding.switchMute.setChecked(!isMuted);
                Toast.makeText(UserProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateNickname(String nickname) {
        // Show loading while updating
        showLoading(true);
        
        // Hide keyboard and clear focus from EditText
        clearFocusAndHideKeyboard();
        
        // If nickname is empty, pass null to remove the nickname
        String nicknameToSave = TextUtils.isEmpty(nickname) ? null : nickname;
        
        relationshipService.updateNickname(userId, nicknameToSave, new UserRelationshipService.RelationshipOperationListener() {
            @Override
            public void onSuccess() {
                showLoading(false);
                // Show feedback to user
                String message = TextUtils.isEmpty(nickname) 
                        ? "Nickname removed" 
                        : "Nickname updated";
                Toast.makeText(UserProfileActivity.this, message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                showLoading(false);
                Toast.makeText(UserProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void clearFocusAndHideKeyboard() {
        // Clear focus from EditText
        binding.editTextNickname.clearFocus();
        
        // Make EditText not focusable again
        binding.editTextNickname.setFocusable(false);
        
        // Hide keyboard
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
                getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(binding.editTextNickname.getWindowToken(), 0);
        }
    }
    
    private void deleteNickname() {
        // Show loading while updating
        showLoading(true);
        
        relationshipService.updateNickname(userId, null, new UserRelationshipService.RelationshipOperationListener() {
            @Override
            public void onSuccess() {
                showLoading(false);
                // Clear the nickname field
                binding.editTextNickname.setText("");
                // Show feedback to user
                Toast.makeText(UserProfileActivity.this, "Nickname deleted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                showLoading(false);
                Toast.makeText(UserProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openSharedFilesActivity() {
        Intent intent = new Intent(this, SharedFilesActivity.class);
        intent.putExtra(Constants.KEY_USER_ID, userId);
        intent.putExtra("userName", user.getName());
        startActivity(intent);
    }
}