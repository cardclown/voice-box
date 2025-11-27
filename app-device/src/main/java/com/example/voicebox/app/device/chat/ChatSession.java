package com.example.voicebox.app.device.chat;

import java.sql.Timestamp;

public class ChatSession {
    private Long id;
    private String title;
    private String model;
    private String deviceInfo;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Long userId;
    private String tags; // JSON string
    private String personalizationContext; // JSON string

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getPersonalizationContext() {
        return personalizationContext;
    }

    public void setPersonalizationContext(String personalizationContext) {
        this.personalizationContext = personalizationContext;
    }
}

