package com.hgb7725.botchattyapp.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.hgb7725.botchattyapp.adapters.FileItemAdapter;
import com.hgb7725.botchattyapp.databinding.ActivitySharedFilesBinding;
import com.hgb7725.botchattyapp.firebase.MediaFirebaseService;
import com.hgb7725.botchattyapp.models.FileItem;
import com.hgb7725.botchattyapp.utilities.Constants;
import com.hgb7725.botchattyapp.utilities.PreferenceManager;

import java.util.List;

/**
 * Activity for displaying all shared files between two users
 */
public class SharedFilesActivity extends AppCompatActivity {

    private ActivitySharedFilesBinding binding;
    private String userId;
    private MediaFirebaseService mediaFirebaseService;
    private PreferenceManager preferenceManager;
    private static final String TAG = "SharedFilesActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySharedFilesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());
        mediaFirebaseService = new MediaFirebaseService(this, preferenceManager);
        
        // Get user ID from intent
        userId = getIntent().getStringExtra(Constants.KEY_USER_ID);
        String userName = getIntent().getStringExtra("userName");
        
        if (userId == null) {
            Toast.makeText(this, "User information not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Set title if username is available
        if (userName != null) {
            binding.textTitle.setText(userName + "'s Files");
        }
        
        // Setup back button
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        
        // Setup recycler view
        binding.recyclerViewFiles.setLayoutManager(new LinearLayoutManager(this));
        
        // Load shared files
        loadSharedFiles();
    }
    
    private void loadSharedFiles() {
        showLoading(true);
        
        mediaFirebaseService.fetchSharedFiles(userId, new MediaFirebaseService.FileFetchListener() {
            @Override
            public void onFilesFetched(List<FileItem> fileItems) {
                showLoading(false);
                
                if (fileItems.isEmpty()) {
                    showEmptyState();
                } else {
                    showFiles(fileItems);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error fetching files: " + errorMessage);
                showLoading(false);
                showEmptyState();
                Toast.makeText(SharedFilesActivity.this, "Error loading files", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showFiles(List<FileItem> fileItems) {
        FileItemAdapter adapter = new FileItemAdapter(this, fileItems);
        binding.recyclerViewFiles.setAdapter(adapter);
        
        binding.recyclerViewFiles.setVisibility(View.VISIBLE);
        binding.textEmptyState.setVisibility(View.GONE);
    }
    
    private void showEmptyState() {
        binding.recyclerViewFiles.setVisibility(View.GONE);
        binding.textEmptyState.setVisibility(View.VISIBLE);
    }
    
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.GONE);
        }
    }
}