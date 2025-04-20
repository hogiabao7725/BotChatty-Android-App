package com.hgb7725.botchattyapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.hgb7725.botchattyapp.databinding.ActivityResetPasswordBinding;

public class ResetPasswordActivity extends AppCompatActivity {

    private ActivityResetPasswordBinding binding;
    private FirebaseFirestore database;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = FirebaseFirestore.getInstance();
        userEmail = getIntent().getStringExtra("email");

        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.btnResetPassword.setOnClickListener(v -> handlePasswordReset());
    }

    private void handlePasswordReset() {
        String newPassword = binding.inputNewPassword.getText().toString().trim();
        String confirmPassword = binding.inputConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
            showToast("Please fill in all fields");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showToast("Passwords do not match");
            return;
        }

        if (newPassword.length() < 8) {
            showToast("Password must be at least 8 characters");
            return;
        }

        updatePasswordInFirestore(newPassword);
    }

    private void updatePasswordInFirestore(String newPassword) {
        database.collection("users")
                .whereEqualTo("email", userEmail)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        showToast("User not found");
                        return;
                    }

                    String userId = querySnapshot.getDocuments().get(0).getId();
                    database.collection("users")
                            .document(userId)
                            .update("password", newPassword)
                            .addOnSuccessListener(aVoid -> {
                                showToast("Password reset successfully");
                                navigateToSignIn();
                            })
                            .addOnFailureListener(e -> showToast("Failed to update password: " + e.getMessage()));
                })
                .addOnFailureListener(e -> showToast("Error retrieving user: " + e.getMessage()));
    }

    private void navigateToSignIn() {
        Intent intent = new Intent(this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
