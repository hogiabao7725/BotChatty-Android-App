package com.hgb7725.botchattyapp.firebase;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.hgb7725.botchattyapp.activities.SignInActivity;
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
    
    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}