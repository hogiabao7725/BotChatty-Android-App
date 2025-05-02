package com.hgb7725.botchattyapp.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hgb7725.botchattyapp.databinding.ActivityUserProfileBinding;
import com.hgb7725.botchattyapp.firebase.UserFirebaseService;
import com.hgb7725.botchattyapp.models.User;
import com.hgb7725.botchattyapp.utilities.Constants;
import com.hgb7725.botchattyapp.utilities.PreferenceManager;

public class UserProfileActivity extends AppCompatActivity {

    private ActivityUserProfileBinding binding;
    private User user;
    private PreferenceManager preferenceManager;
    private UserFirebaseService userFirebaseService;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        preferenceManager = new PreferenceManager(getApplicationContext());
        userFirebaseService = new UserFirebaseService(this, preferenceManager);
        
        // Show loading state
        showLoading(true);
        
        // Get the user ID from intent
        userId = getIntent().getStringExtra(Constants.KEY_USER_ID);
        
        if (userId != null) {
            loadUserDetails();
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
            binding.switchMute.toggle();
            // Placeholder for mute functionality
            String status = binding.switchMute.isChecked() ? "muted" : "unmuted";
            Toast.makeText(this, "Notifications " + status, Toast.LENGTH_SHORT).show();
        });
        
        binding.layoutBlockUser.setOnClickListener(v -> {
            binding.switchBlock.toggle();
            // Placeholder for block functionality
            String status = binding.switchBlock.isChecked() ? "blocked" : "unblocked";
            Toast.makeText(this, "User " + status, Toast.LENGTH_SHORT).show();
        });
        
        binding.layoutClearChat.setOnClickListener(v -> {
            Toast.makeText(this, "Clear chat functionality will be implemented soon", Toast.LENGTH_SHORT).show();
        });
        
        binding.layoutReport.setOnClickListener(v -> {
            Toast.makeText(this, "Report user functionality will be implemented soon", Toast.LENGTH_SHORT).show();
        });
    }
}