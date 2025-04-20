package com.hgb7725.botchattyapp.emailJs;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

public class EmailJsService {

    private static final String TAG = "EmailJsService";

    /**
     * Sends an OTP email to the specified address using EmailJS
     *
     * @param context   Application context
     * @param toEmail   Target email address
     * @param otpCode   The OTP code to be sent
     * @param onSuccess Callback when the request is successful or assumed successful
     */
    public static void sendOtpEmail(@NonNull Context context,
                                    @NonNull String toEmail,
                                    @NonNull String otpCode,
                                    @NonNull Runnable onSuccess) {

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("service_id", EmailJsConfig.SERVICE_ID);
            requestBody.put("template_id", EmailJsConfig.TEMPLATE_ID);
            requestBody.put("user_id", EmailJsConfig.PUBLIC_KEY);

            JSONObject templateParams = new JSONObject();
            templateParams.put("user_email", toEmail);
            templateParams.put("passcode", otpCode);
            requestBody.put("template_params", templateParams);

            Log.d(TAG, "Sending JSON: " + requestBody.toString());

        } catch (JSONException e) {
            Log.e(TAG, "Failed to build JSON request", e);
            Toast.makeText(context, "Failed to build email request", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                EmailJsConfig.EMAIL_JS_API_URL,
                requestBody,
                response -> {
                    Log.d(TAG, "EmailJS response: " + response.toString());
                    Toast.makeText(context, "OTP sent successfully", Toast.LENGTH_SHORT).show();
                    onSuccess.run();
                },
                error -> {
                    error.printStackTrace();

                    // If server gives no response, it's possibly 204 No Content → still success
                    if (error.networkResponse == null) {
                        Log.w(TAG, "No response from server. Assuming success.");
                        Toast.makeText(context, "OTP may have been sent. Check your email.", Toast.LENGTH_SHORT).show();
                        onSuccess.run();
                        return;
                    }

                    // Server returned an error
                    int statusCode = error.networkResponse.statusCode;
                    String responseData = new String(error.networkResponse.data);
                    Log.e(TAG, "Status: " + statusCode + " | Response: " + responseData);
                    Toast.makeText(context, "Failed to send OTP email", Toast.LENGTH_SHORT).show();
                });

        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(request);
    }

    /**
     * Generates a numeric OTP code of given length
     *
     * @param length Length of OTP (e.g., 6)
     * @return Random numeric OTP string
     */
    public static String generateOtp(int length) {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();

        for (int i = 0; i < length; i++) {
            otp.append(random.nextInt(10)); // digits 0-9
        }

        return otp.toString();
    }
}
