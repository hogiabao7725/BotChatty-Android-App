package com.hgb7725.botchattyapp.firebase;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.hgb7725.botchattyapp.models.FileItem;
import com.hgb7725.botchattyapp.models.MediaItem;
import com.hgb7725.botchattyapp.utilities.Constants;
import com.hgb7725.botchattyapp.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MediaFirebaseService {

    private static final String TAG = "MediaFirebaseService";
    private final FirebaseFirestore database;
    private final Context context;
    private final PreferenceManager preferenceManager;

    public MediaFirebaseService(Context context, PreferenceManager preferenceManager) {
        this.context = context;
        this.preferenceManager = preferenceManager;
        this.database = FirebaseFirestore.getInstance();
    }

    /**
     * Interface for media fetch listener
     */
    public interface MediaFetchListener {
        void onMediaFetched(List<MediaItem> mediaItems);
        void onError(String errorMessage);
    }
    
    /**
     * Interface for file fetch listener
     */
    public interface FileFetchListener {
        void onFilesFetched(List<FileItem> fileItems);
        void onError(String errorMessage);
    }

    /**
     * Fetches all shared media (images and videos) between current user and specified user
     *
     * @param otherUserId ID of the other user in conversation
     * @param listener callback for result
     */
    public void fetchSharedMedia(String otherUserId, MediaFetchListener listener) {
        List<MediaItem> mediaItems = new ArrayList<>();
        String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
        
        // Query for media messages from both users
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereIn("type", Arrays.asList("image", "video"))
                .orderBy(Constants.KEY_TIMESTAMP, Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String senderId = document.getString(Constants.KEY_SENDER_ID);
                            String receiverId = document.getString(Constants.KEY_RECEIVER_ID);
                            
                            // Check if this message belongs to the conversation between these users
                            if ((senderId.equals(currentUserId) && receiverId.equals(otherUserId)) ||
                                (senderId.equals(otherUserId) && receiverId.equals(currentUserId))) {
                                
                                String type = document.getString("type");
                                // The URL for both images and videos is stored in the message field
                                String url = document.getString(Constants.KEY_MESSAGE);
                                String timestamp = document.getDate(Constants.KEY_TIMESTAMP).toString();
                                
                                // Create the MediaItem with available information
                                MediaItem mediaItem = new MediaItem(url, type, timestamp, senderId);
                                mediaItems.add(mediaItem);
                            }
                        }
                        listener.onMediaFetched(mediaItems);
                    } else {
                        Log.e(TAG, "Error getting media: ", task.getException());
                        listener.onError("Failed to load shared media");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting media: ", e);
                    listener.onError("Failed to load shared media: " + e.getMessage());
                });
    }
    
    /**
     * Fetches limited number of media items for preview
     */
    public void fetchMediaPreview(String otherUserId, int limit, MediaFetchListener listener) {
        List<MediaItem> mediaItems = new ArrayList<>();
        String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
        
        // Query for media messages from both users with limit
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereIn("type", Arrays.asList("image", "video"))
                .orderBy(Constants.KEY_TIMESTAMP, Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String senderId = document.getString(Constants.KEY_SENDER_ID);
                            String receiverId = document.getString(Constants.KEY_RECEIVER_ID);
                            
                            // Check if this message belongs to the conversation between these users
                            if ((senderId.equals(currentUserId) && receiverId.equals(otherUserId)) ||
                                (senderId.equals(otherUserId) && receiverId.equals(currentUserId))) {
                                
                                String type = document.getString("type");
                                String url = document.getString(Constants.KEY_MESSAGE);
                                String timestamp = document.getDate(Constants.KEY_TIMESTAMP).toString();
                                
                                // Create the MediaItem with available information
                                MediaItem mediaItem = new MediaItem(url, type, timestamp, senderId);
                                mediaItems.add(mediaItem);
                            }
                        }
                        listener.onMediaFetched(mediaItems);
                    } else {
                        Log.e(TAG, "Error getting media preview: ", task.getException());
                        listener.onError("Failed to load shared media");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting media preview: ", e);
                    listener.onError("Failed to load shared media: " + e.getMessage());
                });
    }
    
    /**
     * Fetches all shared files between current user and specified user
     */
    public void fetchSharedFiles(String otherUserId, FileFetchListener listener) {
        List<FileItem> fileItems = new ArrayList<>();
        String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
        
        // Query for file messages
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo("type", "file")
                .orderBy(Constants.KEY_TIMESTAMP, Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String senderId = document.getString(Constants.KEY_SENDER_ID);
                            String receiverId = document.getString(Constants.KEY_RECEIVER_ID);
                            
                            // Check if this message belongs to the conversation between these users
                            if ((senderId.equals(currentUserId) && receiverId.equals(otherUserId)) ||
                                (senderId.equals(otherUserId) && receiverId.equals(currentUserId))) {
                                
                                String fileName = document.getString("fileName");
                                String fileUrl = document.getString(Constants.KEY_MESSAGE);
                                String timestamp = document.getDate(Constants.KEY_TIMESTAMP).toString();
                                
                                FileItem fileItem = new FileItem(fileUrl, fileName, timestamp, senderId);
                                fileItems.add(fileItem);
                            }
                        }
                        listener.onFilesFetched(fileItems);
                    } else {
                        Log.e(TAG, "Error getting files: ", task.getException());
                        listener.onError("Failed to load shared files");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting files: ", e);
                    listener.onError("Failed to load shared files: " + e.getMessage());
                });
    }
    
    /**
     * Fetches limited number of file items for preview
     */
    public void fetchFilesPreview(String otherUserId, int limit, FileFetchListener listener) {
        List<FileItem> fileItems = new ArrayList<>();
        String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
        
        // Query for file messages with limit
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo("type", "file")
                .orderBy(Constants.KEY_TIMESTAMP, Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String senderId = document.getString(Constants.KEY_SENDER_ID);
                            String receiverId = document.getString(Constants.KEY_RECEIVER_ID);
                            
                            // Check if this message belongs to the conversation between these users
                            if ((senderId.equals(currentUserId) && receiverId.equals(otherUserId)) ||
                                (senderId.equals(otherUserId) && receiverId.equals(currentUserId))) {
                                
                                String fileName = document.getString("fileName");
                                String fileUrl = document.getString(Constants.KEY_MESSAGE);
                                String timestamp = document.getDate(Constants.KEY_TIMESTAMP).toString();
                                
                                FileItem fileItem = new FileItem(fileUrl, fileName, timestamp, senderId);
                                fileItems.add(fileItem);
                            }
                        }
                        listener.onFilesFetched(fileItems);
                    } else {
                        Log.e(TAG, "Error getting file preview: ", task.getException());
                        listener.onError("Failed to load shared files");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting file preview: ", e);
                    listener.onError("Failed to load shared files: " + e.getMessage());
                });
    }
}