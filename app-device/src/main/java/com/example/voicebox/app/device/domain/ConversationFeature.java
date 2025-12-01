package com.example.voicebox.app.device.domain;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 对话特征实体
 * 存储从用户消息中提取的各种特征
 */
public class ConversationFeature {
    
    private Long id;
    private Long userId;
    private Long sessionId;
    private Long messageId;
    
    // 语言学特征
    private Integer messageLength;
    private Integer wordCount;
    private Integer sentenceCount;
    private BigDecimal avgWordLength;
    private BigDecimal vocabularyRichness;
    
    // 语义特征
    private String topics;              // JSON: ["tech", "life"]
    private BigDecimal sentimentScore;  // -1 到 1
    private String intent;
    private String keywords;            // JSON: ["keyword1", "keyword2"]
    
    // 对话模式
    private Integer questionCount;
    private Integer exclamationCount;
    private Integer emojiCount;
    private Integer codeBlockCount;
    
    private Timestamp createdAt;
    
    // Constructors
    public ConversationFeature() {
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
    
    public Integer getMessageLength() {
        return messageLength;
    }
    
    public void setMessageLength(Integer messageLength) {
        this.messageLength = messageLength;
    }
    
    public Integer getWordCount() {
        return wordCount;
    }
    
    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }
    
    public Integer getSentenceCount() {
        return sentenceCount;
    }
    
    public void setSentenceCount(Integer sentenceCount) {
        this.sentenceCount = sentenceCount;
    }
    
    public BigDecimal getAvgWordLength() {
        return avgWordLength;
    }
    
    public void setAvgWordLength(BigDecimal avgWordLength) {
        this.avgWordLength = avgWordLength;
    }
    
    public BigDecimal getVocabularyRichness() {
        return vocabularyRichness;
    }
    
    public void setVocabularyRichness(BigDecimal vocabularyRichness) {
        this.vocabularyRichness = vocabularyRichness;
    }
    
    public String getTopics() {
        return topics;
    }
    
    public void setTopics(String topics) {
        this.topics = topics;
    }
    
    public BigDecimal getSentimentScore() {
        return sentimentScore;
    }
    
    public void setSentimentScore(BigDecimal sentimentScore) {
        this.sentimentScore = sentimentScore;
    }
    
    public String getIntent() {
        return intent;
    }
    
    public void setIntent(String intent) {
        this.intent = intent;
    }
    
    public String getKeywords() {
        return keywords;
    }
    
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }
    
    public Integer getQuestionCount() {
        return questionCount;
    }
    
    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }
    
    public Integer getExclamationCount() {
        return exclamationCount;
    }
    
    public void setExclamationCount(Integer exclamationCount) {
        this.exclamationCount = exclamationCount;
    }
    
    public Integer getEmojiCount() {
        return emojiCount;
    }
    
    public void setEmojiCount(Integer emojiCount) {
        this.emojiCount = emojiCount;
    }
    
    public Integer getCodeBlockCount() {
        return codeBlockCount;
    }
    
    public void setCodeBlockCount(Integer codeBlockCount) {
        this.codeBlockCount = codeBlockCount;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * 判断是否为问题
     */
    public boolean isQuestion() {
        return questionCount != null && questionCount > 0;
    }
    
    /**
     * 判断是否为积极情绪
     */
    public boolean isPositiveSentiment() {
        return sentimentScore != null && sentimentScore.compareTo(new BigDecimal("0.3")) > 0;
    }
    
    /**
     * 判断是否为消极情绪
     */
    public boolean isNegativeSentiment() {
        return sentimentScore != null && sentimentScore.compareTo(new BigDecimal("-0.3")) < 0;
    }
    
    /**
     * 判断是否情感表达丰富
     */
    public boolean isEmotionallyExpressive() {
        int totalEmotionalMarkers = (exclamationCount != null ? exclamationCount : 0) + 
                                   (emojiCount != null ? emojiCount : 0);
        return totalEmotionalMarkers > 2;
    }
    
    /**
     * 获取主题列表（从 JSON 字符串解析）
     */
    public java.util.List<String> getTopicList() {
        if (topics == null || topics.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        // 简单的 JSON 数组解析
        String cleaned = topics.replaceAll("[\\[\\]\"]", "");
        return java.util.Arrays.asList(cleaned.split(","));
    }
    
    /**
     * 设置主题列表（转换为 JSON 字符串）
     */
    public void setTopicList(java.util.List<String> topicList) {
        if (topicList == null || topicList.isEmpty()) {
            this.topics = "[]";
        } else {
            this.topics = "[\"" + String.join("\",\"", topicList) + "\"]";
        }
    }
    
    /**
     * 获取关键词列表（从 JSON 字符串解析）
     */
    public java.util.List<String> getKeywordList() {
        if (keywords == null || keywords.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        String cleaned = keywords.replaceAll("[\\[\\]\"]", "");
        return java.util.Arrays.asList(cleaned.split(","));
    }
    
    /**
     * 设置关键词列表（转换为 JSON 字符串）
     */
    public void setKeywordList(java.util.List<String> keywordList) {
        if (keywordList == null || keywordList.isEmpty()) {
            this.keywords = "[]";
        } else {
            this.keywords = "[\"" + String.join("\",\"", keywordList) + "\"]";
        }
    }
    
    @Override
    public String toString() {
        return "ConversationFeature{" +
                "id=" + id +
                ", userId=" + userId +
                ", sessionId=" + sessionId +
                ", messageId=" + messageId +
                ", messageLength=" + messageLength +
                ", sentimentScore=" + sentimentScore +
                '}';
    }
}
