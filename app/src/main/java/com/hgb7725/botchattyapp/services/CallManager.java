package com.hgb7725.botchattyapp.services;

import android.app.Activity;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.hgb7725.botchattyapp.dialogs.IncomingCallDialog;
import com.hgb7725.botchattyapp.models.User;
import com.hgb7725.botchattyapp.utilities.Constants;
import com.hgb7725.botchattyapp.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashSet;
import java.util.Set;

public class CallManager {
    private static CallManager instance;
    private final Application application;
    private Activity currentActivity;
    private IncomingCallDialog incomingCallDialog;
    private CallService callService;
    private PreferenceManager preferenceManager;
    private Set<String> shownDialogCallIds = new HashSet<>();
    private String currentCallId = null;

    private CallManager(Application app) {
        this.application = app;
        this.preferenceManager = new PreferenceManager(app.getApplicationContext());
        this.callService = new CallService(app.getApplicationContext());
        registerActivityLifecycleCallbacks();
        startListeningForCalls();
    }

    public static void init(Application app) {
        if (instance == null) {
            instance = new CallManager(app);
        }
    }

    private void registerActivityLifecycleCallbacks() {
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, android.os.Bundle bundle) {}
            @Override
            public void onActivityStarted(Activity activity) { currentActivity = activity; }
            @Override
            public void onActivityResumed(Activity activity) { currentActivity = activity; }
            @Override
            public void onActivityPaused(Activity activity) {}
            @Override
            public void onActivityStopped(Activity activity) {}
            @Override
            public void onActivitySaveInstanceState(Activity activity, android.os.Bundle bundle) {}
            @Override
            public void onActivityDestroyed(Activity activity) {
                if (currentActivity == activity) {
                    currentActivity = null;
                    // Don't dismiss dialog when activity is destroyed
                    // This ensures the dialog stays visible across activity transitions
                }
            }
        });
    }

    private void startListeningForCalls() {
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);
        if (userId == null) return;
        callService.setCallListener(new CallService.OnCallListener() {
            @Override
            public void onIncomingCall(String callId, User caller, boolean isVideoCall) {
                // Clear previous call states when receiving a new call
                clearAllCallStates();
                
                FirebaseFirestore.getInstance()
                    .collection(Constants.KEY_COLLECTION_CALLS)
                    .document(callId)
                    .get()
                    .addOnSuccessListener((DocumentSnapshot document) -> {
                        String status = document.getString("status");
                        if ("pending".equals(status)) {
                            currentCallId = callId;
                            shownDialogCallIds.add(callId);
                            showIncomingCallDialog(callId, caller, isVideoCall);
                        }
                    });
            }
            @Override
            public void onCallAccepted(String callId) {
                cleanupCallState(callId);
            }
            @Override
            public void onCallRejected(String callId) {
                cleanupCallState(callId);
            }
            @Override
            public void onCallEnded(String callId) {
                cleanupCallState(callId);
            }
        });
        callService.listenForIncomingCalls(userId);
    }

    private void showIncomingCallDialog(String callId, User caller, boolean isVideoCall) {
        if (currentActivity == null) return;
        
        new Handler(Looper.getMainLooper()).post(() -> {
            // Always dismiss any existing dialog first
            dismissDialog();
            
            incomingCallDialog = new IncomingCallDialog(currentActivity, caller, isVideoCall, new IncomingCallDialog.OnCallActionListener() {
                @Override
                public void onAcceptCall(String dialogCallId) {
                    callService.acceptCall(dialogCallId);
                    try {
                        Class<?> callActivity = isVideoCall ?
                                Class.forName("com.hgb7725.botchattyapp.activities.VideoCallActivity") :
                                Class.forName("com.hgb7725.botchattyapp.activities.VoiceCallActivity");
                        android.content.Intent intent = new android.content.Intent(currentActivity, callActivity);
                        intent.putExtra("callId", dialogCallId);
                        intent.putExtra(Constants.KEY_USER, caller);
                        currentActivity.startActivity(intent);
                    } catch (Exception e) {
                        Log.e("CallManager", "Error starting call activity", e);
                    }
                }
                @Override
                public void onRejectCall(String dialogCallId) {
                    callService.rejectCall(dialogCallId);
                }
            });
            incomingCallDialog.setCallId(callId);
            incomingCallDialog.show();
        });
    }

    private void cleanupCallState(String callId) {
        shownDialogCallIds.remove(callId);
        if (currentCallId != null && currentCallId.equals(callId)) {
            currentCallId = null;
        }
        dismissDialog();
    }

    private void clearAllCallStates() {
        shownDialogCallIds.clear();
        currentCallId = null;
        dismissDialog();
    }

    private void dismissDialog() {
        if (incomingCallDialog != null && incomingCallDialog.isShowing()) {
            incomingCallDialog.dismiss();
        }
        incomingCallDialog = null;
    }
} 