package com.hgb7725.botchattyapp.models;

import java.util.Date;

/**
 * Represents a relationship between two users.
 * This model is used to store different relationship states like blocking, muting, etc.
 */
public class UserRelationship {
    // Removed id field as it's unnecessary
    private String userId;
    private String otherUserId;
    private boolean blocked;
    private boolean muted;
    private String nickname;
    private Date lastUpdated;

    /**
     * Default constructor required for Firebase
     */
    public UserRelationship() {
    }

    /**
     * Constructor for creating a new user relationship
     * @param userId The ID of the current user
     * @param otherUserId The ID of the other user in the relationship
     * @param blocked Whether the current user has blocked the other user
     * @param muted Whether the current user has muted notifications from the other user
     * @param nickname Optional nickname for the other user (can be null)
     */
    public UserRelationship(String userId, String otherUserId, boolean blocked, boolean muted, String nickname) {
        this.userId = userId;
        this.otherUserId = otherUserId;
        this.blocked = blocked;
        this.muted = muted;
        this.nickname = nickname;
        this.lastUpdated = new Date();
    }

    // Getters
    public String getUserId() { return userId; }
    public String getOtherUserId() { return otherUserId; }
    public boolean isBlocked() { return blocked; } // Keep isBlocked() method name for boolean getter
    public boolean isMuted() { return muted; }     // Keep isMuted() method name for boolean getter
    public String getNickname() { return nickname; }
    public Date getLastUpdated() { return lastUpdated; }

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setOtherUserId(String otherUserId) { this.otherUserId = otherUserId; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
    public void setMuted(boolean muted) { this.muted = muted; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setLastUpdated(Date lastUpdated) { this.lastUpdated = lastUpdated; }

    /**
     * Updates the relationship status and sets the lastUpdated to current time
     * @param blocked New block status
     * @param muted New mute status
     * @param nickname New nickname (can be null)
     */
    public void updateStatus(boolean blocked, boolean muted, String nickname) {
        this.blocked = blocked;
        this.muted = muted;
        this.nickname = nickname;
        this.lastUpdated = new Date();
    }
}