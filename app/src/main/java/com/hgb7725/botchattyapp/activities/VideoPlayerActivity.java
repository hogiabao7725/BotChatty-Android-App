package com.hgb7725.botchattyapp.activities;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hgb7725.botchattyapp.R;
import com.hgb7725.botchattyapp.databinding.ActivityVideoPlayerBinding;
import com.hgb7725.botchattyapp.utilities.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class VideoPlayerActivity extends AppCompatActivity {

    private ActivityVideoPlayerBinding binding;
    private String videoUrl;
    private String originalFileName = null;
    private boolean isDownloading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get video URL from intent
        videoUrl = getIntent().getStringExtra("videoUrl");
        originalFileName = getIntent().getStringExtra("fileName");
        
        if (originalFileName == null) {
            originalFileName = "video_" + System.currentTimeMillis() + ".mp4";
        }

        // Set up video view
        setupVideoPlayer();
        
        // Set up back button
        binding.imageBack.setOnClickListener(v -> finish());
        
        // Set up download button
        binding.imageDownload.setOnClickListener(v -> {
            if (!isDownloading) {
                downloadVideo();
            } else {
                Toast.makeText(this, "Download already in progress", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupVideoPlayer() {
        // Set up media controller
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(binding.videoView);
        binding.videoView.setMediaController(mediaController);
        
        // Set video source
        binding.videoView.setVideoURI(Uri.parse(videoUrl));
        
        // Show loading until video is ready
        binding.progressBar.setVisibility(View.VISIBLE);
        
        // Set video event listeners
        binding.videoView.setOnPreparedListener(mp -> {
            // Hide progress bar when video is ready
            binding.progressBar.setVisibility(View.GONE);
            
            // Set video properties 
            mp.setLooping(false);
            
            // Get video dimensions to maintain proper aspect ratio
            int videoWidth = mp.getVideoWidth();
            int videoHeight = mp.getVideoHeight();
            
            if (videoWidth > 0 && videoHeight > 0) {
                // Update the container's aspect ratio based on actual video dimensions
                FrameLayout container = binding.videoContainer;
                float videoRatio = (float) videoWidth / videoHeight;
                
                // Update constraint to match actual video aspect ratio
                binding.videoContainer.post(() -> {
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params = 
                            (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) container.getLayoutParams();
                    params.dimensionRatio = "H," + videoWidth + ":" + videoHeight;
                    container.setLayoutParams(params);
                });
            }
            
            // Start playing video
            binding.videoView.start();
            
            // Show video duration in formatted time
            int duration = mp.getDuration();
            binding.textVideoDuration.setText(formatDuration(duration));
        });
        
        binding.videoView.setOnErrorListener((mp, what, extra) -> {
            binding.progressBar.setVisibility(View.GONE);
            Toast.makeText(VideoPlayerActivity.this, 
                    "Error playing video", Toast.LENGTH_SHORT).show();
            return true;
        });
        
        binding.videoView.setOnCompletionListener(mp -> {
            // Video playback completed
        });
        
        // Request focus for the video view
        binding.videoView.requestFocus();
    }
    
    private String formatDuration(int durationMs) {
        int seconds = (durationMs / 1000) % 60;
        int minutes = (durationMs / (1000 * 60)) % 60;
        int hours = durationMs / (1000 * 60 * 60);
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
    
    private void downloadVideo() {
        // Show progress during download
        binding.progressBar.setVisibility(View.VISIBLE);
        isDownloading = true;
        
        // Download in background thread
        new Thread(() -> {
            boolean success = false;
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    success = downloadVideoApi29AndAbove();
                } else {
                    success = downloadVideoLegacy();
                }
            } catch (Exception e) {
                Log.e("VideoPlayerActivity", "Download failed", e);
                success = false;
            }
            
            final boolean finalSuccess = success;
            
            // Update UI on main thread
            runOnUiThread(() -> {
                binding.progressBar.setVisibility(View.GONE);
                isDownloading = false;
                
                if (finalSuccess) {
                    Toast.makeText(VideoPlayerActivity.this, 
                            "Video saved to gallery", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(VideoPlayerActivity.this, 
                            "Failed to save video", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
    
    private boolean downloadVideoApi29AndAbove() {
        ContentResolver resolver = getContentResolver();
        ContentValues contentValues = new ContentValues();
        
        // Set file metadata
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, originalFileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, 
                Environment.DIRECTORY_MOVIES + File.separator + "BotChatty");
        
        // Create new file in MediaStore
        Uri uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
        if (uri == null) return false;
        
        try {
            // Open streams
            OutputStream outputStream = resolver.openOutputStream(uri);
            if (outputStream == null) return false;
            
            URL url = new URL(videoUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }
            
            InputStream inputStream = connection.getInputStream();
            
            // Copy file
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            
            outputStream.close();
            inputStream.close();
            
            return true;
        } catch (IOException e) {
            Log.e("VideoPlayerActivity", "Download failed", e);
            // Delete failed file
            resolver.delete(uri, null, null);
            return false;
        }
    }
    
    private boolean downloadVideoLegacy() {
        try {
            // Create directory if it doesn't exist
            File directory = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MOVIES), "BotChatty");
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    return false;
                }
            }
            
            // Create file
            File file = new File(directory, originalFileName);
            
            // Download file from URL
            URL url = new URL(videoUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }
            
            FileUtils.downloadFile(connection.getInputStream(), file);
            
            // Make it visible in gallery
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(file));
            sendBroadcast(mediaScanIntent);
            
            return true;
        } catch (IOException e) {
            Log.e("VideoPlayerActivity", "Download failed", e);
            return false;
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Pause video when activity is paused
        if (binding.videoView.isPlaying()) {
            binding.videoView.pause();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release resources
        binding.videoView.stopPlayback();
    }
}