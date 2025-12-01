package com.example.voicebox.app.device.domain;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 语音消息实体
 */
@Entity
@Table(name = "voice_messages")
public class VoiceMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "file_id", nullable = false)
    private String fileId;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Column(name = "format", nullable = false, length = 20)
    private String format;

    @Column(name = "sample_rate")
    private Integer sampleRate;

    @Column(name = "recognized_text", columnDefinition = "TEXT")
    private String recognizedText;

    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "language", length = 10)
    private String language;

    @Column(name = "is_input")
    private Boolean isInput;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Manual getters and setters (Lombok not working)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    
    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    
    public Integer getSampleRate() { return sampleRate; }
    public void setSampleRate(Integer sampleRate) { this.sampleRate = sampleRate; }
    
    public String getRecognizedText() { return recognizedText; }
    public void setRecognizedText(String recognizedText) { this.recognizedText = recognizedText; }
    
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public Boolean getIsInput() { return isInput; }
    public void setIsInput(Boolean isInput) { this.isInput = isInput; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
