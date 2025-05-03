package com.hgb7725.botchattyapp.firebase;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.hgb7725.botchattyapp.models.UserRelationship;
import com.hgb7725.botchattyapp.utilities.Constants;
import com.hgb7725.botchattyapp.utilities.PreferenceManager;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Service to manage relationship between users, including block/mute functionality.
 */
public class UserRelationshipService {

    private static final String TAG = "UserRelationshipService";
    private final FirebaseFirestore database;
    private final PreferenceManager preferenceManager;
    private final Context context;

    /**
     * Interface to handle relationship status results
     */
    public interface RelationshipStatusListener {
        void onStatusLoaded(boolean isBlocked, boolean isMuted);
        void onFailure(String errorMessage);
    }

    /**
     * Interface to handle operation results
     */
    public interface RelationshipOperationListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    /**
     * Interface to handle nickname results
     */
    public interface NicknameListener {
        void onNicknameLoaded(String nickname);
        void onFailure(String errorMessage);
    }

    /**
     * Constructor for UserRelationshipService
     * @param context Application context
     * @param preferenceManager Preference manager to access user preferences
     */
    public UserRelationshipService(Context context, PreferenceManager preferenceManager) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        if (preferenceManager == null) {
            throw new IllegalArgumentException("PreferenceManager cannot be null");
        }

