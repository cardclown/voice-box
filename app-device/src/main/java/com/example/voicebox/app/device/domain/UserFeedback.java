package com.example.voicebox.app.device.domain;

import java.sql.Timestamp;

/**
 * 用户反馈实体
 * 记录用户对AI回复的反馈
 */
public class UserFeedback {
    
    private Long id;
    private Long userId;
    private Long sessionId;
    private Long messageId;
    
    // 反馈类型
    private String feedbackType;      // like/dislike/regenerate/edit/copy
    private Integer feedbackValue;    // 1=正面 -1=负面
    
    // 反馈内容
    private String feedbackText;
    private String feedbackTags;      // JSON
    
    // 上下文
    private Long aiResponseId;
    private String responseStrategy;  // JSON
    
    private Timestamp createdAt;
    
    // Constructors
    public UserFeedback() {
    }
    
    // Getters and Setters
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
    
    public Long getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }
    
    public Long getMessageId() {
        return messageId;
    }
    
    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }
    
    public String getFeedbackType() {
        return feedbackType;
    }
    
    public void setFeedbackType(String feedbackType) {
        this.feedbackType = feedbackType;
    }
    
    public Integer getFeedbackValue() {
        return feedbackValue;
    }
    
    public void setFeedbackValue(Integer feedbackValue) {
        this.feedbackValue = feedbackValue;
    }
    
    public String getFeedbackText() {
        return feedbackText;
    }
    
    public void setFeedbackText(String feedbackText) {
        this.feedbackText = feedbackText;
    }
    
    public String getFeedbackTags() {
        return feedbackTags;
    }
    
    public void setFeedbackTags(String feedbackTags) {
        this.feedbackTags = feedbackTags;
    }
    
    public Long getAiResponseId() {
        return aiResponseId;
    }
    
    public void setAiResponseId(Long aiResponseId) {
        this.aiResponseId = aiResponseId;
    }
    
    public String getResponseStrategy() {
        return responseStrategy;
    }
    
    public void setResponseStrategy(String responseStrategy) {
        this.responseStrategy = responseStrategy;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "UserFeedback{" +
                "id=" + id +
                ", userId=" + userId +
                ", feedbackType='" + feedbackType + '\'' +
                ", feedbackValue=" + feedbackValue +
                '}';
    }
}
