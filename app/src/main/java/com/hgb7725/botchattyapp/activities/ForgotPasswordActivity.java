package com.hgb7725.botchattyapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.hgb7725.botchattyapp.databinding.ActivityForgotPasswordBinding;
import com.hgb7725.botchattyapp.emailJs.EmailJsService;
import com.hgb7725.botchattyapp.utilities.OtpTextWatcher;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;
    private FirebaseFirestore database;

    private String currentOtpCode = null;
    private boolean isVerifying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
    }

    private void init() {
        database = FirebaseFirestore.getInstance();

        disableOtpFields();
        binding.btnVerifyOtp.setEnabled(false);
        binding.btnVerifyOtp.setAlpha(0.3f);

        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        binding.btnSendOtp.setOnClickListener(v -> handleSendOtp());

        binding.btnVerifyOtp.setOnClickListener(v -> verifyOtpCode());
    }

//    private void navigateBack() {
//        startActivity(new Intent(this, SignInActivity.class));
//        finish();
//    }

    private void handleSendOtp() {
        String email = binding.inputEmail.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        checkEmailExists(email);
    }

    private void checkEmailExists(String email) {
        database.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        currentOtpCode = EmailJsService.generateOtp(4);
                        EmailJsService.sendOtpEmail(this, email, currentOtpCode, () -> {
                            Toast.makeText(this, "Send OTP successful", Toast.LENGTH_SHORT).show();

                            binding.inputEmail.setEnabled(false);
                            binding.btnSendOtp.setEnabled(false);
                            binding.btnSendOtp.setAlpha(0.5f);
                            isVerifying = true;

                            enableOtpFields();
                            binding.btnVerifyOtp.setEnabled(true);
                            binding.btnVerifyOtp.setAlpha(1f);
                        });
                    } else {
                        Toast.makeText(this, "Email not registered", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error checking email: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void verifyOtpCode() {
        String enteredCode = binding.otp1.getText().toString().trim()
                + binding.otp2.getText().toString().trim()
                + binding.otp3.getText().toString().trim()
                + binding.otp4.getText().toString().trim();

        if (enteredCode.length() != 4) {
            Toast.makeText(this, "Please enter the full 4-digit code", Toast.LENGTH_SHORT).show();
            return;
        }

        if (enteredCode.equals(currentOtpCode)) {
            String email = binding.inputEmail.getText().toString().trim();
            Intent intent = new Intent(this, ResetPasswordActivity.class);
            intent.putExtra("email", email);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Incorrect code", Toast.LENGTH_SHORT).show();
        }
    }

    private void disableOtpFields() {
        binding.otp1.setEnabled(false);
        binding.otp2.setEnabled(false);
        binding.otp3.setEnabled(false);
        binding.otp4.setEnabled(false);
    }

    private void enableOtpFields() {
        binding.otp1.setEnabled(true);
        binding.otp2.setEnabled(true);
        binding.otp3.setEnabled(true);
        binding.otp4.setEnabled(true);

        setupOtpInputs();
        binding.otp1.requestFocus();
    }

    private void setupOtpInputs() {
        binding.otp1.addTextChangedListener(new OtpTextWatcher(this, binding.otp1, binding.otp2));
        binding.otp2.addTextChangedListener(new OtpTextWatcher(this, binding.otp2, binding.otp3));
        binding.otp3.addTextChangedListener(new OtpTextWatcher(this, binding.otp3, binding.otp4));
        binding.otp4.addTextChangedListener(new OtpTextWatcher(this, binding.otp4, null));
    }
}