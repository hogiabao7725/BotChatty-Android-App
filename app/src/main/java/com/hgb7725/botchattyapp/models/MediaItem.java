package com.hgb7725.botchattyapp.models;

import java.io.Serializable;

/**
 * Represents a media item (image or video) shared between users
 */
public class MediaItem implements Serializable {
    private String url;
    private String type; // "image" or "video"
    private String timestamp;
    private String senderId;

    public MediaItem(String url, String type, String timestamp, String senderId) {
        this.url = url;
        this.type = type;
        this.timestamp = timestamp;
        this.senderId = senderId;
    }

    public String getUrl() {
        return url;
    }

    public String getType() {
        return type;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getSenderId() {
        return senderId;
    }

    public boolean isVideo() {
        return "video".equals(type);
    }

    public boolean isImage() {
        return "image".equals(type);
    }
}