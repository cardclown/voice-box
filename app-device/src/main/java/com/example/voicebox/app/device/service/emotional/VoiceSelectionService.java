package com.example.voicebox.app.device.service.emotional;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/**
 * 音色个性化选择服务
 * 根据用户的性别、性格特征选择合适的音色
 */
@Service
public class VoiceSelectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(VoiceSelectionService.class);
    
    // 音色配置
    private static final Map<String, VoiceConfig> VOICE_CONFIGS = new HashMap<>();
    
    static {
        // 男性音色
        VOICE_CONFIGS.put("male_warm", new VoiceConfig("male_warm", "MALE", "温暖", 
            Arrays.asList("外向", "友好", "开朗"), 1.0, 1.0));
        VOICE_CONFIGS.put("male_professional", new VoiceConfig("male_professional", "MALE", "专业", 
            Arrays.asList("理性", "严谨", "正式"), 0.9, 1.1));
        VOICE_CONFIGS.put("male_energetic", new VoiceConfig("male_energetic", "MALE", "活力", 
            Arrays.asList("外向", "兴奋", "积极"), 1.2, 1.2));
        VOICE_CONFIGS.put("male_calm", new VoiceConfig("male_calm", "MALE", "沉稳", 
            Arrays.asList("内向", "平静", "理性"), 0.8, 0.9));
        
        // 女性音色
        VOICE_CONFIGS.put("female_sweet", new VoiceConfig("female_sweet", "FEMALE", "甜美", 
            Arrays.asList("外向", "友好", "温柔"), 1.0, 1.1));
        VOICE_CONFIGS.put("female_professional", new VoiceConfig("female_professional", "FEMALE", "专业", 
            Arrays.asList("理性", "严谨", "正式"), 0.9, 1.0));
        VOICE_CONFIGS.put("female_energetic", new VoiceConfig("female_energetic", "FEMALE", "活力", 
            Arrays.asList("外向", "兴奋", "积极"), 1.2, 1.2));
        VOICE_CONFIGS.put("female_gentle", new VoiceConfig("female_gentle", "FEMALE", "温柔", 
            Arrays.asList("内向", "平静", "感性"), 0.8, 1.0));
        
        // 中性音色
        VOICE_CONFIGS.put("neutral_standard", new VoiceConfig("neutral_standard", "NEUTRAL", "标准", 
            Arrays.asList("中性", "平衡"), 1.0, 1.0));
    }
    
    /**
     * 根据性别选择音色
     * 需求: 9.1
     */
    public VoiceConfig selectVoiceByGender(String gender) {
        logger.info("根据性别选择音色: {}", gender);
        
        if (gender == null || gender.isEmpty()) {
            return VOICE_CONFIGS.get("neutral_standard");
        }
        
        String normalizedGender = gender.toUpperCase();
        
        // 根据性别返回默认音色
        switch (normalizedGender) {
            case "MALE":
                return VOICE_CONFIGS.get("male_warm");
            case "FEMALE":
                return VOICE_CONFIGS.get("female_sweet");
            default:
                return VOICE_CONFIGS.get("neutral_standard");
        }
    }
    
    /**
     * 根据性格特征选择音色
     * 需求: 9.2
     */
    public VoiceConfig selectVoiceByPersonality(String gender, 
                                                PersonalityRecognitionService.PersonalityTraits traits) {
        logger.info("根据性格特征选择音色: gender={}, traits={}", gender, traits);
        
        if (traits == null) {
            return selectVoiceByGender(gender);
        }
        
        String normalizedGender = gender != null ? gender.toUpperCase() : "NEUTRAL";
        
        // 根据性格特征选择音色
        if (normalizedGender.equals("MALE")) {
            if (traits.getExtroversionScore() > 0.7) {
                return VOICE_CONFIGS.get("male_energetic");
            } else if (traits.getExtroversionScore() < 0.3) {
                return VOICE_CONFIGS.get("male_calm");
            } else if (traits.getRationalityScore() > 0.7) {
                return VOICE_CONFIGS.get("male_professional");
            } else {
                return VOICE_CONFIGS.get("male_warm");
            }
        } else if (normalizedGender.equals("FEMALE")) {
            if (traits.getExtroversionScore() > 0.7) {
                return VOICE_CONFIGS.get("female_energetic");
            } else if (traits.getExtroversionScore() < 0.3) {
                return VOICE_CONFIGS.get("female_gentle");
            } else if (traits.getRationalityScore() > 0.7) {
                return VOICE_CONFIGS.get("female_professional");
            } else {
                return VOICE_CONFIGS.get("female_sweet");
            }
        } else {
            return VOICE_CONFIGS.get("neutral_standard");
        }
    }
    
    /**
     * 根据情绪调整音色参数
     * 需求: 9.3
     */
    public VoiceConfig adjustVoiceForEmotion(VoiceConfig baseVoice, 
                                            EmotionRecognitionService.EmotionType emotion) {
        logger.info("根据情绪调整音色: baseVoice={}, emotion={}", baseVoice.getVoiceId(), emotion);
        
        if (baseVoice == null || emotion == null) {
            return baseVoice;
        }
        
        VoiceConfig adjusted = new VoiceConfig(baseVoice);
        
        // 根据情绪调整语速和音调
        switch (emotion) {
            case HAPPY:
            case EXCITED:
                adjusted.setDefaultSpeed(baseVoice.getDefaultSpeed() * 1.1);
                adjusted.setDefaultPitch(baseVoice.getDefaultPitch() * 1.1);
                break;
            case SAD:
                adjusted.setDefaultSpeed(baseVoice.getDefaultSpeed() * 0.9);
                adjusted.setDefaultPitch(baseVoice.getDefaultPitch() * 0.9);
                break;
            case ANGRY:
                adjusted.setDefaultSpeed(baseVoice.getDefaultSpeed() * 1.2);
                adjusted.setDefaultPitch(baseVoice.getDefaultPitch() * 1.2);
                break;
            case CALM:
                adjusted.setDefaultSpeed(baseVoice.getDefaultSpeed() * 0.95);
                adjusted.setDefaultPitch(baseVoice.getDefaultPitch() * 1.0);
                break;
            case ANXIOUS:
                adjusted.setDefaultSpeed(baseVoice.getDefaultSpeed() * 1.15);
                adjusted.setDefaultPitch(baseVoice.getDefaultPitch() * 1.05);
                break;
            default:
                // 保持原样
                break;
        }
        
        return adjusted;
    }
    
    /**
     * 获取所有可用音色
     * 需求: 9.4
     */
    public List<VoiceConfig> getAllVoices() {
        return new ArrayList<>(VOICE_CONFIGS.values());
    }
    
    /**
     * 根据音色ID获取配置
     * 需求: 9.4
     */
    public VoiceConfig getVoiceById(String voiceId) {
        return VOICE_CONFIGS.get(voiceId);
    }
    
    /**
     * 获取推荐音色列表
     * 需求: 9.5
     */
    public List<VoiceConfig> getRecommendedVoices(String gender, 
                                                  PersonalityRecognitionService.PersonalityTraits traits) {
        logger.info("获取推荐音色列表: gender={}, traits={}", gender, traits);
        
        List<VoiceConfig> recommended = new ArrayList<>();
        
        // 首选音色
        VoiceConfig primary = selectVoiceByPersonality(gender, traits);
        recommended.add(primary);
        
        // 添加同性别的其他音色
        String normalizedGender = gender != null ? gender.toUpperCase() : "NEUTRAL";
        for (VoiceConfig config : VOICE_CONFIGS.values()) {
            if (config.getGender().equals(normalizedGender) && 
                !config.getVoiceId().equals(primary.getVoiceId())) {
                recommended.add(config);
            }
        }
        
        // 如果推荐列表少于3个，添加中性音色
        if (recommended.size() < 3) {
            VoiceConfig neutral = VOICE_CONFIGS.get("neutral_standard");
            if (!recommended.contains(neutral)) {
                recommended.add(neutral);
            }
        }
        
        return recommended;
    }
    
    /**
     * 音色配置类
     */
    public static class VoiceConfig {
        private String voiceId;
        private String gender;
        private String style;
        private List<String> suitableTraits;
        private double defaultSpeed;
        private double defaultPitch;
        
        public VoiceConfig(String voiceId, String gender, String style, 
                          List<String> suitableTraits, double defaultSpeed, double defaultPitch) {
            this.voiceId = voiceId;
            this.gender = gender;
            this.style = style;
            this.suitableTraits = suitableTraits;
            this.defaultSpeed = defaultSpeed;
            this.defaultPitch = defaultPitch;
        }
        
        // 复制构造函数
        public VoiceConfig(VoiceConfig other) {
            this.voiceId = other.voiceId;
            this.gender = other.gender;
            this.style = other.style;
            this.suitableTraits = new ArrayList<>(other.suitableTraits);
            this.defaultSpeed = other.defaultSpeed;
            this.defaultPitch = other.defaultPitch;
        }
        
        // Getters and Setters
        public String getVoiceId() { return voiceId; }
        public void setVoiceId(String voiceId) { this.voiceId = voiceId; }
        
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        
        public String getStyle() { return style; }
        public void setStyle(String style) { this.style = style; }
        
        public List<String> getSuitableTraits() { return suitableTraits; }
        public void setSuitableTraits(List<String> suitableTraits) { 
            this.suitableTraits = suitableTraits; 
        }
        
        public double getDefaultSpeed() { return defaultSpeed; }
        public void setDefaultSpeed(double defaultSpeed) { 
            this.defaultSpeed = Math.max(0.5, Math.min(2.0, defaultSpeed)); 
        }
        
        public double getDefaultPitch() { return defaultPitch; }
        public void setDefaultPitch(double defaultPitch) { 
            this.defaultPitch = Math.max(0.5, Math.min(2.0, defaultPitch)); 
        }
        
        @Override
        public String toString() {
            return String.format("VoiceConfig{voiceId='%s', gender='%s', style='%s', speed=%.2f, pitch=%.2f}",
                voiceId, gender, style, defaultSpeed, defaultPitch);
        }
    }
    
    /**
     * 音色选择记录
     */
    public static class VoiceSelectionRecord {
        private String userId;
        private String voiceId;
        private String gender;
        private String reason;
        private Date timestamp;
        
        public VoiceSelectionRecord(String userId, String voiceId, String gender, String reason) {
            this.userId = userId;
            this.voiceId = voiceId;
            this.gender = gender;
            this.reason = reason;
            this.timestamp = new Date();
        }
        
        // Getters and Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getVoiceId() { return voiceId; }
        public void setVoiceId(String voiceId) { this.voiceId = voiceId; }
        
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    }
}
