package com.hgb7725.botchattyapp.firebase;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
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
    private static final String TAG = "UserFirebaseService";
    
    public interface SignOutListener {
        void onSignOutSuccess();
        void onSignOutFailure(String errorMessage);
    }
    
    public interface UserProfileListener {
        void onUserDataLoaded(User user);
        void onFailure(String errorMessage);
    }
    
    public interface OnlineStatusListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }
    
    public interface UserProfileUpdateListener {
        void onUpdateSuccess();
        void onUpdateFailure(String errorMessage);
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

    /**
     * Signs out the user and updates offline status
     * @param listener Interface callback to notify results
     */
    public void signOut(SignOutListener listener) {
        try {
            String userId = preferenceManager.getString(Constants.KEY_USER_ID);
            if (userId == null) {
                // If unable to get user ID, clear preferences and proceed to sign-in screen
                preferenceManager.clear();
                if (listener != null) {
                    listener.onSignOutSuccess();
                }
                return;
            }
            
            DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
                    .document(userId);
            HashMap<String, Object> updates = new HashMap<>();
            // Set user as offline when signing out
            updates.put(Constants.KEY_AVAILABILITY, Constants.AVAILABILITY_OFFLINE);
            // Delete FCM token
            updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
            
            documentReference.update(updates)
                    .addOnSuccessListener(unused -> {
                        preferenceManager.clear();
                        if (listener != null) {
                            listener.onSignOutSuccess();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Firebase error signing out: " + e.getMessage());
                        // Even if Firebase update fails, still sign out locally
                        preferenceManager.clear();
                        if (listener != null) {
                            listener.onSignOutFailure("Unable to update status but signed out locally");
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in signOut: " + e.getMessage());
            // If any exception occurs, still try to sign out
            preferenceManager.clear();
            if (listener != null) {
                listener.onSignOutSuccess();
            }
        }
    }
    
    /**
     * Updates the visibility of user's online status
     * @param isVisible Whether to display online status
     * @param listener Interface callback to notify results
     */
    public void updateOnlineStatusVisibility(boolean isVisible, OnlineStatusListener listener) {
        try {
            String userId = preferenceManager.getString(Constants.KEY_USER_ID);
            if (userId == null) {
                if (listener != null) {
                    listener.onFailure("User ID is null when updating online status");
                }
                return;
            }
            
            DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
                    .document(userId);
            HashMap<String, Object> updates = new HashMap<>();
            // Update display settings
            updates.put(Constants.KEY_ONLINE_STATUS_VISIBLE, isVisible);
            
            // Also update availability status based on display settings
            // If display is turned off, set availability to 0 (offline)
            // If display is turned on, set availability to 1 (online)
            updates.put(Constants.KEY_AVAILABILITY, isVisible ? Constants.AVAILABILITY_ONLINE : Constants.AVAILABILITY_OFFLINE);
            
            documentReference.update(updates)
                    .addOnSuccessListener(aVoid -> {
                        if (listener != null) {
                            listener.onSuccess();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Firebase error updating online status: " + e.getMessage());
                        if (listener != null) {
                            listener.onFailure("Unable to update online status visibility");
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in updateOnlineStatusVisibility: " + e.getMessage());
            if (listener != null) {
                listener.onFailure("An error occurred: " + e.getMessage());
            }
        }
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
    
    /**
     * Deletes user account and all related data
     * @param listener Interface callback to notify results
     */
    public void deleteAccount(SignOutListener listener) {
        try {
            String userId = preferenceManager.getString(Constants.KEY_USER_ID);
            if (userId == null) {
                if (listener != null) {
                    listener.onSignOutFailure("Cannot delete account: User ID is null");
                }
                return;
            }
            
            // Delete all chat messages where user is sender or receiver
            database.collection(Constants.KEY_COLLECTION_CHAT)
                    .whereEqualTo(Constants.KEY_SENDER_ID, userId)
                    .get()
                    .addOnSuccessListener(senderChats -> {
                        // Delete each message sent by the user
                        for (DocumentSnapshot doc : senderChats.getDocuments()) {
                            doc.getReference().delete();
                        }
                        
                        // Delete all messages received by user
                        database.collection(Constants.KEY_COLLECTION_CHAT)
                                .whereEqualTo(Constants.KEY_RECEIVER_ID, userId)
                                .get()
                                .addOnSuccessListener(receiverChats -> {
                                    // Delete each message received by the user
                                    for (DocumentSnapshot doc : receiverChats.getDocuments()) {
                                        doc.getReference().delete();
                                    }
                                    
                                    // Delete conversations where user is sender
                                    database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                                            .whereEqualTo(Constants.KEY_SENDER_ID, userId)
                                            .get()
                                            .addOnSuccessListener(senderConversations -> {
                                                // Delete each conversation started by the user
                                                for (DocumentSnapshot doc : senderConversations.getDocuments()) {
                                                    doc.getReference().delete();
                                                }
                                                
                                                // Delete conversations where user is receiver
                                                database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                                                        .whereEqualTo(Constants.KEY_RECEIVER_ID, userId)
                                                        .get()
                                                        .addOnSuccessListener(receiverConversations -> {
                                                            // Delete each conversation where user is the receiver
                                                            for (DocumentSnapshot doc : receiverConversations.getDocuments()) {
                                                                doc.getReference().delete();
                                                            }
                                                            
                                                            // Finally, delete the user account
                                                            database.collection(Constants.KEY_COLLECTION_USERS)
                                                                    .document(userId)
                                                                    .delete()
                                                                    .addOnSuccessListener(aVoid -> {
                                                                        // Clear local data and notify success
                                                                        preferenceManager.clear();
                                                                        if (listener != null) {
                                                                            listener.onSignOutSuccess();
                                                                        }
                                                                    })
                                                                    .addOnFailureListener(e -> {
                                                                        Log.e(TAG, "Error deleting user: " + e.getMessage());
                                                                        if (listener != null) {
                                                                            listener.onSignOutFailure("Failed to delete account: " + e.getMessage());
                                                                        }
                                                                    });
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Log.e(TAG, "Error deleting receiver conversations: " + e.getMessage());
                                                            if (listener != null) {
                                                                listener.onSignOutFailure("Failed to clean up conversations: " + e.getMessage());
                                                            }
                                                        });
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Error deleting sender conversations: " + e.getMessage());
                                                if (listener != null) {
                                                    listener.onSignOutFailure("Failed to clean up conversations: " + e.getMessage());
                                                }
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error deleting receiver messages: " + e.getMessage());
                                    if (listener != null) {
                                        listener.onSignOutFailure("Failed to clean up messages: " + e.getMessage());
                                    }
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error deleting sender messages: " + e.getMessage());
                        if (listener != null) {
                            listener.onSignOutFailure("Failed to clean up messages: " + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in deleteAccount: " + e.getMessage());
            if (listener != null) {
                listener.onSignOutFailure("Error while deleting account: " + e.getMessage());
            }
        }
    }
    
    /**
     * Updates user profile information
     * @param name Updated user name (optional, can be null)
     * @param email Updated email (optional, can be null)
     * @param image Updated profile image Base64 string (optional, can be null)
     * @param listener Interface callback to notify results
     */
    public void updateUserProfile(String name, String email, String image, UserProfileUpdateListener listener) {
        try {
            String userId = preferenceManager.getString(Constants.KEY_USER_ID);
            if (userId == null) {
                if (listener != null) {
                    listener.onUpdateFailure("User ID is null when updating profile");
                }
                return;
            }
            
            DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
                    .document(userId);
            HashMap<String, Object> updates = new HashMap<>();
            
            // Only update fields that have new values
            if (name != null && !name.trim().isEmpty()) {
                updates.put(Constants.KEY_NAME, name);
                // Update in preferences too
                preferenceManager.putString(Constants.KEY_NAME, name);
            }
            
            if (email != null && !email.trim().isEmpty()) {
                updates.put(Constants.KEY_EMAIL, email);
                // Update in preferences too
                preferenceManager.putString(Constants.KEY_EMAIL, email);
            }
            
            if (image != null && !image.trim().isEmpty()) {
                updates.put(Constants.KEY_IMAGE, image);
                // Update in preferences too
                preferenceManager.putString(Constants.KEY_IMAGE, image);
            }
            
            if (updates.isEmpty()) {
                if (listener != null) {
                    listener.onUpdateFailure("No changes to update");
                }
                return;
            }
            
            documentReference.update(updates)
                    .addOnSuccessListener(aVoid -> {
                        if (listener != null) {
                            listener.onUpdateSuccess();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Firebase error updating profile: " + e.getMessage());
                        if (listener != null) {
                            listener.onUpdateFailure("Unable to update profile: " + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in updateUserProfile: " + e.getMessage());
            if (listener != null) {
                listener.onUpdateFailure("An error occurred: " + e.getMessage());
            }
        }
    }
    
    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}