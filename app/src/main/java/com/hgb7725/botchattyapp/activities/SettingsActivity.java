package com.hgb7725.botchattyapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.SwitchCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hgb7725.botchattyapp.R;
import com.hgb7725.botchattyapp.databinding.ActivitySettingsBinding;
import com.hgb7725.botchattyapp.utilities.Constants;
import com.hgb7725.botchattyapp.utilities.PreferenceManager;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.HashMap;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchNotifications;
    private SwitchCompat switchOnlineStatus;
    private AppCompatButton buttonLogout;
    private AppCompatButton buttonChangePassword;
    private AppCompatButton buttonDeleteAccount;
    private TextView textUsername;
    private TextView textEmail;
    private RoundedImageView imageProfile;
    private PreferenceManager preferenceManager;
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        preferenceManager = new PreferenceManager(getApplicationContext());
        
        initViews();
        setListeners();
        loadUserData();
    }
    
    private void initViews() {
        // Initialize views
        AppCompatImageView imageBack = findViewById(R.id.imageBack);
        imageProfile = findViewById(R.id.imageProfile);
        textUsername = findViewById(R.id.textUsername);
        textEmail = findViewById(R.id.textEmail);
        switchNotifications = findViewById(R.id.switchNotifications);
        switchOnlineStatus = findViewById(R.id.switchOnlineStatus);
        buttonLogout = findViewById(R.id.buttonLogout);
        buttonChangePassword = findViewById(R.id.buttonChangePassword);
        buttonDeleteAccount = findViewById(R.id.buttonDeleteAccount);
        
        // Initialize setting items
        LinearLayout layoutPrivacy = findViewById(R.id.layoutPrivacy);
        LinearLayout layoutHelp = findViewById(R.id.layoutHelp);
        LinearLayout layoutAbout = findViewById(R.id.layoutAbout);
    }
    
    private void setListeners() {
        // Set up back button
        AppCompatImageView imageBack = findViewById(R.id.imageBack);
        imageBack.setOnClickListener(v -> onBackPressed());
        
        // Set up settings navigation
        LinearLayout layoutPrivacy = findViewById(R.id.layoutPrivacy);
        layoutPrivacy.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, PrivacyPolicyActivity.class);
            startActivity(intent);
        });
        
        LinearLayout layoutHelp = findViewById(R.id.layoutHelp);
        layoutHelp.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, HelpSupportActivity.class);
            startActivity(intent);
        });
        
        LinearLayout layoutAbout = findViewById(R.id.layoutAbout);
        layoutAbout.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, AboutActivity.class);
            startActivity(intent);
        });
        
        // Set up switch listeners
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save notification preference
            preferenceManager.putBoolean(KEY_NOTIFICATIONS_ENABLED, isChecked);
            showToast(isChecked ? "Notifications enabled" : "Notifications disabled");
        });
        
        switchOnlineStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                // Save online status visibility preference
                preferenceManager.putBoolean(Constants.KEY_ONLINE_STATUS_VISIBLE, isChecked);
                // Update the online status in Firestore
                updateOnlineStatusVisibility(isChecked);
                showToast(isChecked ? "Online status visible" : "Online status hidden");
            } catch (Exception e) {
                Log.e(TAG, "Error updating online status: " + e.getMessage());
                showToast("Error updating status. Please try again.");
            }
        });
        
        // Set up button listeners
        buttonLogout.setOnClickListener(v -> signOut());
        
        buttonChangePassword.setOnClickListener(v -> {
            showToast("Change Password feature coming soon!");
        });
        
        buttonDeleteAccount.setOnClickListener(v -> {
            showDeleteAccountConfirmation();
        });
        
        // Initialize notification switch with saved preference
        try {
            // Initialize notification switch with default value true
            boolean notificationsEnabled = preferenceManager.getBoolean(KEY_NOTIFICATIONS_ENABLED);
            
            // Check if notifications setting exists in shared preferences
            boolean notificationKeyExists = preferenceManager.keyExists(KEY_NOTIFICATIONS_ENABLED);
            if (!notificationKeyExists) {
                // Key doesn't exist, set default to true
                preferenceManager.putBoolean(KEY_NOTIFICATIONS_ENABLED, true);
                notificationsEnabled = true;
            }
            
            switchNotifications.setChecked(notificationsEnabled);
            
            // Initialize online status switch
            boolean onlineStatusVisible = preferenceManager.getBoolean(Constants.KEY_ONLINE_STATUS_VISIBLE);
            
            // Check if online status visibility setting exists
            boolean onlineStatusKeyExists = preferenceManager.keyExists(Constants.KEY_ONLINE_STATUS_VISIBLE);
            if (!onlineStatusKeyExists) {
                // Key doesn't exist, set default to true
                preferenceManager.putBoolean(Constants.KEY_ONLINE_STATUS_VISIBLE, true);
                onlineStatusVisible = true;
            }
            
            switchOnlineStatus.setChecked(onlineStatusVisible);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing switches: " + e.getMessage());
            // Set defaults if there's an error
            switchNotifications.setChecked(true);
            switchOnlineStatus.setChecked(true);
        }
    }
    
    private void loadUserData() {
        try {
            // Set user name
            String name = preferenceManager.getString(Constants.KEY_NAME);
            if (name != null) {
                textUsername.setText(name);
            } else {
                textUsername.setText("User");
            }
            
            // Set user email
            String email = preferenceManager.getString(Constants.KEY_EMAIL);
            if (email != null) {
                textEmail.setText(email);
            } else {
                textEmail.setText("No email available");
            }
            
            // Set profile image
            String imageString = preferenceManager.getString(Constants.KEY_IMAGE);
            if (imageString != null) {
                byte[] bytes = Base64.decode(imageString, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                imageProfile.setImageBitmap(bitmap);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading user details: " + e.getMessage());
            // Don't crash if we can't load user details
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
            // Update the visibility setting
            updates.put(Constants.KEY_ONLINE_STATUS_VISIBLE, isVisible);
            
            // Also immediately update the availability status based on visibility setting
            // If visibility is turned off, set availability to 0 (offline)
            // If visibility is turned on, set availability to 1 (online)
            updates.put(Constants.KEY_AVAILABILITY, isVisible ? Constants.AVAILABILITY_ONLINE : Constants.AVAILABILITY_OFFLINE);
            
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
    
    private void showDeleteAccountConfirmation() {
        // Show a confirmation dialog before deleting account
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Delete Account");
        builder.setMessage("Are you sure you want to delete your account? This action cannot be undone.");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            // Handle account deletion logic here
            Toast.makeText(SettingsActivity.this, "Account deletion initiated", Toast.LENGTH_SHORT).show();
            // You would implement actual account deletion logic here
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}