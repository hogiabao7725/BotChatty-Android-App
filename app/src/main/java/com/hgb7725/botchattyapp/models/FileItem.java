package com.hgb7725.botchattyapp.models;

import java.io.Serializable;

/**
 * Represents a file shared between users in a chat
 */
public class FileItem implements Serializable {
    private String fileUrl;
    private String fileName;
    private String timestamp;
    private String senderId;

    public FileItem(String fileUrl, String fileName, String timestamp, String senderId) {
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.timestamp = timestamp;
        this.senderId = senderId;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getSenderId() {
        return senderId;
    }
    
    /**
     * Returns the file extension (e.g., pdf, docx)
     */
    public String getFileExtension() {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}