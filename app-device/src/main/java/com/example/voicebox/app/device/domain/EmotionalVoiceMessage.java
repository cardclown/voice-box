package com.example.voicebox.app.device.domain;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 情感语音消息实体
 */
@Entity
@Table(name = "emotional_voice_messages")
public class EmotionalVoiceMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private EmotionalVoiceSession session;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "audio_file_path")
    private String audioFilePath;
    
    @Column(name = "audio_duration_seconds")
    private Integer audioDurationSeconds;
    
    @Column(name = "detected_emotion")
    private String detectedEmotion;
    
    @Column(name = "emotion_confidence", precision = 3, scale = 2)
    private Double emotionConfidence;
    
    @Column(name = "emotion_intensity", precision = 3, scale = 2)
    private Double emotionIntensity;
    
    @Column(name = "sentiment_score", precision = 3, scale = 2)
    private Double sentimentScore;
    
    @Column(name = "voice_pitch_avg", precision = 5, scale = 2)
    private Double voicePitchAvg;
    
    @Column(name = "voice_speed_wpm")
    private Integer voiceSpeedWpm;
    
    @Column(name = "voice_volume_db", precision = 5, scale = 2)
    private Double voiceVolumeDb;
    
    @Column(name = "voice_tone")
    private String voiceTone;
    
    @Column(name = "is_directed_to_system")
    private Boolean isDirectedToSystem;
    
    @Column(name = "trigger_confidence", precision = 3, scale = 2)
    private Double triggerConfidence;
    
    @Column(name = "speech_direction")
    private String speechDirection;
    
    @Column(name = "response_strategy")
    private String responseStrategy;
    
    @Column(name = "emotional_response_type")
    private String emotionalResponseType;
    
    @Column(name = "voice_style_used")
    private String voiceStyleUsed;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    public enum MessageType {
        USER_INPUT,
        SYSTEM_RESPONSE,
        SYSTEM_PROMPT
    }
    
    public EmotionalVoiceMessage() {
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public EmotionalVoiceSession getSession() { return session; }
    public void setSession(EmotionalVoiceSession session) { this.session = session; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getAudioFilePath() { return audioFilePath; }
    public void setAudioFilePath(String audioFilePath) { this.audioFilePath = audioFilePath; }
    
    public Integer getAudioDurationSeconds() { return audioDurationSeconds; }
    public void setAudioDurationSeconds(Integer audioDurationSeconds) { this.audioDurationSeconds = audioDurationSeconds; }
    
    public String getDetectedEmotion() { return detectedEmotion; }
    public void setDetectedEmotion(String detectedEmotion) { this.detectedEmotion = detectedEmotion; }
    
    public Double getEmotionConfidence() { return emotionConfidence; }
    public void setEmotionConfidence(Double emotionConfidence) { this.emotionConfidence = emotionConfidence; }
    
    public Double getEmotionIntensity() { return emotionIntensity; }
    public void setEmotionIntensity(Double emotionIntensity) { this.emotionIntensity = emotionIntensity; }
    
    public Double getSentimentScore() { return sentimentScore; }
    public void setSentimentScore(Double sentimentScore) { this.sentimentScore = sentimentScore; }
    
    public Double getVoicePitchAvg() { return voicePitchAvg; }
    public void setVoicePitchAvg(Double voicePitchAvg) { this.voicePitchAvg = voicePitchAvg; }
    
    public Integer getVoiceSpeedWpm() { return voiceSpeedWpm; }
    public void setVoiceSpeedWpm(Integer voiceSpeedWpm) { this.voiceSpeedWpm = voiceSpeedWpm; }
    
    public Double getVoiceVolumeDb() { return voiceVolumeDb; }
    public void setVoiceVolumeDb(Double voiceVolumeDb) { this.voiceVolumeDb = voiceVolumeDb; }
    
    public String getVoiceTone() { return voiceTone; }
    public void setVoiceTone(String voiceTone) { this.voiceTone = voiceTone; }
    
    public Boolean getIsDirectedToSystem() { return isDirectedToSystem; }
    public void setIsDirectedToSystem(Boolean isDirectedToSystem) { this.isDirectedToSystem = isDirectedToSystem; }
    
    public Double getTriggerConfidence() { return triggerConfidence; }
    public void setTriggerConfidence(Double triggerConfidence) { this.triggerConfidence = triggerConfidence; }
    
    public String getSpeechDirection() { return speechDirection; }
    public void setSpeechDirection(String speechDirection) { this.speechDirection = speechDirection; }
    
    public String getResponseStrategy() { return responseStrategy; }
    public void setResponseStrategy(String responseStrategy) { this.responseStrategy = responseStrategy; }
    
    public String getEmotionalResponseType() { return emotionalResponseType; }
    public void setEmotionalResponseType(String emotionalResponseType) { this.emotionalResponseType = emotionalResponseType; }
    
    public String getVoiceStyleUsed() { return voiceStyleUsed; }
    public void setVoiceStyleUsed(String voiceStyleUsed) { this.voiceStyleUsed = voiceStyleUsed; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    
    public void markAsProcessed() {
        this.processedAt = LocalDateTime.now();
    }
    
    public boolean shouldTriggerResponse() {
        if (messageType != MessageType.USER_INPUT) return false;
        if (isDirectedToSystem == null || !isDirectedToSystem) return false;
        if (triggerConfidence == null) return false;
        return triggerConfidence > 0.85;
    }
}
