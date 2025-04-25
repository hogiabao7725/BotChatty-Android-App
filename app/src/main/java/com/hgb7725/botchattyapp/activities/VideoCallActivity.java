package com.hgb7725.botchattyapp.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.hgb7725.botchattyapp.R;
import com.hgb7725.botchattyapp.databinding.ActivityVideoCallBinding;
import com.hgb7725.botchattyapp.models.User;
import com.hgb7725.botchattyapp.utilities.Constants;
import com.hgb7725.botchattyapp.utilities.PreferenceManager;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;

public class VideoCallActivity extends BaseActivity {

    private ActivityVideoCallBinding binding;
    private PreferenceManager preferenceManager;
    private RtcEngine agoraEngine;
    private User receiverUser;
    private String channelName;
    private boolean isMuted = false;
    private boolean isCameraOn = true;
    private boolean isFrontCamera = true;
    private static final String TAG = "VideoCallActivity";
    
    // Agora App ID - Replace with your own App ID from Agora Console
    private static final String AGORA_APP_ID = "your_agora_app_id";
    
    // Permissions
    private static final int PERMISSION_REQUEST_ID = 22;
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onUserJoined(int uid, int elapsed) {
            // Called when a remote user joins the channel
            runOnUiThread(() -> {
                binding.callStatus.setText("Connected");
                binding.callStatus.setVisibility(View.GONE);
                binding.progressBar.setVisibility(View.GONE);
                setupRemoteVideo(uid);
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            // Called when a remote user leaves the channel
            runOnUiThread(() -> {
                binding.callStatus.setText("Call ended");
                binding.callStatus.setVisibility(View.VISIBLE);
                binding.remoteVideoView.setVisibility(View.GONE);
                endCall();
            });
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            // Called when local user successfully joins the channel
            runOnUiThread(() -> {
                binding.callStatus.setText("Waiting for recipient to join...");
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        preferenceManager = new PreferenceManager(getApplicationContext());
        
        // Get receiver user data from intent
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        
        // Set up UI
        binding.textName.setText(receiverUser.getName());
        
        // Check permissions
        if (checkPermissions()) {
            initializeAndJoinChannel();
        }
    }
    
    private boolean checkPermissions() {
        boolean hasAudioPermission = ContextCompat.checkSelfPermission(
                this, REQUESTED_PERMISSIONS[0]) == PackageManager.PERMISSION_GRANTED;
        boolean hasCameraPermission = ContextCompat.checkSelfPermission(
                this, REQUESTED_PERMISSIONS[1]) == PackageManager.PERMISSION_GRANTED;
                
        if (hasAudioPermission && hasCameraPermission) {
            return true;
        }
        
        ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQUEST_ID);
        return false;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_ID) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            
            if (allPermissionsGranted) {
                initializeAndJoinChannel();
            } else {
                showToast("Permissions denied. Cannot make video call.");
                finish();
            }
        }
    }
    
    private void initializeAndJoinChannel() {
        // Generate a unique channel name using both user IDs
        String senderId = preferenceManager.getString(Constants.KEY_USER_ID);
        String receiverId = receiverUser.getId();
        channelName = senderId.compareTo(receiverId) < 0 ? 
                      senderId + "_" + receiverId + "_video" : 
                      receiverId + "_" + senderId + "_video";
        
        // Setup Video SDK Engine
        setupVideoSDKEngine();
        
        // Set listeners
        setListeners();
        
        // Join channel to start the call
        joinChannel();
    }
    
    private void setListeners() {
        binding.imageEndCall.setOnClickListener(v -> endCall());
        
        binding.imageMuteUnmute.setOnClickListener(v -> {
            isMuted = !isMuted;
            agoraEngine.muteLocalAudioStream(isMuted);
            binding.imageMuteUnmute.setImageResource(
                    isMuted ? R.drawable.ic_mic_off : R.drawable.ic_mic);
        });
        
        binding.imageCamera.setOnClickListener(v -> {
            isCameraOn = !isCameraOn;
            agoraEngine.muteLocalVideoStream(!isCameraOn);
            binding.imageCamera.setImageResource(
                    isCameraOn ? R.drawable.ic_video_on : R.drawable.ic_video_off);
            binding.localVideoView.setVisibility(isCameraOn ? View.VISIBLE : View.INVISIBLE);
        });
        
        binding.imageSwitchCamera.setOnClickListener(v -> {
            agoraEngine.switchCamera();
            isFrontCamera = !isFrontCamera;
        });
    }
    
    private void setupVideoSDKEngine() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getBaseContext();
            config.mAppId = AGORA_APP_ID;
            config.mEventHandler = mRtcEventHandler;
            agoraEngine = RtcEngine.create(config);
            
            // Enable video
            agoraEngine.enableVideo();
            
            // Set video configuration
            VideoEncoderConfiguration videoConfig = new VideoEncoderConfiguration(
                    VideoEncoderConfiguration.VD_640x360,
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
            );
            agoraEngine.setVideoEncoderConfiguration(videoConfig);
            
            // Setup local video view
            setupLocalVideo();
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Agora Video SDK: " + e.getMessage());
            showToast("Error initializing video call. Please try again.");
            finish();
        }
    }
    
    private void setupLocalVideo() {
        // Create a SurfaceView object
        SurfaceView surfaceView = new SurfaceView(getBaseContext());
        
        // Add the view to the layout
        binding.localVideoView.addView(surfaceView);
        
        // Setup the video canvas
        VideoCanvas localCanvas = new VideoCanvas(
                surfaceView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                0  // Local user ID is always 0
        );
        agoraEngine.setupLocalVideo(localCanvas);
    }
    
    private void setupRemoteVideo(int uid) {
        // Check if the container already has a SurfaceView
        if (binding.remoteVideoView.getChildCount() > 0) {
            binding.remoteVideoView.removeAllViews();
        }
        
        // Create a SurfaceView object for remote view
        SurfaceView surfaceView = new SurfaceView(getBaseContext());
        binding.remoteVideoView.addView(surfaceView);
        
        // Setup the video canvas
        VideoCanvas remoteCanvas = new VideoCanvas(
                surfaceView,
                VideoCanvas.RENDER_MODE_FIT,
                uid
        );
        agoraEngine.setupRemoteVideo(remoteCanvas);
    }
    
    private void joinChannel() {
        // Configure the channel for video
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.autoSubscribeVideo = true;
        options.autoSubscribeAudio = true;
        options.clientRoleType = io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER;
        options.channelProfile = io.agora.rtc2.Constants.CHANNEL_PROFILE_COMMUNICATION;
        
        // Generate temp token (in production, should be generated server-side)
        String token = null; // Set to null for testing environments without token security
        
        // Join the channel
        int uid = (int) (System.currentTimeMillis() % 100000);
        agoraEngine.joinChannel(token, channelName, uid, options);
        
        // Show calling UI
        binding.callStatus.setText("Calling...");
        binding.progressBar.setVisibility(View.VISIBLE);
    }
    
    private void endCall() {
        if (agoraEngine != null) {
            agoraEngine.leaveChannel();
        }
        
        // Return to previous screen after a short delay
        binding.getRoot().postDelayed(() -> {
            finish();
        }, 1000);
    }
    
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (agoraEngine != null) {
            agoraEngine.leaveChannel();
            // Destroy the engine in a sub-thread to avoid blocking the main thread
            new Thread(() -> {
                RtcEngine.destroy();
                agoraEngine = null;
            }).start();
        }
    }
}