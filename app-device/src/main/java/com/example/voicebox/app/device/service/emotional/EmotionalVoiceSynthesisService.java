package com.example.voicebox.app.device.service.emotional;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.time.LocalDateTime;

@Service
public class EmotionalVoiceSynthesisService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmotionalVoiceSynthesisService.class);
    
    @Autowired
    private UserEmotionalProfileService profileService;
    
    @Autowired
    private EmotionRecognitionService emotionService;
    
    public static class VoiceSynthesisParams {
        private double speed = 1.0;
        private double pitch = 1.0;
        private double volume = 1.0;
        private String emotion = "neutral";
        private double emotionIntensity = 0.5;
        private String voiceStyle = "default";
        private String gender = "female";
        private Map<String, Object> customParams = new HashMap<>();
        
        public VoiceSynthesisParams() {}
        
        public double getSpeed() { return speed; }
        public void setSpeed(double speed) { this.speed = Math.max(0.5, Math.min(2.0, speed)); }
        
        public double getPitch() { return pitch; }
        public void setPitch(double pitch) { this.pitch = Math.max(0.5, Math.min(2.0, pitch)); }
        
        public double getVolume() { return volume; }
        public void setVolume(double volume) { this.volume = Math.max(0.1, Math.min(1.0, volume)); }
        
        public String getEmotion() { return emotion; }
        public void setEmotion(String emotion) { this.emotion = emotion; }
        
        public double getEmotionIntensity() { return emotionIntensity; }
        public void setEmotionIntensity(double intensity) { 
            this.emotionIntensity = Math.max(0.0, Math.min(1.0, intensity)); 
        }
        
        public String getVoiceStyle() { return voiceStyle; }
        public void setVoiceStyle(String voiceStyle) { this.voiceStyle = voiceStyle; }
        
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        
        public Map<String, Object> getCustomParams() { return customParams; }
        public void setCustomParams(Map<String, Object> customParams) { 
            this.customParams = customParams; 
        }
    }
    
    public static class VoiceSynthesisResult {
        private byte[] audioData;
        private String audioFormat = "wav";
        private VoiceSynthesisParams params;
        private double duration;
        private String text;
        private LocalDateTime createdAt;
        private Map<String, Object> metadata;
        
        public VoiceSynthesisResult() {
            this.createdAt = LocalDateTime.now();
            this.metadata = new HashMap<>();
        }
        
        public byte[] getAudioData() { return audioData; }
        public void setAudioData(byte[] audioData) { this.audioData = audioData; }
        
        public String getAudioFormat() { return audioFormat; }
        public void setAudioFormat(String audioFormat) { this.audioFormat = audioFormat; }
        
        public VoiceSynthesisParams getParams() { return params; }
        public void setParams(VoiceSynthesisParams params) { this.params = params; }
        
        public double getDuration() { return duration; }
        public void setDuration(double duration) { this.duration = duration; }
        
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
    
    public VoiceSynthesisResult synthesizeEmotionalVoice(String userId, String text, 
                                                        EmotionRecognitionService.EmotionType targetEmotion) {
        logger.info("开始为用户{}生成情感化语音", userId);
        
        UserEmotionalProfileService.UserEmotionalProfile profile = profileService.getProfile(userId);
        VoiceSynthesisParams params = generateSynthesisParams(profile, targetEmotion);
        
        return performVoiceSynthesis(text, params);
    }
    
    public VoiceSynthesisResult synthesizeVoice(String text, VoiceSynthesisParams params) {
        logger.info("开始生成语音");
        return performVoiceSynthesis(text, params);
    }
    
    private VoiceSynthesisParams generateSynthesisParams(
            UserEmotionalProfileService.UserEmotionalProfile profile, 
            EmotionRecognitionService.EmotionType targetEmotion) {
        
        VoiceSynthesisParams params = new VoiceSynthesisParams();
        
        if (targetEmotion != null) {
            params.setEmotion(targetEmotion.name().toLowerCase());
            
            switch (targetEmotion) {
                case HAPPY:
                    params.setSpeed(1.05);
                    params.setPitch(1.1);
                    params.setEmotionIntensity(0.7);
                    break;
                case SAD:
                    params.setSpeed(0.9);
                    params.setPitch(0.9);
                    params.setVolume(0.9);
                    params.setEmotionIntensity(0.6);
                    break;
                case ANGRY:
                    params.setSpeed(1.1);
                    params.setPitch(1.15);
                    params.setVolume(1.1);
                    params.setEmotionIntensity(0.8);
                    break;
                case CALM:
                    params.setSpeed(0.95);
                    params.setPitch(0.98);
                    params.setEmotionIntensity(0.4);
                    break;
                case EXCITED:
                    params.setSpeed(1.15);
                    params.setPitch(1.2);
                    params.setVolume(1.05);
                    params.setEmotionIntensity(0.9);
                    break;
                default:
                    params.setEmotionIntensity(0.5);
                    break;
            }
        }
        
        return params;
    }
    
    private VoiceSynthesisResult performVoiceSynthesis(String text, VoiceSynthesisParams params) {
        logger.info("执行语音合成");
        
        VoiceSynthesisResult result = new VoiceSynthesisResult();
        result.setText(text);
        result.setParams(params);
        
        byte[] audioData = simulateVoiceSynthesis(text, params);
        
        result.setAudioData(audioData);
        result.setAudioFormat("wav");
        result.setDuration(estimateAudioDuration(text, params.getSpeed()));
        
        result.getMetadata().put("textLength", text.length());
        result.getMetadata().put("synthesisTime", System.currentTimeMillis());
        
        return result;
    }
    
    private byte[] simulateVoiceSynthesis(String text, VoiceSynthesisParams params) {
        logger.info("模拟语音合成");
        
        int textLength = text.length();
        int audioDataSize = textLength * 100;
        
        byte[] audioData = new byte[audioDataSize + 44];
        
        String wavHeader = "RIFF";
        System.arraycopy(wavHeader.getBytes(), 0, audioData, 0, 4);
        
        Random random = new Random();
        for (int i = 44; i < audioData.length; i++) {
            double emotionFactor = params.getEmotionIntensity();
            audioData[i] = (byte) (random.nextGaussian() * 50 * emotionFactor);
        }
        
        return audioData;
    }
    
    private double estimateAudioDuration(String text, double speed) {
        double baseWordsPerMinute = 150.0;
        double adjustedWPM = baseWordsPerMinute * speed;
        double minutes = text.length() / adjustedWPM;
        return minutes * 60.0;
    }
    
    public VoiceSynthesisParams getRecommendedParams(String userId, Map<String, Object> context) {
        logger.info("获取用户{}的推荐合成参数", userId);
        
        try {
            UserEmotionalProfileService.UserEmotionalProfile profile = profileService.getProfile(userId);
            return generateSynthesisParams(profile, null);
        } catch (Exception e) {
            logger.warn("获取推荐参数失败，使用默认参数: {}", e.getMessage());
            return new VoiceSynthesisParams();
        }
    }
    
    public List<VoiceSynthesisResult> batchSynthesize(String userId, List<String> texts, 
                                                     EmotionRecognitionService.EmotionType targetEmotion) {
        logger.info("开始批量合成语音，用户: {}, 文本数量: {}", userId, texts.size());
        
        List<VoiceSynthesisResult> results = new ArrayList<>();
        
        UserEmotionalProfileService.UserEmotionalProfile profile = profileService.getProfile(userId);
        VoiceSynthesisParams baseParams = generateSynthesisParams(profile, targetEmotion);
        
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            try {
                VoiceSynthesisResult result = performVoiceSynthesis(text, baseParams);
                result.getMetadata().put("batchIndex", i);
                results.add(result);
            } catch (Exception e) {
                logger.error("批量合成第{}个文本失败: {}", i, e.getMessage());
            }
        }
        
        logger.info("批量合成完成，成功: {}/{}", results.size(), texts.size());
        return results;
    }
}
