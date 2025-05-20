package com.hgb7725.botchattyapp.application;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hgb7725.botchattyapp.utilities.Constants;
import com.hgb7725.botchattyapp.utilities.PreferenceManager;
import com.hgb7725.botchattyapp.services.CallManager;

/**
 * Custom Application class to monitor app lifecycle and handle online status updates
 */
public class BotChattyApplication extends Application implements LifecycleObserver {
    
    private static final String TAG = "BotChattyApplication";
    private int activityReferences = 0;
    private boolean isAppBackground = false;
    private PreferenceManager preferenceManager;
    private DocumentReference userDocumentReference;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new AppLifecycleCallbacks());
        // Initialize LifecycleObserver to detect app foreground/background state
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        
        // Initialize preference manager
        preferenceManager = new PreferenceManager(getApplicationContext());
        // Initialize global CallManager
        CallManager.init(this);
    }

    /**
     * Initialize Firebase document reference if user is logged in
     */
    private void initializeUserDocReference() {
        // Only initialize if user is signed in and document reference is null
        if (preferenceManager != null && 
            preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN) && 
            userDocumentReference == null) {
            
            String userId = preferenceManager.getString(Constants.KEY_USER_ID);
            if (userId != null && !userId.isEmpty()) {
                FirebaseFirestore database = FirebaseFirestore.getInstance();
                userDocumentReference = database.collection(Constants.KEY_COLLECTION_USERS)
                        .document(userId);
            }
        }
    }

    /**
     * Called when the app goes to background
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onAppBackgrounded() {
        isAppBackground = true;
        Log.d(TAG, "App went to background");
        // Set user status to offline when app goes to background
        updateUserAvailability(Constants.AVAILABILITY_OFFLINE);
    }

    /**
     * Called when the app comes to foreground
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onAppForegrounded() {
        isAppBackground = false;
        Log.d(TAG, "App came to foreground");
        // Set user status to online when app comes to foreground
        updateUserAvailability(Constants.AVAILABILITY_ONLINE);
    }

    /**
     * Update user's availability status in Firestore
     *
     * @param availabilityStatus AVAILABILITY_ONLINE or AVAILABILITY_OFFLINE
     */
    private void updateUserAvailability(int availabilityStatus) {
        initializeUserDocReference();
        
        if (userDocumentReference != null && 
            preferenceManager != null && 
            preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            
            // Only update if online status is visible to others
            if (!preferenceManager.keyExists(Constants.KEY_ONLINE_STATUS_VISIBLE) || 
                preferenceManager.getBoolean(Constants.KEY_ONLINE_STATUS_VISIBLE)) {
                
                Log.d(TAG, "Updating user availability to: " + availabilityStatus);
                userDocumentReference.update(Constants.KEY_AVAILABILITY, availabilityStatus)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "User availability status updated successfully");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update user availability status", e);
                    });
            } else {
                Log.d(TAG, "Online status visibility is disabled, not updating status");
            }
        } else {
            Log.d(TAG, "User document reference not available or user not signed in");
        }
    }

    /**
     * Activity Lifecycle callbacks to track when app has no activities in foreground
     */
    private class AppLifecycleCallbacks implements ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
            if (activityReferences == 0) {
                // App went to foreground
                Log.d(TAG, "App went to foreground (activity started)");
                updateUserAvailability(Constants.AVAILABILITY_ONLINE);
            }
            activityReferences++;
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
            activityReferences--;
            if (activityReferences == 0) {
                // App went to background
                Log.d(TAG, "App went to background (activity stopped)");
                updateUserAvailability(Constants.AVAILABILITY_OFFLINE);
            }
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
        }
    }
}