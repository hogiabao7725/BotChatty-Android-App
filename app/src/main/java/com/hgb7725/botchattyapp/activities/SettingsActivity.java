package com.hgb7725.botchattyapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hgb7725.botchattyapp.databinding.ActivitySettingsBinding;
import com.hgb7725.botchattyapp.utilities.Constants;
import com.hgb7725.botchattyapp.utilities.PreferenceManager;

import java.util.HashMap;

public class SettingsActivity extends BaseActivity {
    
    private ActivitySettingsBinding binding;
    private PreferenceManager preferenceManager;
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_ONLINE_STATUS_VISIBLE = "online_status_visible";
    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            binding = ActivitySettingsBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            
            preferenceManager = new PreferenceManager(getApplicationContext());
            
            loadUserDetails();
            setListeners();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
            showToast("Error loading settings. Please try again.");
            finish();
        }
    }
    
    private void loadUserDetails() {
        try {
            // Set user name
            String name = preferenceManager.getString(Constants.KEY_NAME);
            if (name != null) {
                binding.textUsername.setText(name);
            } else {
                binding.textUsername.setText("User");
            }
            
            // Set user email
            String email = preferenceManager.getString(Constants.KEY_EMAIL);
            if (email != null) {
                binding.textEmail.setText(email);
            } else {
                binding.textEmail.setText("No email available");
            }
            
            // Set profile image
            String imageString = preferenceManager.getString(Constants.KEY_IMAGE);
            if (imageString != null) {
                byte[] bytes = Base64.decode(imageString, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                binding.imageProfile.setImageBitmap(bitmap);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading user details: " + e.getMessage());
            // Don't crash if we can't load user details
        }
    }
    
    private void setListeners() {
        // Back button listener
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        
        // Logout button listener
        binding.buttonLogout.setOnClickListener(v -> signOut());
        
        // Change Password button listener
        binding.buttonChangePassword.setOnClickListener(v -> {
            showToast("Change Password feature coming soon!");
        });
        
        // Delete Account button listener
        binding.buttonDeleteAccount.setOnClickListener(v -> {
            showToast("Delete Account feature coming soon!");
        });
        
        // Privacy policy listener
        binding.layoutPrivacy.setOnClickListener(v -> {
            showToast("Privacy Policy coming soon!");
        });
        
        // Help & support listener
        binding.layoutHelp.setOnClickListener(v -> {
            showToast("Help & Support coming soon!");
        });
        
        // About listener
        binding.layoutAbout.setOnClickListener(v -> {
            showToast("About BotChatty App coming soon!");
        });
        
        // Notifications toggle listener
        binding.switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save notification preference
            preferenceManager.putBoolean(KEY_NOTIFICATIONS_ENABLED, isChecked);
            showToast(isChecked ? "Notifications enabled" : "Notifications disabled");
        });
        
        // Online Status toggle listener
        binding.switchOnlineStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                // Save online status visibility preference
                preferenceManager.putBoolean(KEY_ONLINE_STATUS_VISIBLE, isChecked);
                // Update the online status in Firestore
                updateOnlineStatusVisibility(isChecked);
                showToast(isChecked ? "Online status visible" : "Online status hidden");
            } catch (Exception e) {
                Log.e(TAG, "Error updating online status: " + e.getMessage());
                showToast("Error updating status. Please try again.");
            }
        });
        
        // Initialize notification switch with saved preference
        try {
            // Initialize notification switch with default value true
            boolean notificationsEnabled = preferenceManager.getBoolean(KEY_NOTIFICATIONS_ENABLED);
            
            // The error is in this section - we should not attempt to get a Boolean as a String
            // Instead, we can check if the value exists differently
            
            // Check if notifications setting exists in shared preferences
            boolean notificationKeyExists = preferenceManager.keyExists(KEY_NOTIFICATIONS_ENABLED);
            if (!notificationKeyExists) {
                // Key doesn't exist, set default to true
                preferenceManager.putBoolean(KEY_NOTIFICATIONS_ENABLED, true);
                notificationsEnabled = true;
            }
            
            binding.switchNotifications.setChecked(notificationsEnabled);
            
            // Initialize online status switch
            boolean onlineStatusVisible = preferenceManager.getBoolean(KEY_ONLINE_STATUS_VISIBLE);
            
            // Similarly fix this part
            boolean onlineStatusKeyExists = preferenceManager.keyExists(KEY_ONLINE_STATUS_VISIBLE);
            if (!onlineStatusKeyExists) {
                // Key doesn't exist, set default to true
                preferenceManager.putBoolean(KEY_ONLINE_STATUS_VISIBLE, true);
                onlineStatusVisible = true;
            }
            
            binding.switchOnlineStatus.setChecked(onlineStatusVisible);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing switches: " + e.getMessage());
            // Set defaults if there's an error
            binding.switchNotifications.setChecked(true);
            binding.switchOnlineStatus.setChecked(true);
        }
    }
    
    private void updateOnlineStatusVisibility(boolean isVisible) {
        try {
            FirebaseFirestore database = FirebaseFirestore.getInstance();
            String userId = preferenceManager.getString(Constants.KEY_USER_ID);
            if (userId == null) {
                Log.e(TAG, "User ID is null when updating online status");
                return;
            }
            
            DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
                    .document(userId);
            HashMap<String, Object> updates = new HashMap<>();
            updates.put("online_status_visible", isVisible);
            documentReference.update(updates)
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Firebase error updating online status: " + e.getMessage());
                        showToast("Unable to update online status visibility");
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in updateOnlineStatusVisibility: " + e.getMessage());
        }
    }
    
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    
    private void signOut() {
        showToast("Signing out...");
        try {
            FirebaseFirestore database = FirebaseFirestore.getInstance();
            String userId = preferenceManager.getString(Constants.KEY_USER_ID);
            if (userId == null) {
                // If we can't get the user ID, just clear preferences and go to sign in
                preferenceManager.clear();
                startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                finish();
                return;
            }
            
            DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
                    .document(userId);
            HashMap<String, Object> updates = new HashMap<>();
            updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
            documentReference.update(updates)
                    .addOnSuccessListener(unused -> {
                        preferenceManager.clear();
                        startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Firebase error signing out: " + e.getMessage());
                        // Even if Firebase update fails, still sign out locally
                        preferenceManager.clear();
                        startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                        finish();
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in signOut: " + e.getMessage());
            // If any exception occurs, still try to sign out
            preferenceManager.clear();
            startActivity(new Intent(getApplicationContext(), SignInActivity.class));
            finish();
        }
    }
}