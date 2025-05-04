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
import com.hgb7725.botchattyapp.firebase.UserFirebaseService;
import com.hgb7725.botchattyapp.models.User;
import com.hgb7725.botchattyapp.utilities.Constants;
import com.hgb7725.botchattyapp.utilities.PreferenceManager;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.HashMap;

public class SettingsActivity extends BaseActivity {

    private SwitchCompat switchNotifications;
    private SwitchCompat switchOnlineStatus;
    private AppCompatButton buttonLogout;
    private AppCompatButton buttonChangePassword;
    private AppCompatButton buttonDeleteAccount;
    private TextView textUsername;
    private TextView textEmail;
    private RoundedImageView imageProfile;
    private PreferenceManager preferenceManager;
    private UserFirebaseService userService;
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String TAG = "SettingsActivity";
    private AppCompatImageView imageEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        preferenceManager = new PreferenceManager(getApplicationContext());
        userService = new UserFirebaseService(getApplicationContext(), preferenceManager);
        
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
        imageEdit = findViewById(R.id.imageEdit);
        
        // Initialize setting items
        LinearLayout layoutPrivacy = findViewById(R.id.layoutPrivacy);
        LinearLayout layoutHelp = findViewById(R.id.layoutHelp);
        LinearLayout layoutAbout = findViewById(R.id.layoutAbout);
    }
    
    private void setListeners() {
        // Set up back button
        AppCompatImageView imageBack = findViewById(R.id.imageBack);
        imageBack.setOnClickListener(v -> onBackPressed());
        
        // Set up edit profile button
        imageEdit.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });
        
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
                // Update the online status in Firestore using UserFirebaseService
                userService.updateOnlineStatusVisibility(isChecked, new UserFirebaseService.OnlineStatusListener() {
                    @Override
                    public void onSuccess() {
                        showToast(isChecked ? "Online status visible" : "Online status hidden");
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Error updating online status: " + errorMessage);
                        showToast("Error updating status. Please try again.");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error updating online status: " + e.getMessage());
                showToast("Error updating status. Please try again.");
            }
        });
        
        // Set up button listeners
        buttonLogout.setOnClickListener(v -> signOut());
        
        buttonChangePassword.setOnClickListener(v -> {
            String email = textEmail.getText().toString().trim();
            if (email != null && !email.isEmpty()) {
                Intent intent = new Intent(SettingsActivity.this, ResetPasswordActivity.class);
                intent.putExtra("email", email);
                startActivity(intent);
            } else {
                showToast("Unable to retrieve your email. Please sign in again.");
            }
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
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh user data when returning from edit profile
        loadUserData();
    }
    
    private void loadUserData() {
        // Get the user ID from intent
        String userId = getIntent().getStringExtra(Constants.KEY_USER_ID);
        if (userId == null) {
            // Fallback to preference manager if not passed in intent
            userId = preferenceManager.getString(Constants.KEY_USER_ID);
        }
        
        if (userId != null) {
            // Show loading state
            showLoading(true);
            
            // Use UserFirebaseService to get user data
            userService.getUserById(userId, new UserFirebaseService.UserProfileListener() {
                @Override
                public void onUserDataLoaded(User user) {
                    // Update UI with user data
                    if (user != null) {
                        textUsername.setText(user.getName());
                        textEmail.setText(user.getEmail());
                        
                        // Load profile image if available
                        String imageString = user.getImage();
                        if (imageString != null) {
                            byte[] bytes = Base64.decode(imageString, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            imageProfile.setImageBitmap(bitmap);
                        }
                    }
                    showLoading(false);
                }

                @Override
                public void onFailure(String errorMessage) {
                    showLoading(false);
                    showToast("Failed to load user data: " + errorMessage);
                    
                    // Fallback to preference manager data
                    String name = preferenceManager.getString(Constants.KEY_NAME);
                    if (name != null) {
                        textUsername.setText(name);
                    } else {
                        textUsername.setText("User");
                    }
                    
                    String email = preferenceManager.getString(Constants.KEY_EMAIL);
                    if (email != null) {
                        textEmail.setText(email);
                    } else {
                        textEmail.setText("Email not available");
                    }
                    
                    String imageString = preferenceManager.getString(Constants.KEY_IMAGE);
                    if (imageString != null) {
                        byte[] bytes = Base64.decode(imageString, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        imageProfile.setImageBitmap(bitmap);
                    }
                }
            });
        } else {
            // If user ID is not available anywhere, use fallback data
            String name = preferenceManager.getString(Constants.KEY_NAME);
            if (name != null) {
                textUsername.setText(name);
            } else {
                textUsername.setText("User");
            }
            
            String email = preferenceManager.getString(Constants.KEY_EMAIL);
            if (email != null) {
                textEmail.setText(email);
            } else {
                textEmail.setText("Email not available");
            }
            
            String imageString = preferenceManager.getString(Constants.KEY_IMAGE);
            if (imageString != null) {
                byte[] bytes = Base64.decode(imageString, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                imageProfile.setImageBitmap(bitmap);
            }
        }
    }
    
    private void showLoading(boolean isLoading) {
        // If you have a progress bar, show/hide it based on loading state
        // For now, we'll skip this since we don't know if there's a progress bar
    }
    
    private void updateOnlineStatusVisibility(boolean isVisible) {
        userService.updateOnlineStatusVisibility(isVisible, new UserFirebaseService.OnlineStatusListener() {
            @Override
            public void onSuccess() {
                // Status updated successfully
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Error updating online status: " + errorMessage);
                showToast("Unable to update online status visibility");
            }
        });
    }
    
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    
    private void signOut() {
        showToast("Signing out...");
        userService.signOut(new UserFirebaseService.SignOutListener() {
            @Override
            public void onSignOutSuccess() {
                startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                finish();
            }

            @Override
            public void onSignOutFailure(String errorMessage) {
                Log.e(TAG, "Error signing out: " + errorMessage);
                // Even if there's an error, try to sign out locally
                startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                finish();
            }
        });
    }
    
    private void showDeleteAccountConfirmation() {
        // Show a confirmation dialog with additional warning
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Delete Account");
        builder.setMessage("Are you sure you want to delete your account? This will permanently delete:\n\n" +
                "• Your user profile\n" +
                "• All your conversations\n" +
                "• All messages you've sent or received\n\n" +
                "This action cannot be undone.");
        
        builder.setPositiveButton("Delete", (dialog, which) -> {
            // Show a second confirmation dialog requiring the user to type "DELETE"
            final android.app.AlertDialog.Builder confirmBuilder = new android.app.AlertDialog.Builder(this);
            confirmBuilder.setTitle("Confirm Deletion");
            confirmBuilder.setMessage("Type DELETE (all caps) to confirm account deletion:");

            // Add an input field
            final android.widget.EditText input = new android.widget.EditText(this);
            confirmBuilder.setView(input);

            confirmBuilder.setPositiveButton("Confirm", (confirmDialog, confirmWhich) -> {
                // Check if user typed DELETE
                String confirmText = input.getText().toString();
                if (confirmText.equals("DELETE")) {
                    // Show a loading dialog while deletion is in progress
                    android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
                    progressDialog.setMessage("Deleting account...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    // Use UserFirebaseService to handle account deletion
                    userService.deleteAccount(new UserFirebaseService.SignOutListener() {
                        @Override
                        public void onSignOutSuccess() {
                            // Dismiss the loading dialog
                            progressDialog.dismiss();
                            showToast("Account successfully deleted");
                            startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                            finish();
                        }

                        @Override
                        public void onSignOutFailure(String errorMessage) {
                            // Dismiss the loading dialog
                            progressDialog.dismiss();
                            showToast("Failed to delete account: " + errorMessage);
                        }
                    });
                } else {
                    showToast("Account deletion cancelled. Text did not match.");
                }
            });

            confirmBuilder.setNegativeButton("Cancel", (confirmDialog, confirmWhich) -> {
                showToast("Account deletion cancelled");
            });

            confirmBuilder.show();
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            showToast("Account deletion cancelled");
        });
        
        builder.show();
    }
}