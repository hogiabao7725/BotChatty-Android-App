package com.hgb7725.botchattyapp.utilities;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class OtpTextWatcher implements TextWatcher {

    private final View currentView;
    private final View nextView;
    private final Context context;

    public OtpTextWatcher(Context context, View currentView, View nextView) {
        this.context = context;
        this.currentView = currentView;
        this.nextView = nextView;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override
    public void afterTextChanged(Editable s) {
        if (s.length() == 1) {
            if (nextView != null) {
                nextView.requestFocus();
            } else {
                // Hide keyboard if it's the last field
                InputMethodManager imm = (InputMethodManager)
                        context.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(currentView.getWindowToken(), 0);
                }
            }
        }
    }
}
