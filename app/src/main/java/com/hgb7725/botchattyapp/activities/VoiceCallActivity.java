package com.hgb7725.botchattyapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.hgb7725.botchattyapp.R;
import com.hgb7725.botchattyapp.databinding.ActivityVoiceCallBinding;
import com.hgb7725.botchattyapp.models.User;
import com.hgb7725.botchattyapp.utilities.Constants;
import com.hgb7725.botchattyapp.utilities.PreferenceManager;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;

public class VoiceCallActivity extends BaseActivity {

    private ActivityVoiceCallBinding binding;
    private PreferenceManager preferenceManager;
    private RtcEngine agoraEngine;
    private User receiverUser;
    private String channelName;
    private boolean isMuted = false;
    private boolean isSpeakerOn = true;
    private static final String TAG = "VoiceCallActivity";
    
    // Agora App ID - Replace with your own App ID from Agora Console
    private static final String AGORA_APP_ID = "8271e3645db7490ab53b74ddf137d3e9";
    
    // Permission request
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    setupVoiceSDKEngine();
                    joinChannel();
                } else {
                    showToast("Microphone permission is required for voice calls");
                    finish();
                }
            });

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onUserJoined(int uid, int elapsed) {
            // Called when a remote user joins the channel
            runOnUiThread(() -> {
                Log.d(TAG, "Remote user joined: " + uid);
                binding.callStatus.setText("Connected");
                binding.progressBar.setVisibility(View.GONE);
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            // Called when a remote user leaves the channel
            runOnUiThread(() -> {
                Log.d(TAG, "Remote user left: " + uid + ", reason: " + reason);
                endCall();
            });
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            // Called when local user successfully joins the channel
            runOnUiThread(() -> {
                Log.d(TAG, "Local user joined channel: " + channel + ", uid: " + uid);
                binding.callStatus.setText("Waiting for recipient to join...");
            });
        }
        
        @Override
        public void onError(int err) {
            runOnUiThread(() -> {
                Log.e(TAG, "Agora error: " + err);
                showToast("Error code: " + err);
            });
        }
        
        @Override
        public void onAudioVolumeIndication(AudioVolumeInfo[] speakers, int totalVolume) {
            // This callback indicates audio is working and being detected
            // Log audio volume for debugging
            for (AudioVolumeInfo info : speakers) {
                Log.d(TAG, "Audio volume - uid: " + info.uid + ", volume: " + info.volume);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVoiceCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        preferenceManager = new PreferenceManager(getApplicationContext());
        
        // Get receiver user data from intent
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        
        // Set up UI
        binding.textName.setText(receiverUser.getName());
        
        // Generate a unique channel name using both user IDs
        String senderId = preferenceManager.getString(Constants.KEY_USER_ID);
        String receiverId = receiverUser.getId();
        channelName = senderId.compareTo(receiverId) < 0 ? 
                      senderId + "_" + receiverId : 
                      receiverId + "_" + senderId;
        
        // Set listeners
        setListeners();
        
        // Check and request permissions
        checkPermissions();
    }
    
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted, proceed with setup
            setupVoiceSDKEngine();
            joinChannel();
        } else {
            // Request the permission
            if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                // Show dialog explaining why we need this permission
                new AlertDialog.Builder(this)
                        .setTitle("Microphone Permission Required")
                        .setMessage("BotChatty needs access to your microphone to make voice calls.")
                        .setPositiveButton("OK", (dialog, which) -> 
                                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO))
                        .setNegativeButton("Cancel", (dialog, which) -> finish())
                        .create().show();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            }
        }
    }
    
    private void setListeners() {
        binding.imageEndCall.setOnClickListener(v -> endCall());
        
        binding.imageMuteUnmute.setOnClickListener(v -> {
            isMuted = !isMuted;
            agoraEngine.muteLocalAudioStream(isMuted);
            binding.imageMuteUnmute.setImageResource(
                    isMuted ? R.drawable.ic_mic_off : R.drawable.ic_mic);
            showToast(isMuted ? "Microphone muted" : "Microphone unmuted");
        });
        
        binding.imageSpeaker.setOnClickListener(v -> {
            isSpeakerOn = !isSpeakerOn;
            agoraEngine.setEnableSpeakerphone(isSpeakerOn);
            binding.imageSpeaker.setImageResource(
                    isSpeakerOn ? R.drawable.ic_speaker_on : R.drawable.ic_speaker_off);
            showToast(isSpeakerOn ? "Speaker on" : "Speaker off");
        });
    }
    
    private void setupVoiceSDKEngine() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getBaseContext();
            config.mAppId = AGORA_APP_ID;
            config.mEventHandler = mRtcEventHandler;
            agoraEngine = RtcEngine.create(config);
            
            // Enable audio volume indication for debugging
            agoraEngine.enableAudioVolumeIndication(500, 3, true);
            
            // Enable echo cancellation and noise suppression
            agoraEngine.setParameters("{"
                    + "\"che.audio.enable.aec\": true,"
                    + "\"che.audio.enable.ns\": true"
                    + "}");
                    
            Log.d(TAG, "Agora Voice SDK initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Agora Voice SDK: " + e.getMessage());
            showToast("Error initializing voice call: " + e.getMessage());
            finish();
        }
    }
    
    private void joinChannel() {
        if (agoraEngine == null) {
            Log.e(TAG, "Agora engine is null when trying to join channel");
            showToast("Voice call initialization failed. Please try again.");
            finish();
            return;
        }
        
        try {
            // Configure the channel for audio
            ChannelMediaOptions options = new ChannelMediaOptions();
            options.autoSubscribeAudio = true;
            options.publishMicrophoneTrack = true; // Ensure microphone is published
            options.enableAudioRecordingOrPlayout = true; // Enable audio recording and playback
            options.clientRoleType = io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER;
            options.channelProfile = io.agora.rtc2.Constants.CHANNEL_PROFILE_COMMUNICATION;
            
            // Enable speaker by default
            agoraEngine.setDefaultAudioRoutetoSpeakerphone(isSpeakerOn);
            agoraEngine.setEnableSpeakerphone(isSpeakerOn);
            
            // Set audio profile for better quality
            agoraEngine.setAudioProfile(
                    io.agora.rtc2.Constants.AUDIO_PROFILE_DEFAULT,
                    io.agora.rtc2.Constants.AUDIO_SCENARIO_CHATROOM);
            
            // Generate temp token (in production, should be generated server-side)
            String token = null; // Set to null for testing environments without token security
            
            // Join the channel
            int uid = (int) (System.currentTimeMillis() % 100000);
            int result = agoraEngine.joinChannel(token, channelName, uid, options);
            
            if (result != 0) {
                Log.e(TAG, "Join channel failed: " + result);
                showToast("Failed to join call: Error " + result);
                finish();
                return;
            }
            
            Log.d(TAG, "Joining channel: " + channelName);
            
            // Show calling UI
            binding.callStatus.setText("Calling...");
            binding.progressBar.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e(TAG, "Error joining channel: " + e.getMessage());
            showToast("Error joining call: " + e.getMessage());
            finish();
        }
    }
    
    private void endCall() {
        binding.callStatus.setText("Call ended");
        binding.progressBar.setVisibility(View.GONE);
        
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