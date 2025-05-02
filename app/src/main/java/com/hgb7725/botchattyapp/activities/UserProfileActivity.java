package com.hgb7725.botchattyapp.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hgb7725.botchattyapp.databinding.ActivityUserProfileBinding;
import com.hgb7725.botchattyapp.firebase.UserFirebaseService;
import com.hgb7725.botchattyapp.firebase.UserRelationshipService;
import com.hgb7725.botchattyapp.models.User;
import com.hgb7725.botchattyapp.utilities.BlockStatusChecker;
import com.hgb7725.botchattyapp.utilities.Constants;
import com.hgb7725.botchattyapp.utilities.PreferenceManager;

public class UserProfileActivity extends AppCompatActivity {

    private ActivityUserProfileBinding binding;
    private User user;
    private PreferenceManager preferenceManager;
    private UserFirebaseService userFirebaseService;
    private UserRelationshipService relationshipService;
    private BlockStatusChecker blockStatusChecker;
    private String userId;
    private static final String TAG = "UserProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        preferenceManager = new PreferenceManager(getApplicationContext());
        userFirebaseService = new UserFirebaseService(this, preferenceManager);
        relationshipService = new UserRelationshipService(this, preferenceManager);
        blockStatusChecker = new BlockStatusChecker(this, preferenceManager);
        
        // Show loading state
        showLoading(true);
        
        // Get the user ID from intent
        userId = getIntent().getStringExtra(Constants.KEY_USER_ID);
        
        if (userId != null) {
            loadUserDetails();
            loadRelationshipStatus();
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
        
        binding.layoutClearChat.setOnClickListener(v -> {
            Toast.makeText(this, "Clear chat functionality will be implemented soon", Toast.LENGTH_SHORT).show();
        });
        
        binding.layoutReport.setOnClickListener(v -> {
            Toast.makeText(this, "Report user functionality will be implemented soon", Toast.LENGTH_SHORT).show();
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
}