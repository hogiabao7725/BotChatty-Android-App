package com.hgb7725.botchattyapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.AppCompatImageView;

import com.google.android.material.button.MaterialButton;
import com.hgb7725.botchattyapp.R;
import com.hgb7725.botchattyapp.firebase.UserFirebaseService;
import com.hgb7725.botchattyapp.models.User;
import com.hgb7725.botchattyapp.utilities.Constants;
import com.hgb7725.botchattyapp.utilities.PreferenceManager;
import com.makeramen.roundedimageview.RoundedImageView;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class EditProfileActivity extends BaseActivity {

    private static final String TAG = "EditProfileActivity";
    
    private RoundedImageView imageProfile;
    private TextView textAddImage;
    private EditText inputName, inputEmail;
    private ProgressBar progressBar;
    private MaterialButton buttonSave;
    
    private PreferenceManager preferenceManager;
    private UserFirebaseService userService;
    private String encodedImage;
    private User currentUser;
    
    private ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            imageProfile.setImageBitmap(bitmap);
                            textAddImage.setVisibility(View.GONE);
                            encodedImage = encodeImage(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            showToast("Error selecting image: " + e.getMessage());
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        
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
        textAddImage = findViewById(R.id.textAddImage);
        inputName = findViewById(R.id.inputName);
        inputEmail = findViewById(R.id.inputEmail);
        progressBar = findViewById(R.id.progressBar);
        buttonSave = findViewById(R.id.buttonSave);
    }
    
    private void setListeners() {
        // Set up back button
        AppCompatImageView imageBack = findViewById(R.id.imageBack);
        imageBack.setOnClickListener(v -> onBackPressed());
        
        // Set up profile image click
        FrameLayout layoutImage = findViewById(R.id.layoutImage);
        layoutImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
        
        // Also allow clicking directly on the image
        imageProfile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
        
        // Set up save button
        buttonSave.setOnClickListener(v -> {
            if (isValidDetails()) {
                updateProfile();
            }
        });
    }
    
    private void loadUserData() {
        loading(true);
        
        // Get the current user data
        userService.getCurrentUser(new UserFirebaseService.UserProfileListener() {
            @Override
            public void onUserDataLoaded(User user) {
                currentUser = user;
                
                // Populate the form fields with current user data
                inputName.setText(user.getName());
                inputEmail.setText(user.getEmail());
                
                // Load profile image if available
                String imageString = user.getImage();
                if (imageString != null && !imageString.isEmpty()) {
                    byte[] bytes = Base64.decode(imageString, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    imageProfile.setImageBitmap(bitmap);
                    textAddImage.setVisibility(View.GONE);
                    
                    // Store the current image in case user doesn't change it
                    encodedImage = imageString;
                }
                
                loading(false);
            }

            @Override
            public void onFailure(String errorMessage) {
                loading(false);
                showToast("Failed to load user data: " + errorMessage);
                
                // Try to get data from preferences as fallback
                String name = preferenceManager.getString(Constants.KEY_NAME);
                if (name != null) {
                    inputName.setText(name);
                }
                
                String email = preferenceManager.getString(Constants.KEY_EMAIL);
                if (email != null) {
                    inputEmail.setText(email);
                }
                
                String imageString = preferenceManager.getString(Constants.KEY_IMAGE);
                if (imageString != null) {
                    byte[] bytes = Base64.decode(imageString, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    imageProfile.setImageBitmap(bitmap);
                    textAddImage.setVisibility(View.GONE);
                    
                    // Store the current image
                    encodedImage = imageString;
                }
            }
        });
    }
    
    private void updateProfile() {
        loading(true);
        
        // Get updated values
        String name = inputName.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        
        // Check what has changed and only update those fields
        boolean nameChanged = !name.equals(currentUser.getName());
        boolean emailChanged = !email.equals(currentUser.getEmail());
        
        // Only update what has changed
        String updatedName = nameChanged ? name : null;
        String updatedEmail = emailChanged ? email : null;
        
        userService.updateUserProfile(
                updatedName,
                updatedEmail,
                encodedImage,
                new UserFirebaseService.UserProfileUpdateListener() {
                    @Override
                    public void onUpdateSuccess() {
                        loading(false);
                        showToast("Profile updated successfully");
                        
                        // Go back to settings activity
                        onBackPressed();
                    }

                    @Override
                    public void onUpdateFailure(String errorMessage) {
                        loading(false);
                        showToast("Failed to update profile: " + errorMessage);
                    }
                });
    }
    
    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }
    
    private boolean isValidDetails() {
        if (encodedImage == null) {
            showToast("Please select a profile image");
            return false;
        }
        
        if (inputName.getText().toString().trim().isEmpty()) {
            showToast("Enter name");
            return false;
        }
        
        if (inputEmail.getText().toString().trim().isEmpty()) {
            showToast("Enter email");
            return false;
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(inputEmail.getText().toString()).matches()) {
            showToast("Enter valid email");
            return false;
        }
        
        return true;
    }
    
    private void loading(boolean isLoading) {
        if (isLoading) {
            buttonSave.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
            buttonSave.setVisibility(View.VISIBLE);
        }
    }
    
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}