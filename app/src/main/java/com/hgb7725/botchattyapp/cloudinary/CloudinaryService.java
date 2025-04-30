package com.hgb7725.botchattyapp.cloudinary;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.hgb7725.botchattyapp.utilities.FileUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;

public class CloudinaryService {
    private final Context context;
    private final RequestQueue requestQueue;
    private static final String TAG = "CloudinaryService";

    public interface UploadCallback {
        void onSuccess(String url, String fileName);
        void onError(String errorMessage);
    }

    public CloudinaryService(Context context) {
        this.context = context;
        this.requestQueue = Volley.newRequestQueue(context);
    }

    public void uploadImage(Uri imageUri, UploadCallback callback) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            byte[] imageBytes = new byte[inputStream.available()];
            inputStream.read(imageBytes);
            String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

            String uploadUrl = CloudinaryConfig.getUploadUrl("image");

            JSONObject params = new JSONObject();
            params.put("file", "data:image/jpeg;base64," + base64Image);
            params.put("upload_preset", CloudinaryConfig.PRESET_IMAGE);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, uploadUrl, params,
                    response -> {
                        try {
                            String imageUrl = response.getString("secure_url");
                            callback.onSuccess(imageUrl, null);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            callback.onError("Error parsing response: " + e.getMessage());
                        }
                    },
                    error -> {
                        String errorMsg = "Unknown error";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            errorMsg = new String(error.networkResponse.data);
                        } else if (error.getMessage() != null) {
                            errorMsg = error.getMessage();
                        }
                        Log.e(TAG, "Upload failed: " + errorMsg, error);
                        callback.onError("Upload failed: " + errorMsg);
                    }
            );

            requestQueue.add(request);
            Toast.makeText(context, "Uploading image...", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            callback.onError("Upload error: " + e.getMessage());
        }
    }

    public void uploadFile(Uri fileUri, UploadCallback callback) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
            byte[] fileBytes = new byte[inputStream.available()];
            inputStream.read(fileBytes);
            String base64File = Base64.encodeToString(fileBytes, Base64.NO_WRAP);

            String originalFileName = FileUtils.getFileNameFromUri(context, fileUri);
            String fileName = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");

            String uploadUrl = CloudinaryConfig.getUploadUrl("raw");

            JSONObject params = new JSONObject();
            String mimeType = context.getContentResolver().getType(fileUri);
            if (mimeType == null) {
                mimeType = "application/octet-stream"; // fallback for unknown
            }
            params.put("file", "data:" + mimeType + ";base64," + base64File);
            params.put("upload_preset", CloudinaryConfig.PRESET_FILE);
            params.put("public_id", fileName);

            Toast.makeText(context, "Uploading file: " + fileName, Toast.LENGTH_SHORT).show();

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, uploadUrl, params,
                    response -> {
                        try {
                            String fileUrl = response.getString("secure_url");
                            callback.onSuccess(fileUrl, fileName);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            callback.onError("Error parsing response: " + e.getMessage());
                        }
                    },
                    error -> {
                        String errorMsg = "Unknown error";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            errorMsg = new String(error.networkResponse.data);
                        } else if (error.getMessage() != null) {
                            errorMsg = error.getMessage();
                        }
                        Log.e(TAG, "Upload failed: " + errorMsg);
                        callback.onError("Upload failed: " + errorMsg);
                    }
            );
            requestQueue.add(request);
        } catch (Exception e) {
            e.printStackTrace();
            callback.onError("Upload error: " + e.getMessage());
        }
    }

    // Future implementations for handling audio uploads could be added here
    public void uploadAudio(Uri audioUri, UploadCallback callback) {
        // Implementation for audio uploads
        Toast.makeText(context, "Audio upload not implemented yet", Toast.LENGTH_SHORT).show();
        callback.onError("Audio upload not implemented yet");
    }
}