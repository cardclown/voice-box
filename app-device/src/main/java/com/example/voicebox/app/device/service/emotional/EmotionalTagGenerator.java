package com.example.voicebox.app.device.service.emotional;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.time.LocalDateTime;

/**
 * 情感标签生成器
 */
@Service
public class EmotionalTagGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(EmotionalTagGenerator.class);
    
    public enum TagType {
        PERSONALITY, EMOTION, TONE, GENDER, BEHAVIOR, PREFERENCE
    }
    
    public enum TagPriority {
        HIGH, MEDIUM, LOW
    }
    
    public static class EmotionalTag {
        private String tagId;
        private String tagName;
        private TagType tagType;
        private double confidence;
        private TagPriority priority;
        private String description;
        private Map<String, Object> attributes = new HashMap<>();
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime updatedAt = LocalDateTime.now();
        private boolean isActive = true;
        private int frequency = 1;
        
        public EmotionalTag() {}
        
        public EmotionalTag(String tagName, TagType tagType, double confidence) {
            this.tagName = tagName;
            this.tagType = tagType;
            this.confidence = confidence;
            this.priority = confidence >= 0.7 ? TagPriority.HIGH : 
                           confidence >= 0.4 ? TagPriority.MEDIUM : TagPriority.LOW;
        }
        
        public String getTagId() { return tagId; }
        public String getTagName() { return tagName; }
        public TagType getTagType() { return tagType; }
        public double getConfidence() { return confidence; }
        public TagPriority getPriority() { return priority; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Map<String, Object> getAttributes() { return attributes; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public boolean isActive() { return isActive; }
        public int getFrequency() { return frequency; }
        public void incrementFrequency() { 
            this.frequency++; 
            this.updatedAt = LocalDateTime.now(); 
        }
    }
    
    public static class TagGenerationResult {
        private List<EmotionalTag> tags = new ArrayList<>();
        private Map<TagType, Integer> tagCounts = new HashMap<>();
        private double overallConfidence;
        private String summary;
        private LocalDateTime generatedAt = LocalDateTime.now();
        
        public List<EmotionalTag> getTags() { return tags; }
        public void setTags(List<EmotionalTag> tags) { this.tags = tags; }
        public Map<TagType, Integer> getTagCounts() { return tagCounts; }
        public double getOverallConfidence() { return overallConfidence; }
        public void setOverallConfidence(double c) { this.overallConfidence = c; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
    }
    
    public TagGenerationResult generateTags(VoiceFeatureAnalyzer.VoiceFeatures features, String text) {
        logger.info("开始生成情感标签");
        
        TagGenerationResult result = new TagGenerationResult();
        List<EmotionalTag> tags = new ArrayList<>();
        
        if (features.getSpeedWpm() != null && features.getSpeedWpm() > 180) {
            tags.add(new EmotionalTag("语速快", TagType.BEHAVIOR, 0.8));
        }
        
        if (features.getVolumeMean() != null && features.getVolumeMean() > 0.7) {
            tags.add(new EmotionalTag("声音洪亮", TagType.BEHAVIOR, 0.7));
        }
        
        result.setTags(tags);
        result.setOverallConfidence(0.7);
        result.setSummary("生成了" + tags.size() + "个标签");
        
        logger.info("情感标签生成完成，共{}个标签", tags.size());
        return result;
    }
}
