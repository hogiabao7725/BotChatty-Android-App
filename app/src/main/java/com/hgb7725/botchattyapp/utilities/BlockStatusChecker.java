package com.hgb7725.botchattyapp.utilities;

import android.content.Context;
import android.util.Log;

import com.hgb7725.botchattyapp.firebase.UserRelationshipService;
import com.hgb7725.botchattyapp.models.User;

/**
 * Helper utility class to check block status between users
 * and enforce block rules across the application
 */
public class BlockStatusChecker {
    private static final String TAG = "BlockStatusChecker";
    private UserRelationshipService relationshipService;

    /**
     * Interface for block status results
     */
    public interface BlockCheckListener {
        void onCheckComplete(boolean isBlocked, boolean isBlockedBy);
    }

    /**
     * Constructor for BlockStatusChecker
     * @param context Application context
     * @param preferenceManager Preference manager to access user preferences
     */
    public BlockStatusChecker(Context context, PreferenceManager preferenceManager) {
        relationshipService = new UserRelationshipService(context, preferenceManager);
    }

    /**
     * Check block status between current user and another user
     * @param otherUserId ID of the other user to check against
     * @param listener Callback listener with the results
     */
    public void checkBlockStatus(String otherUserId, BlockCheckListener listener) {
        // First check if current user has blocked the other user
        relationshipService.getRelationshipStatus(otherUserId, new UserRelationshipService.RelationshipStatusListener() {
            @Override
            public void onStatusLoaded(boolean isBlocked, boolean isMuted) {
                // Then check if current user is blocked by the other user
                relationshipService.isBlockedByUser(otherUserId, new UserRelationshipService.RelationshipStatusListener() {
                    @Override
                    public void onStatusLoaded(boolean isBlockedBy, boolean isMutedBy) {
                        listener.onCheckComplete(isBlocked, isBlockedBy);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Error checking if blocked by other user: " + errorMessage);
                        // If we can't check if we're blocked, assume we're not
                        listener.onCheckComplete(isBlocked, false);
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Error checking if current user blocked other user: " + errorMessage);
                // If we can't check block status, assume no one is blocked
                listener.onCheckComplete(false, false);
            }
        });
    }

    /**
     * Check if current user can send message to other user
     * @param otherUserId ID of the other user
     * @param listener Callback listener with result
     */
    public void canSendMessage(String otherUserId, MessagePermissionListener listener) {
        checkBlockStatus(otherUserId, (isBlocked, isBlockedBy) -> {
            // New behavior: If either user has blocked the other, can't send messages
            boolean canSend = !isBlocked && !isBlockedBy;
            String blockReason = "";
            
            if (isBlockedBy) {
                blockReason = "You have been blocked by this user";
            } else if (isBlocked) {
                blockReason = "You have blocked this user";
            }
            
            listener.onPermissionChecked(canSend, blockReason);
        });
    }
    
    /**
     * Check if current user can make voice or video call to other user
     * @param otherUserId ID of the other user
     * @param listener Callback listener with result
     */
    public void canMakeCall(String otherUserId, MessagePermissionListener listener) {
        checkBlockStatus(otherUserId, (isBlocked, isBlockedBy) -> {
            // Similar to messages, if either user has blocked the other, can't make calls
            boolean canCall = !isBlocked && !isBlockedBy;
            String blockReason = "";
            
            if (isBlockedBy) {
                blockReason = "You have been blocked by this user";
            } else if (isBlocked) {
                blockReason = "You have blocked this user";
            }
            
            listener.onPermissionChecked(canCall, blockReason);
        });
    }
    
    /**
     * Interface for messaging permission check
     */
    public interface MessagePermissionListener {
        void onPermissionChecked(boolean canSend, String blockReason);
    }
}