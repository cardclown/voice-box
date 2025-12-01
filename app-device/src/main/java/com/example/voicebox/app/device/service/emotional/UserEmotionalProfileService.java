package com.example.voicebox.app.device.service.emotional;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 用户情感画像服务
 */
@Service
public class UserEmotionalProfileService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserEmotionalProfileService.class);
    
    @Autowired
    private EmotionalTagGenerator tagGenerator;
    
    @Autowired
    private PersonalityRecognitionService personalityService;
    
    @Autowired
    private EmotionRecognitionService emotionService;
    
    @Autowired
    private ToneStyleAnalyzer toneAnalyzer;
    
    @Autowired
    private com.example.voicebox.app.device.repository.UserEmotionalProfileRepository profileRepository;
    
    private Map<String, List<AnalysisHistory>> userHistoryMap = new HashMap<>();
    private Map<String, UserEmotionalProfile> userProfileMap = new HashMap<>();
    
    public UserEmotionalProfile buildProfile(String userId, 
                                           VoiceFeatureAnalyzer.VoiceFeatures features, 
                                           String text) {
        logger.info("开始构建用户{}的情感画像", userId);
        
        addToHistory(userId, features, text);
        
        UserEmotionalProfile profile = userProfileMap.getOrDefault(userId, new UserEmotionalProfile());
        profile.setUserId(userId);
        profile.incrementAnalysisCount();
        
        List<AnalysisHistory> history = userHistoryMap.getOrDefault(userId, new ArrayList<>());
        
        profile.setPersonalityProfile(buildPersonalityProfile(history));
        profile.setEmotionProfile(buildEmotionProfile(history));
        profile.setBehaviorProfile(buildBehaviorProfile(history));
        
        List<EmotionalTagGenerator.EmotionalTag> tags = generateProfileTags(history);
        profile.setTags(tags);
        
        Map<String, Object> insights = generateInsights(profile);
        profile.setInsights(insights);
        
        double completeness = calculateCompleteness(profile, history.size());
        profile.setProfileCompleteness(completeness);
        
        userProfileMap.put(userId, profile);
        
        logger.info("用户{}的情感画像构建完成，完整度: {}%", userId, 
                   String.format("%.1f", completeness * 100));
        
        return profile;
    }
    
    private void addToHistory(String userId, VoiceFeatureAnalyzer.VoiceFeatures features, String text) {
        List<AnalysisHistory> history = userHistoryMap.getOrDefault(userId, new ArrayList<>());
        AnalysisHistory record = new AnalysisHistory(features, text);
        
        try {
            record.getAnalysisResults().put("personality", 
                personalityService.analyzePersonality(features, text));
            record.getAnalysisResults().put("emotion", 
                emotionService.recognizeEmotion(features, text));
            record.getAnalysisResults().put("tone", 
                toneAnalyzer.analyzeTone(features, text));
        } catch (Exception e) {
            logger.warn("分析历史数据时出错: {}", e.getMessage());
        }
        
        history.add(record);
        if (history.size() > 100) {
            history = history.subList(history.size() - 100, history.size());
        }
        userHistoryMap.put(userId, history);
    }
    
    private PersonalityProfile buildPersonalityProfile(List<AnalysisHistory> history) {
        PersonalityProfile profile = new PersonalityProfile();
        if (history.isEmpty()) return profile;
        
        List<PersonalityRecognitionService.PersonalityTraits> results = history.stream()
            .map(h -> (PersonalityRecognitionService.PersonalityTraits) 
                     h.getAnalysisResults().get("personality"))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        if (results.isEmpty()) return profile;
        
        profile.setExtroversionScore(results.stream()
            .mapToDouble(PersonalityRecognitionService.PersonalityTraits::getExtroversionScore)
            .average().orElse(0.5));
        
        profile.setRationalityScore(results.stream()
            .mapToDouble(PersonalityRecognitionService.PersonalityTraits::getRationalityScore)
            .average().orElse(0.5));
        
        Map<String, Long> typeCounts = results.stream()
            .map(PersonalityRecognitionService.PersonalityTraits::getPrimaryType)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(t -> t, Collectors.counting()));
        
        profile.setDominantType(typeCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null));
        
        profile.setStability(0.8);
        profile.setPersonalityTraits(Arrays.asList("外向", "理性"));
        
        return profile;
    }
    
    private EmotionProfile buildEmotionProfile(List<AnalysisHistory> history) {
        EmotionProfile profile = new EmotionProfile();
        if (history.isEmpty()) return profile;
        
        List<EmotionRecognitionService.EmotionResult> results = history.stream()
            .map(h -> (EmotionRecognitionService.EmotionResult) 
                     h.getAnalysisResults().get("emotion"))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        if (results.isEmpty()) return profile;
        
        Map<EmotionRecognitionService.EmotionType, Long> emotionCounts = results.stream()
            .map(EmotionRecognitionService.EmotionResult::getPrimaryEmotion)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(e -> e, Collectors.counting()));
        
        profile.setDominantEmotion(emotionCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null));
        
        profile.setAverageIntensity(results.stream()
            .mapToDouble(EmotionRecognitionService.EmotionResult::getIntensity)
            .average().orElse(0.5));
        
        profile.setEmotionalStability(0.7);
        
        return profile;
    }
    
    private BehaviorProfile buildBehaviorProfile(List<AnalysisHistory> history) {
        BehaviorProfile profile = new BehaviorProfile();
        if (history.isEmpty()) return profile;
        
        profile.setAverageSpeed(history.stream()
            .map(AnalysisHistory::getFeatures)
            .map(VoiceFeatureAnalyzer.VoiceFeatures::getSpeedWpm)
            .filter(Objects::nonNull)
            .mapToDouble(Integer::doubleValue)
            .average().orElse(150.0));
        
        profile.setAverageVolume(history.stream()
            .map(AnalysisHistory::getFeatures)
            .map(VoiceFeatureAnalyzer.VoiceFeatures::getVolumeMean)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .average().orElse(0.5));
        
        profile.setAverageTextLength(history.stream()
            .map(AnalysisHistory::getText)
            .filter(Objects::nonNull)
            .mapToInt(String::length)
            .average().orElse(50.0));
        
        return profile;
    }
    
    private List<EmotionalTagGenerator.EmotionalTag> generateProfileTags(List<AnalysisHistory> history) {
        if (history.isEmpty()) return new ArrayList<>();
        AnalysisHistory latest = history.get(history.size() - 1);
        return tagGenerator.generateTags(latest.getFeatures(), latest.getText()).getTags();
    }
    
    private Map<String, Object> generateInsights(UserEmotionalProfile profile) {
        Map<String, Object> insights = new HashMap<>();
        PersonalityProfile personality = profile.getPersonalityProfile();
        if (personality != null) {
            if (personality.getExtroversionScore() > 0.7) {
                insights.put("personality_insight", "用户表现出明显的外向特征");
            }
        }
        return insights;
    }
    
    private double calculateCompleteness(UserEmotionalProfile profile, int historySize) {
        double completeness = 0.0;
        if (profile.getPersonalityProfile() != null) completeness += 0.3;
        if (profile.getEmotionProfile() != null) completeness += 0.3;
        if (profile.getBehaviorProfile() != null) completeness += 0.2;
        if (!profile.getTags().isEmpty()) completeness += 0.2;
        return completeness * Math.min(1.0, historySize / 10.0);
    }
    
    public UserEmotionalProfile getProfile(String userId) {
        // 先从内存缓存获取
        UserEmotionalProfile cachedProfile = userProfileMap.get(userId);
        if (cachedProfile != null) {
            return cachedProfile;
        }
        
        // 从数据库读取
        try {
            Long userIdLong = Long.parseLong(userId);
            Optional<com.example.voicebox.app.device.domain.UserEmotionalProfile> dbProfile = 
                profileRepository.findByUserId(userIdLong);
            
            if (dbProfile.isPresent()) {
                // 创建简单的画像对象
                UserEmotionalProfile profile = new UserEmotionalProfile();
                profile.setUserId(userId);
                
                // 设置基本的性格画像
                PersonalityProfile personality = new PersonalityProfile();
                personality.setDominantType(dbProfile.get().getDominantPersonalityType());
                personality.setStability(dbProfile.get().getEmotionalStability() != null ? 
                    dbProfile.get().getEmotionalStability() : 0.5);
                profile.setPersonalityProfile(personality);
                
                // 缓存到内存
                userProfileMap.put(userId, profile);
                logger.info("从数据库加载用户{}的画像成功", userId);
                return profile;
            }
        } catch (Exception e) {
            logger.error("从数据库读取用户画像失败: {}", e.getMessage(), e);
        }
        
        return null;
    }
    
    public void clearHistory(String userId) {
        userHistoryMap.remove(userId);
        userProfileMap.remove(userId);
    }
    
    public static class UserEmotionalProfile {
        private String userId;
        private PersonalityProfile personalityProfile;
        private EmotionProfile emotionProfile;
        private BehaviorProfile behaviorProfile;
        private List<EmotionalTagGenerator.EmotionalTag> tags = new ArrayList<>();
        private Map<String, Object> insights = new HashMap<>();
        private double profileCompleteness;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime updatedAt = LocalDateTime.now();
        private int analysisCount = 0;
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public PersonalityProfile getPersonalityProfile() { return personalityProfile; }
        public void setPersonalityProfile(PersonalityProfile p) { this.personalityProfile = p; }
        public EmotionProfile getEmotionProfile() { return emotionProfile; }
        public void setEmotionProfile(EmotionProfile e) { this.emotionProfile = e; }
        public BehaviorProfile getBehaviorProfile() { return behaviorProfile; }
        public void setBehaviorProfile(BehaviorProfile b) { this.behaviorProfile = b; }
        public List<EmotionalTagGenerator.EmotionalTag> getTags() { return tags; }
        public void setTags(List<EmotionalTagGenerator.EmotionalTag> tags) { this.tags = tags; }
        public Map<String, Object> getInsights() { return insights; }
        public void setInsights(Map<String, Object> insights) { this.insights = insights; }
        public double getProfileCompleteness() { return profileCompleteness; }
        public void setProfileCompleteness(double c) { this.profileCompleteness = c; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public int getAnalysisCount() { return analysisCount; }
        public void setAnalysisCount(int count) { this.analysisCount = count; }
        public void incrementAnalysisCount() { 
            this.analysisCount++; 
            this.updatedAt = LocalDateTime.now(); 
        }
        
        // 便捷方法
        public int getTotalInteractions() { return analysisCount; }
        public LocalDateTime getLastUpdated() { return updatedAt; }
    }
    
    public static class PersonalityProfile {
        private double extroversionScore;
        private double rationalityScore;
        private String dominantType;
        private double stability;
        private List<String> personalityTraits = new ArrayList<>();
        
        public double getExtroversionScore() { return extroversionScore; }
        public void setExtroversionScore(double s) { this.extroversionScore = s; }
        public double getRationalityScore() { return rationalityScore; }
        public void setRationalityScore(double s) { this.rationalityScore = s; }
        public String getDominantType() { return dominantType; }
        public void setDominantType(String t) { this.dominantType = t; }
        public double getStability() { return stability; }
        public void setStability(double s) { this.stability = s; }
        public List<String> getPersonalityTraits() { return personalityTraits; }
        public void setPersonalityTraits(List<String> traits) { this.personalityTraits = traits; }
        
        // 便捷方法
        public String getDominantTrait() { return dominantType; }
    }
    
    public static class EmotionProfile {
        private EmotionRecognitionService.EmotionType dominantEmotion;
        private double averageIntensity;
        private double emotionalStability;
        
        public EmotionRecognitionService.EmotionType getDominantEmotion() { return dominantEmotion; }
        public void setDominantEmotion(EmotionRecognitionService.EmotionType e) { this.dominantEmotion = e; }
        public double getAverageIntensity() { return averageIntensity; }
        public void setAverageIntensity(double i) { this.averageIntensity = i; }
        public double getEmotionalStability() { return emotionalStability; }
        public void setEmotionalStability(double s) { this.emotionalStability = s; }
    }
    
    public static class BehaviorProfile {
        private double averageSpeed;
        private double averageVolume;
        private double averageTextLength;
        
        public double getAverageSpeed() { return averageSpeed; }
        public void setAverageSpeed(double s) { this.averageSpeed = s; }
        public double getAverageVolume() { return averageVolume; }
        public void setAverageVolume(double v) { this.averageVolume = v; }
        public double getAverageTextLength() { return averageTextLength; }
        public void setAverageTextLength(double l) { this.averageTextLength = l; }
    }
    
    public static class AnalysisHistory {
        private VoiceFeatureAnalyzer.VoiceFeatures features;
        private String text;
        private LocalDateTime timestamp = LocalDateTime.now();
        private Map<String, Object> analysisResults = new HashMap<>();
        
        public AnalysisHistory(VoiceFeatureAnalyzer.VoiceFeatures features, String text) {
            this.features = features;
            this.text = text;
        }
        
        public VoiceFeatureAnalyzer.VoiceFeatures getFeatures() { return features; }
        public String getText() { return text; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public Map<String, Object> getAnalysisResults() { return analysisResults; }
    }
}
