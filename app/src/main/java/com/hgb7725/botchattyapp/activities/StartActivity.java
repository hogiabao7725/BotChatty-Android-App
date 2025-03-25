package com.hgb7725.botchattyapp.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.hgb7725.botchattyapp.databinding.ActivityStartBinding;
import com.hgb7725.botchattyapp.utilities.Constants;
import com.hgb7725.botchattyapp.utilities.PreferenceManager;

public class StartActivity extends AppCompatActivity {

    private ActivityStartBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if user is already signed in
        PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());
        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            // User is signed in, go directly to MainActivity
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        binding = ActivityStartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
    }

    private void setListeners() {
        binding.buttonStart.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), SignInActivity.class);
            startActivity(intent);
            finish();
        });
    }
} 