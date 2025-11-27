package com.example.voicebox.app.device.domain;

import java.sql.Timestamp;

/**
 * Device entity for hardware integration (e.g., Raspberry Pi devices).
 */
public class Device {
    private Long id;
    private Long userId;
    private String deviceType; // 'web', 'mobile', 'raspberry_pi'
    private String deviceName;
    private String apiKey;
    private Timestamp lastSyncAt;
    private Boolean isActive;
    private String deviceMetadata; // JSON string
    private Timestamp createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Timestamp getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(Timestamp lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getDeviceMetadata() {
        return deviceMetadata;
    }

    public void setDeviceMetadata(String deviceMetadata) {
        this.deviceMetadata = deviceMetadata;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