        this.context = context;
        this.preferenceManager = preferenceManager;
        this.database = FirebaseFirestore.getInstance();
    }

    /**
     * Get relationship status between current user and another user
     * @param otherUserId ID of the other user
     * @param listener Callback listener for the result
     */
    public void getRelationshipStatus(String otherUserId, RelationshipStatusListener listener) {
        String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
        
        if (currentUserId == null || currentUserId.isEmpty()) {
            listener.onFailure("Current user not available");
            return;
        }

        database.collection(Constants.KEY_COLLECTION_USER_RELATIONSHIPS)
                .whereEqualTo(Constants.KEY_USER_ID, currentUserId)
                .whereEqualTo(Constants.KEY_OTHER_USER_ID, otherUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Relationship exists
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        UserRelationship relationship = document.toObject(UserRelationship.class);
                        if (relationship != null) {
                            listener.onStatusLoaded(relationship.isBlocked(), relationship.isMuted());
                        } else {
                            // Fallback to direct field access if object conversion fails
                            Boolean blocked = document.getBoolean(Constants.KEY_BLOCKED);
                            Boolean muted = document.getBoolean(Constants.KEY_MUTED);
                            listener.onStatusLoaded(blocked != null && blocked, muted != null && muted);
                        }
                    } else {
                        // No relationship exists yet
                        listener.onStatusLoaded(false, false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting relationship status: " + e.getMessage());
                    listener.onFailure("Failed to get relationship status: " + e.getMessage());
                });
    }

    /**
     * Update block status between current user and another user
     * @param otherUserId ID of the other user
     * @param blocked Whether to block or unblock
     * @param listener Callback listener for the operation result
     */
    public void updateBlockStatus(String otherUserId, boolean blocked, RelationshipOperationListener listener) {
        updateRelationship(otherUserId, blocked, null, null, listener);
    }

    /**
     * Update mute status between current user and another user
     * @param otherUserId ID of the other user
     * @param muted Whether to mute or unmute
     * @param listener Callback listener for the operation result
     */
    public void updateMuteStatus(String otherUserId, boolean muted, RelationshipOperationListener listener) {
        updateRelationship(otherUserId, null, muted, null, listener);
    }

    /**
     * Check if current user is blocked by another user
     * @param otherUserId ID of the other user
     * @param listener Callback listener for the result
     */
    public void isBlockedByUser(String otherUserId, RelationshipStatusListener listener) {
        String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
        
        if (currentUserId == null || currentUserId.isEmpty()) {
            listener.onFailure("Current user not available");
            return;
        }

        database.collection(Constants.KEY_COLLECTION_USER_RELATIONSHIPS)
                .whereEqualTo(Constants.KEY_USER_ID, otherUserId)
                .whereEqualTo(Constants.KEY_OTHER_USER_ID, currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Relationship exists
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        UserRelationship relationship = document.toObject(UserRelationship.class);
                        if (relationship != null) {
                            listener.onStatusLoaded(relationship.isBlocked(), relationship.isMuted());
                        } else {
                            // Fallback to direct field access if object conversion fails
                            Boolean blocked = document.getBoolean(Constants.KEY_BLOCKED);
                            Boolean muted = document.getBoolean(Constants.KEY_MUTED);
                            listener.onStatusLoaded(blocked != null && blocked, muted != null && muted);
                        }
                    } else {
                        // No relationship exists yet
                        listener.onStatusLoaded(false, false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking if blocked by user: " + e.getMessage());
                    listener.onFailure("Failed to check block status: " + e.getMessage());
                });
    }

    /**
     * Update nickname for a user
     * @param otherUserId ID of the other user
     * @param nickname The nickname to set (null to remove)
     * @param listener Callback listener for the operation result
     */
    public void updateNickname(String otherUserId, String nickname, RelationshipOperationListener listener) {
        updateRelationship(otherUserId, null, null, nickname, listener);
    }

    /**
     * Get nickname for a user
     * @param otherUserId ID of the other user
     * @param listener Callback listener for the result
     */
    public void getNickname(String otherUserId, NicknameListener listener) {
        String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
        
        if (currentUserId == null || currentUserId.isEmpty()) {
            listener.onFailure("Current user not available");
            return;
        }

        database.collection(Constants.KEY_COLLECTION_USER_RELATIONSHIPS)
                .whereEqualTo(Constants.KEY_USER_ID, currentUserId)
                .whereEqualTo(Constants.KEY_OTHER_USER_ID, otherUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        String nickname = document.getString(Constants.KEY_NICKNAME);
                        listener.onNicknameLoaded(nickname);
                    } else {
                        // No relationship exists yet
                        listener.onNicknameLoaded(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting nickname: " + e.getMessage());
                    listener.onFailure("Failed to get nickname: " + e.getMessage());
                });
    }

    /**
     * Private helper method to update relationship between users
     * @param otherUserId ID of the other user
     * @param blocked Block status (can be null if not changing)
     * @param muted Mute status (can be null if not changing)
     * @param nickname Nickname (can be null if not changing)
     * @param listener Callback listener for the operation result
     */
    private void updateRelationship(String otherUserId, Boolean blocked, Boolean muted, String nickname, RelationshipOperationListener listener) {
        String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
        
        if (currentUserId == null || currentUserId.isEmpty()) {
            listener.onFailure("Current user not available");
            return;
        }

        // First check if relationship already exists
        database.collection(Constants.KEY_COLLECTION_USER_RELATIONSHIPS)
                .whereEqualTo(Constants.KEY_USER_ID, currentUserId)
                .whereEqualTo(Constants.KEY_OTHER_USER_ID, otherUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Object> updates = new HashMap<>();
                    if (blocked != null) {
                        updates.put(Constants.KEY_BLOCKED, blocked);
                    }
                    if (muted != null) {
                        updates.put(Constants.KEY_MUTED, muted);
                    }
                    // Always update the nickname field when it's explicitly provided as parameter
                    // This ensures that null values (for deleting nicknames) are properly processed
                    if (nickname != null || nickname == null && !queryDocumentSnapshots.isEmpty()) {
                        updates.put(Constants.KEY_NICKNAME, nickname);
                    }
                    updates.put(Constants.KEY_LAST_UPDATED, new Date());

                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Update existing relationship
                        String docId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        database.collection(Constants.KEY_COLLECTION_USER_RELATIONSHIPS)
                                .document(docId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> listener.onSuccess())
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error updating relationship: " + e.getMessage());
                                    listener.onFailure("Failed to update relationship: " + e.getMessage());
                                });
                    } else {
                        // Create new relationship
                        Map<String, Object> relationshipData = new HashMap<>();
                        relationshipData.put(Constants.KEY_USER_ID, currentUserId);
                        relationshipData.put(Constants.KEY_OTHER_USER_ID, otherUserId);
                        relationshipData.put(Constants.KEY_BLOCKED, blocked != null ? blocked : false);
                        relationshipData.put(Constants.KEY_MUTED, muted != null ? muted : false);
                        relationshipData.put(Constants.KEY_NICKNAME, nickname);
                        relationshipData.put(Constants.KEY_LAST_UPDATED, new Date());

                        database.collection(Constants.KEY_COLLECTION_USER_RELATIONSHIPS)
                                .add(relationshipData)
                                .addOnSuccessListener(documentReference -> listener.onSuccess())
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error creating relationship: " + e.getMessage());
                                    listener.onFailure("Failed to create relationship: " + e.getMessage());
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking for existing relationship: " + e.getMessage());
                    listener.onFailure("Failed to update relationship: " + e.getMessage());
                });
    }
}