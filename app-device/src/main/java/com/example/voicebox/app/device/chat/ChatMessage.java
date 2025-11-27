package com.example.voicebox.app.device.chat;

import java.sql.Timestamp;

public class ChatMessage {
    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private Timestamp createdAt;
    private String sentiment;
    private String extractedTopics; // JSON string
    private Integer responseTimeMs;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public String getExtractedTopics() {
        return extractedTopics;
    }

    public void setExtractedTopics(String extractedTopics) {
        this.extractedTopics = extractedTopics;
    }

    public Integer getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Integer responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }
}

