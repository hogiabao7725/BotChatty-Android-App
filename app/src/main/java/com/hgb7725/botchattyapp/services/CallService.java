package com.hgb7725.botchattyapp.services;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.hgb7725.botchattyapp.models.User;
import com.hgb7725.botchattyapp.utilities.Constants;

import java.util.HashMap;

public class CallService {
    private static final String TAG = "CallService";
    private final FirebaseFirestore database;
    private final Context context;
    private OnCallListener callListener;

    public interface OnCallListener {
        void onIncomingCall(String callId, User caller, boolean isVideoCall);
        void onCallAccepted(String callId);
        void onCallRejected(String callId);
        void onCallEnded(String callId);
    }

    public interface OnCallInitiatedListener {
        void onCallInitiated(String callId, boolean isVideoCall);
    }

    public interface SingleCallStatusListener {
        void onStatusChanged(String status);
    }

    public CallService(Context context) {
        this.context = context;
        this.database = FirebaseFirestore.getInstance();
    }

    public void setCallListener(OnCallListener listener) {
        this.callListener = listener;
    }

    public void initiateCall(String receiverId, boolean isVideoCall, OnCallInitiatedListener initiatedListener) {
        String currentUserId = context.getSharedPreferences(Constants.KEY_PREFERENCE_NAME, Context.MODE_PRIVATE)
                .getString(Constants.KEY_USER_ID, null);
        if (currentUserId == null) return;
        // Check for existing pending call between the same users
        database.collection(Constants.KEY_COLLECTION_CALLS)
                .whereEqualTo("callerId", currentUserId)
                .whereEqualTo("receiverId", receiverId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        // No pending call, create new
                        HashMap<String, Object> callData = new HashMap<>();
                        callData.put("callerId", currentUserId);
                        callData.put("receiverId", receiverId);
                        callData.put("isVideoCall", isVideoCall);
                        callData.put("status", "pending");
                        callData.put("timestamp", System.currentTimeMillis());
                        database.collection(Constants.KEY_COLLECTION_CALLS)
                                .add(callData)
                                .addOnSuccessListener(documentReference -> {
                                    String callId = documentReference.getId();
                                    listenForCallStatus(callId);
                                    if (initiatedListener != null) {
                                        initiatedListener.onCallInitiated(callId, isVideoCall);
                                    }
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Error initiating call: " + e.getMessage()));
                    } else {
                        // There is already a pending call, do not create new
                        if (initiatedListener != null) {
                            initiatedListener.onCallInitiated(null, isVideoCall);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking pending calls: " + e.getMessage()));
    }

    private String lastShownCallId = null;
    private String currentCallId = null;

    public void listenForIncomingCalls(String userId) {
        database.collection(Constants.KEY_COLLECTION_CALLS)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening for calls: " + error.getMessage());
                        return;
                    }
                    if (value != null && !value.isEmpty()) {
                        for (com.google.firebase.firestore.DocumentSnapshot document : value.getDocuments()) {
                            String callId = document.getId();
                            // Skip if this call is already being handled
                            if (lastShownCallId != null && lastShownCallId.equals(callId)) continue;
                            
                            lastShownCallId = callId;
                            String callerId = document.getString("callerId");
                            Boolean isVideoCallObj = document.getBoolean("isVideoCall");
                            boolean isVideoCall = isVideoCallObj != null && isVideoCallObj;
                            database.collection(Constants.KEY_COLLECTION_USERS)
                                    .document(callerId)
                                    .get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        User caller = new User();
                                        caller.setId(documentSnapshot.getId());
                                        caller.setName(documentSnapshot.getString(Constants.KEY_NAME));
                                        caller.setImage(documentSnapshot.getString(Constants.KEY_IMAGE));
                                        if (callListener != null) {
                                            callListener.onIncomingCall(callId, caller, isVideoCall);
                                        }
                                    });
                        }
                    }
                });
    }

    private void listenForCallStatus(String callId) {
        currentCallId = callId;
        database.collection(Constants.KEY_COLLECTION_CALLS)
                .document(callId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening for call status: " + error.getMessage());
                        return;
                    }

                    if (value != null && value.exists()) {
                        String status = value.getString("status");
                        if (status != null) {
                            switch (status) {
                                case "accepted":
                                    if (callListener != null) {
                                        callListener.onCallAccepted(callId);
                                    }
                                    break;
                                case "rejected":
                                    if (callListener != null) {
                                        callListener.onCallRejected(callId);
                                    }
                                    cleanupCallState();
                                    break;
                                case "ended":
                                    if (callListener != null) {
                                        callListener.onCallEnded(callId);
                                    }
                                    cleanupCallState();
                                    break;
                            }
                        }
                    }
                });
    }

    private void cleanupCallState() {
        currentCallId = null;
        lastShownCallId = null;
    }

    public void acceptCall(String callId) {
        database.collection(Constants.KEY_COLLECTION_CALLS)
                .document(callId)
                .update("status", "accepted")
                .addOnFailureListener(e -> Log.e(TAG, "Error accepting call: " + e.getMessage()));
    }

    public void rejectCall(String callId) {
        database.collection(Constants.KEY_COLLECTION_CALLS)
                .document(callId)
                .update("status", "rejected")
                .addOnSuccessListener(aVoid -> cleanupCallState())
                .addOnFailureListener(e -> Log.e(TAG, "Error rejecting call: " + e.getMessage()));
    }

    public void endCall(String callId) {
        database.collection(Constants.KEY_COLLECTION_CALLS)
                .document(callId)
                .update("status", "ended")
                .addOnSuccessListener(aVoid -> cleanupCallState())
                .addOnFailureListener(e -> Log.e(TAG, "Error ending call: " + e.getMessage()));
    }

    public static void listenForCallStatus(Context context, String callId, SingleCallStatusListener listener) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_CALLS)
                .document(callId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null && value.exists()) {
                        String status = value.getString("status");
                        if (status != null && listener != null) {
                            listener.onStatusChanged(status);
                        }
                    }
                });
    }
} 