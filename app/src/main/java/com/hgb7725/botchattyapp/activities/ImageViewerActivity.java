package com.hgb7725.botchattyapp.activities;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.hgb7725.botchattyapp.databinding.ActivityImageViewerBinding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageViewerActivity extends AppCompatActivity {

    private ActivityImageViewerBinding binding;
    private String imageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityImageViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get image URL from intent
        imageUrl = getIntent().getStringExtra("imageUrl");
        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load image with Glide
        loadImage();
        
        // Set up listeners
        setupListeners();
    }

    private void loadImage() {
        Glide.with(this)
                .load(imageUrl)
                .into(binding.photoView);
    }

    private void setupListeners() {
        // Back button
        binding.imageBack.setOnClickListener(v -> finish());

        // Download button
        binding.imageDownload.setOnClickListener(v -> downloadImage());

        // Toggle controls when tapping the photo
        binding.photoView.setOnClickListener(v -> toggleControls());
    }

    private void toggleControls() {
        if (binding.toolbar.getVisibility() == View.VISIBLE) {
            binding.toolbar.setVisibility(View.GONE);
            binding.bottomBar.setVisibility(View.GONE);
        } else {
            binding.toolbar.setVisibility(View.VISIBLE);
            binding.bottomBar.setVisibility(View.VISIBLE);
        }
    }

    private void downloadImage() {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        try {
            // Create unique filename based on timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            String filename = "BotChatty_IMG_" + timestamp + ".jpg";
            
            // Set up download manager
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(imageUrl));
            request.setTitle("Downloading Image");
            request.setDescription("Downloading image from BotChatty");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, 
                    File.separator + "BotChatty" + File.separator + filename);
            
            // Get download service and enqueue request
            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
                Toast.makeText(this, "Downloading image...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Cannot access download service", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        
        binding.progressBar.setVisibility(View.GONE);
    }
}