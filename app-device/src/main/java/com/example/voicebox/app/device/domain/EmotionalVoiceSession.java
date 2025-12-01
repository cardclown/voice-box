package com.example.voicebox.app.device.domain;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 情感语音会话实体
 * 记录用户的情感语音对话会话信息
 */
@Entity
@Table(name = "emotional_voice_sessions")
public class EmotionalVoiceSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "session_name", length = 100)
    private String sessionName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false)
    private SessionType sessionType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "mood_context")
    private MoodContext moodContext;
    
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Column(name = "total_duration_seconds")
    private Integer totalDurationSeconds;
    
    @Column(name = "message_count")
    private Integer messageCount;
    
    @Column(name = "dominant_emotion")
    private String dominantEmotion;
    
    @Column(name = "emotion_intensity", precision = 3, scale = 2)
    private Double emotionIntensity;
    
    @Column(name = "user_satisfaction_score", precision = 3, scale = 2)
    private Double userSatisfactionScore;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EmotionalVoiceMessage> messages;
    
    // 会话类型枚举
    public enum SessionType {
        CASUAL_CHAT,      // 闲聊
        EMOTIONAL_SUPPORT, // 情感支持
        COMPANIONSHIP,    // 陪伴
        VENTING,          // 倾诉
        CELEBRATION,      // 庆祝分享
        PROBLEM_SOLVING   // 问题解决
    }
    
    // 情绪背景枚举
    public enum MoodContext {
        HAPPY,           // 开心
        SAD,             // 难过
        ANXIOUS,         // 焦虑
        EXCITED,         // 兴奋
        FRUSTRATED,      // 沮丧
        CALM,            // 平静
        STRESSED,        // 压力大
        LONELY,          // 孤独
        GRATEFUL,        // 感激
        CONFUSED         // 困惑
    }
    
    // 构造函数
    public EmotionalVoiceSession() {
        this.createdAt = LocalDateTime.now();
        this.startTime = LocalDateTime.now();
        this.messageCount = 0;
    }
    
    public EmotionalVoiceSession(Long userId, SessionType sessionType) {
        this();
        this.userId = userId;
        this.sessionType = sessionType;
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
    
    public String getSessionName() {
        return sessionName;
    }
    
    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }
    
    public SessionType getSessionType() {
        return sessionType;
    }
    
    public void setSessionType(SessionType sessionType) {
        this.sessionType = sessionType;
    }
    
    public MoodContext getMoodContext() {
        return moodContext;
    }
    
    public void setMoodContext(MoodContext moodContext) {
        this.moodContext = moodContext;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public Integer getTotalDurationSeconds() {
        return totalDurationSeconds;
    }
    
    public void setTotalDurationSeconds(Integer totalDurationSeconds) {
        this.totalDurationSeconds = totalDurationSeconds;
    }
    
    public Integer getMessageCount() {
        return messageCount;
    }
    
    public void setMessageCount(Integer messageCount) {
        this.messageCount = messageCount;
    }
    
    public String getDominantEmotion() {
        return dominantEmotion;
    }
    
    public void setDominantEmotion(String dominantEmotion) {
        this.dominantEmotion = dominantEmotion;
    }
    
    public Double getEmotionIntensity() {
        return emotionIntensity;
    }
    
    public void setEmotionIntensity(Double emotionIntensity) {
        this.emotionIntensity = emotionIntensity;
    }
    
    public Double getUserSatisfactionScore() {
        return userSatisfactionScore;
    }
    
    public void setUserSatisfactionScore(Double userSatisfactionScore) {
        this.userSatisfactionScore = userSatisfactionScore;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public List<EmotionalVoiceMessage> getMessages() {
        return messages;
    }
    
    public void setMessages(List<EmotionalVoiceMessage> messages) {
        this.messages = messages;
    }
    
    // 业务方法
    
    /**
     * 结束会话
     */
    public void endSession() {
        this.endTime = LocalDateTime.now();
        if (this.startTime != null) {
            this.totalDurationSeconds = (int) java.time.Duration.between(this.startTime, this.endTime).getSeconds();
        }
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 增加消息计数
     */
    public void incrementMessageCount() {
        this.messageCount = (this.messageCount == null ? 0 : this.messageCount) + 1;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 更新主导情绪
     */
    public void updateDominantEmotion(String emotion, Double intensity) {
        this.dominantEmotion = emotion;
        this.emotionIntensity = intensity;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 检查会话是否活跃
     */
    public boolean isActive() {
        return this.endTime == null;
    }
    
    /**
     * 获取会话持续时间（分钟）
     */
    public Integer getDurationMinutes() {
        if (this.totalDurationSeconds == null) {
            return null;
        }
        return this.totalDurationSeconds / 60;
    }
}
