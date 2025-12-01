package com.example.voicebox.app.device.domain;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户情感画像实体
 */
@Entity
@Table(name = "user_emotional_profiles")
public class UserEmotionalProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    
    @Column(name = "dominant_personality_type")
    private String dominantPersonalityType;
    
    @Column(name = "emotional_stability", precision = 3, scale = 2)
    private Double emotionalStability;
    
    @Column(name = "extroversion_level", precision = 3, scale = 2)
    private Double extroversionLevel;
    
    @Column(name = "optimism_level", precision = 3, scale = 2)
    private Double optimismLevel;
    
    @Column(name = "sensitivity_level", precision = 3, scale = 2)
    private Double sensitivityLevel;
    
    @Column(name = "preferred_voice_style")
    private String preferredVoiceStyle;
    
    @Column(name = "preferred_response_length")
    private String preferredResponseLength;
    
    @Column(name = "preferred_conversation_pace")
    private String preferredConversationPace;
    
    @Column(name = "preferred_emotional_tone")
    private String preferredEmotionalTone;
    
    @Column(name = "primary_emotional_need")
    private String primaryEmotionalNeed;
    
    @Column(name = "comfort_strategies", columnDefinition = "TEXT")
    private String comfortStrategies;
    
    @Column(name = "trigger_words", columnDefinition = "TEXT")
    private String triggerWords;
    
    @Column(name = "positive_reinforcement_style")
    private String positiveReinforcementStyle;
    
    @Column(name = "interaction_frequency_preference")
    private String interactionFrequencyPreference;
    
    @Column(name = "preferred_language", length = 10)
    private String preferredLanguage;
    
    @Column(name = "privacy_comfort_level", precision = 3, scale = 2)
    private Double privacyComfortLevel;
    
    @Column(name = "emotional_sharing_willingness", precision = 3, scale = 2)
    private Double emotionalSharingWillingness;
    
    @Column(name = "learning_rate", precision = 3, scale = 2)
    private Double learningRate;
    
    @Column(name = "adaptation_sensitivity", precision = 3, scale = 2)
    private Double adaptationSensitivity;
    
    @Column(name = "feedback_responsiveness", precision = 3, scale = 2)
    private Double feedbackResponsiveness;
    
    @Column(name = "total_sessions")
    private Integer totalSessions;
    
    @Column(name = "total_interaction_minutes")
    private Integer totalInteractionMinutes;
    
    @Column(name = "average_session_satisfaction", precision = 3, scale = 2)
    private Double averageSessionSatisfaction;
    
    @Column(name = "most_frequent_emotion")
    private String mostFrequentEmotion;
    
    @Column(name = "emotional_range_score", precision = 3, scale = 2)
    private Double emotionalRangeScore;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_interaction_at")
    private LocalDateTime lastInteractionAt;
    
    public UserEmotionalProfile() {
        this.createdAt = LocalDateTime.now();
        this.totalSessions = 0;
        this.totalInteractionMinutes = 0;
        this.emotionalStability = 0.5;
        this.extroversionLevel = 0.5;
        this.optimismLevel = 0.5;
        this.sensitivityLevel = 0.5;
        this.learningRate = 0.7;
        this.adaptationSensitivity = 0.6;
        this.feedbackResponsiveness = 0.8;
        this.privacyComfortLevel = 0.5;
        this.emotionalSharingWillingness = 0.5;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getDominantPersonalityType() { return dominantPersonalityType; }
    public void setDominantPersonalityType(String dominantPersonalityType) { this.dominantPersonalityType = dominantPersonalityType; }
    
    public Double getEmotionalStability() { return emotionalStability; }
    public void setEmotionalStability(Double emotionalStability) { this.emotionalStability = emotionalStability; }
    
    public Double getExtroversionLevel() { return extroversionLevel; }
    public void setExtroversionLevel(Double extroversionLevel) { this.extroversionLevel = extroversionLevel; }
    
    public Double getOptimismLevel() { return optimismLevel; }
    public void setOptimismLevel(Double optimismLevel) { this.optimismLevel = optimismLevel; }
    
    public Double getSensitivityLevel() { return sensitivityLevel; }
    public void setSensitivityLevel(Double sensitivityLevel) { this.sensitivityLevel = sensitivityLevel; }
    
    public String getPreferredVoiceStyle() { return preferredVoiceStyle; }
    public void setPreferredVoiceStyle(String preferredVoiceStyle) { this.preferredVoiceStyle = preferredVoiceStyle; }
    
    public Integer getTotalSessions() { return totalSessions; }
    public void setTotalSessions(Integer totalSessions) { this.totalSessions = totalSessions; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getLastInteractionAt() { return lastInteractionAt; }
    public void setLastInteractionAt(LocalDateTime lastInteractionAt) { this.lastInteractionAt = lastInteractionAt; }
    
    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }
    
    public String getMostFrequentEmotion() { return mostFrequentEmotion; }
    public void setMostFrequentEmotion(String mostFrequentEmotion) { this.mostFrequentEmotion = mostFrequentEmotion; }
    
    public boolean isNewUser() {
        return this.totalSessions == null || this.totalSessions < 3;
    }
}
