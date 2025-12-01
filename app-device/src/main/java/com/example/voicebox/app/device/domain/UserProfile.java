package com.example.voicebox.app.device.domain;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 用户画像实体
 * 存储用户的性格维度、偏好设置和统计信息
 */
public class UserProfile {
    
    private Long id;
    private Long userId;
    
    // 性格维度 (大五人格模型)
    private BigDecimal openness;              // 开放性 0-1
    private BigDecimal conscientiousness;     // 尽责性 0-1
    private BigDecimal extraversion;          // 外向性 0-1
    private BigDecimal agreeableness;         // 宜人性 0-1
    private BigDecimal neuroticism;           // 神经质 0-1
    
    // 偏好维度
    private String responseLengthPreference;  // concise/balanced/detailed
    private String languageStylePreference;   // formal/balanced/casual
    private String contentFormatPreference;   // JSON: ["lists", "code", "tables"]
    private String interactionStyle;          // active/balanced/passive
    
    // 统计信息
    private Integer totalMessages;
    private Integer totalSessions;
    private BigDecimal avgSessionDuration;
    private BigDecimal confidenceScore;
    
    // 时间戳
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp lastAnalyzedAt;
    
    // Constructors
    public UserProfile() {
        // 默认值
        this.openness = new BigDecimal("0.500");
        this.conscientiousness = new BigDecimal("0.500");
        this.extraversion = new BigDecimal("0.500");
        this.agreeableness = new BigDecimal("0.500");
        this.neuroticism = new BigDecimal("0.500");
        this.responseLengthPreference = "balanced";
        this.languageStylePreference = "balanced";
        this.interactionStyle = "balanced";
        this.totalMessages = 0;
        this.totalSessions = 0;
        this.confidenceScore = new BigDecimal("0.000");
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
    
    public BigDecimal getOpenness() {
        return openness;
    }
    
    public void setOpenness(BigDecimal openness) {
        this.openness = openness;
    }
    
    public BigDecimal getConscientiousness() {
        return conscientiousness;
    }
    
    public void setConscientiousness(BigDecimal conscientiousness) {
        this.conscientiousness = conscientiousness;
    }
    
    public BigDecimal getExtraversion() {
        return extraversion;
    }
    
    public void setExtraversion(BigDecimal extraversion) {
        this.extraversion = extraversion;
    }
    
    public BigDecimal getAgreeableness() {
        return agreeableness;
    }
    
    public void setAgreeableness(BigDecimal agreeableness) {
        this.agreeableness = agreeableness;
    }
    
    public BigDecimal getNeuroticism() {
        return neuroticism;
    }
    
    public void setNeuroticism(BigDecimal neuroticism) {
        this.neuroticism = neuroticism;
    }
    
    public String getResponseLengthPreference() {
        return responseLengthPreference;
    }
    
    public void setResponseLengthPreference(String responseLengthPreference) {
        this.responseLengthPreference = responseLengthPreference;
    }
    
    public String getLanguageStylePreference() {
        return languageStylePreference;
    }
    
    public void setLanguageStylePreference(String languageStylePreference) {
        this.languageStylePreference = languageStylePreference;
    }
    
    public String getContentFormatPreference() {
        return contentFormatPreference;
    }
    
    public void setContentFormatPreference(String contentFormatPreference) {
        this.contentFormatPreference = contentFormatPreference;
    }
    
    public String getInteractionStyle() {
        return interactionStyle;
    }
    
    public void setInteractionStyle(String interactionStyle) {
        this.interactionStyle = interactionStyle;
    }
    
    public Integer getTotalMessages() {
        return totalMessages;
    }
    
    public void setTotalMessages(Integer totalMessages) {
        this.totalMessages = totalMessages;
    }
    
    public Integer getTotalSessions() {
        return totalSessions;
    }
    
    public void setTotalSessions(Integer totalSessions) {
        this.totalSessions = totalSessions;
    }
    
    public BigDecimal getAvgSessionDuration() {
        return avgSessionDuration;
    }
    
    public void setAvgSessionDuration(BigDecimal avgSessionDuration) {
        this.avgSessionDuration = avgSessionDuration;
    }
    
    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }
    
    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
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
    
    public Timestamp getLastAnalyzedAt() {
        return lastAnalyzedAt;
    }
    
    public void setLastAnalyzedAt(Timestamp lastAnalyzedAt) {
        this.lastAnalyzedAt = lastAnalyzedAt;
    }
    
    /**
     * 获取性格类型（基于大五人格的简化分类）
     */
    public String getPersonalityType() {
        if (extraversion.compareTo(new BigDecimal("0.6")) > 0) {
            return "外向型";
        } else if (extraversion.compareTo(new BigDecimal("0.4")) < 0) {
            return "内向型";
        } else {
            return "平衡型";
        }
    }
    
    /**
     * 判断画像是否需要更新（基于消息数量和时间）
     */
    public boolean needsUpdate() {
        if (lastAnalyzedAt == null) {
            return true;
        }
        // 如果超过7天未分析，或者新增消息超过50条
        long daysSinceAnalysis = (System.currentTimeMillis() - lastAnalyzedAt.getTime()) / (1000 * 60 * 60 * 24);
        return daysSinceAnalysis > 7 || (totalMessages != null && totalMessages % 50 == 0);
    }
    
    /**
     * 判断画像置信度是否足够高
     */
    public boolean isConfident() {
        return confidenceScore != null && confidenceScore.compareTo(new BigDecimal("0.7")) >= 0;
    }
    
    @Override
    public String toString() {
        return "UserProfile{" +
                "id=" + id +
                ", userId=" + userId +
                ", openness=" + openness +
                ", conscientiousness=" + conscientiousness +
                ", extraversion=" + extraversion +
                ", agreeableness=" + agreeableness +
                ", neuroticism=" + neuroticism +
                ", confidenceScore=" + confidenceScore +
                '}';
    }
}
