package com.hgb7725.botchattyapp.firebase;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.hgb7725.botchattyapp.activities.SignInActivity;
import com.hgb7725.botchattyapp.models.User;
import com.hgb7725.botchattyapp.utilities.Constants;
import com.hgb7725.botchattyapp.utilities.PreferenceManager;

import java.util.HashMap;

public class UserFirebaseService {
    // Initialize variables at declaration to prevent "might not have been initialized" warnings
    private FirebaseFirestore database = FirebaseFirestore.getInstance();
    private PreferenceManager preferenceManager;
    private Context context;
    
    public interface SignOutListener {
        void onSignOutSuccess();
        void onSignOutFailure(String errorMessage);
    }
    
    public interface UserProfileListener {
        void onUserDataLoaded(User user);
        void onFailure(String errorMessage);
    }
    
    public UserFirebaseService(Context context, PreferenceManager preferenceManager) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        if (preferenceManager == null) {
            throw new IllegalArgumentException("PreferenceManager cannot be null");
        }
        
        this.context = context;
        this.preferenceManager = preferenceManager;
    }
    
    public void getToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token) {
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnFailureListener(e -> showToast("Unable to update token"));
    }

    public void signOut(SignOutListener listener) {
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    if (listener != null) {
                        listener.onSignOutSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onSignOutFailure("Unable to Sign Out");
                    }
                });
    }
    
    /**
     * Get user profile data by user ID
     */
    public void getUserById(String userId, UserProfileListener listener) {
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = new User();
                        user.setId(documentSnapshot.getId());
                        user.setName(documentSnapshot.getString(Constants.KEY_NAME));
                        user.setEmail(documentSnapshot.getString(Constants.KEY_EMAIL));
                        user.setImage(documentSnapshot.getString(Constants.KEY_IMAGE));
                        user.setToken(documentSnapshot.getString(Constants.KEY_FCM_TOKEN));
                        
                        listener.onUserDataLoaded(user);
                    } else {
                        listener.onFailure("User not found");
                    }
                })
                .addOnFailureListener(e -> {
                    listener.onFailure("Failed to get user data: " + e.getMessage());
                });
    }
    
    /**
     * Get current logged-in user profile
     */
    public void getCurrentUser(UserProfileListener listener) {
        String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
        if (currentUserId != null) {
            getUserById(currentUserId, listener);
        } else {
            listener.onFailure("No user is currently logged in");
        }
    }
    
    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}