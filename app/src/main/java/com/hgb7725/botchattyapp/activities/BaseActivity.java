package com.hgb7725.botchattyapp.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hgb7725.botchattyapp.utilities.Constants;
import com.hgb7725.botchattyapp.utilities.PreferenceManager;

public class BaseActivity extends AppCompatActivity {
    private DocumentReference documentReference;
    private PreferenceManager preferenceManager;
    private static boolean isChangingActivity = false;
    // Key for online status visibility preference
    private static final String KEY_ONLINE_STATUS_VISIBLE = "online_status_visible";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(getApplicationContext());
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID));
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Set the flag when activity is paused
        isChangingActivity = true;

        new android.os.Handler().postDelayed(() -> {
            // If the activity is finishing (not changing to another activity) then set the status to offline
            if (isChangingActivity && (isFinishing() || isTaskRoot())) {
                documentReference.update(Constants.KEY_AVAILABILITY, 0);
            }
            // Reset the flag after a short delay
            isChangingActivity = false;
        }, 300);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Only update status if we're not in the middle of changing activities
        // AND online status visibility is enabled
        if (!isChangingActivity) {
            if (isOnlineStatusVisible()) {
                documentReference.update(Constants.KEY_AVAILABILITY, 1);
            }
        }
        // Reset the flag
        isChangingActivity = false;
    }
    
    /**
     * Checks if the user has enabled online status visibility
     * @return true if online status should be visible to others, false otherwise
     */
    protected boolean isOnlineStatusVisible() {
        // First check if the key exists, if not default to true
        if (!preferenceManager.keyExists(KEY_ONLINE_STATUS_VISIBLE)) {
            return true; // Default is visible
        }
        // Return the saved preference
        return preferenceManager.getBoolean(KEY_ONLINE_STATUS_VISIBLE);
    }
}
