package com.example.voicebox.app.device.domain;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 语音消息实体
 */
@Data
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
}
