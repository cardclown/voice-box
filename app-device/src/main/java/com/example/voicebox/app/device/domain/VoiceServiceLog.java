package com.example.voicebox.app.device.domain;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 语音服务调用日志实体类
 * 
 * @author VoiceBox Team
 * @since 1.5
 */
@Entity
@Table(name = "voice_service_logs")
public class VoiceServiceLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id")
    private Long userId;
    
    // 服务信息
    @Column(name = "service_type", nullable = false, length = 20)
    private String serviceType; // STT/TTS
    
    @Column(name = "provider", nullable = false, length = 50)
    private String provider; // aliyun/tencent/azure
    
    @Column(name = "request_id", length = 255)
    private String requestId;
    
    // 请求响应信息
    @Column(name = "input_size")
    private Long inputSize;
    
    @Column(name = "output_size")
    private Long outputSize;
    
    @Column(name = "duration_ms")
    private Integer durationMs;
    
    // 状态信息
    @Column(name = "status", nullable = false, length = 20)
    private String status; // success/failed/timeout
    
    @Column(name = "error_code", length = 50)
    private String errorCode;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    // 成本信息
    @Column(name = "cost", precision = 10, scale = 6)
    private BigDecimal cost;
    
    // 时间戳
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
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
    
    public String getServiceType() {
        return serviceType;
    }
    
    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public Long getInputSize() {
        return inputSize;
    }
    
    public void setInputSize(Long inputSize) {
        this.inputSize = inputSize;
    }
    
    public Long getOutputSize() {
        return outputSize;
    }
    
    public void setOutputSize(Long outputSize) {
        this.outputSize = outputSize;
    }
    
    public Integer getDurationMs() {
        return durationMs;
    }
    
    public void setDurationMs(Integer durationMs) {
        this.durationMs = durationMs;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public BigDecimal getCost() {
        return cost;
    }
    
    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
