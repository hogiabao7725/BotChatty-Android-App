package com.hgb7725.botchattyapp.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.hgb7725.botchattyapp.R;
import com.hgb7725.botchattyapp.models.User;

public class IncomingCallDialog extends Dialog {
    private final User caller;
    private final boolean isVideoCall;
    private final OnCallActionListener listener;
    private String callId;

    public interface OnCallActionListener {
        void onAcceptCall(String callId);
        void onRejectCall(String callId);
    }

    public IncomingCallDialog(@NonNull Context context, User caller, boolean isVideoCall, OnCallActionListener listener) {
        super(context);
        this.caller = caller;
        this.isVideoCall = isVideoCall;
        this.listener = listener;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_incoming_call);

        // Set dialog to be full width
        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Initialize views
        ImageView imageCallerProfile = findViewById(R.id.imageCallerProfile);
        TextView textCallerName = findViewById(R.id.textCallerName);
        TextView textCallType = findViewById(R.id.textCallType);
        ImageView imageAcceptCall = findViewById(R.id.imageAcceptCall);
        ImageView imageRejectCall = findViewById(R.id.imageRejectCall);

        // Set caller information
        if (caller.getImage() != null && !caller.getImage().isEmpty()) {
            byte[] bytes = Base64.decode(caller.getImage(), Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            imageCallerProfile.setImageBitmap(bitmap);
        }

        textCallerName.setText(caller.getName());
        textCallType.setText(isVideoCall ? "Video Call" : "Voice Call");

        // Set click listeners
        imageAcceptCall.setOnClickListener(v -> {
            if (listener != null && callId != null) {
                listener.onAcceptCall(callId);
            }
            dismiss();
        });

        imageRejectCall.setOnClickListener(v -> {
            if (listener != null && callId != null) {
                listener.onRejectCall(callId);
            }
            dismiss();
        });
    }
} 