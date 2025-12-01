package com.example.voicebox.app.device.service.emotional;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 情感强度控制服务
 * 计算和控制情感表达的强度
 */
@Service
public class EmotionIntensityController {
    
    private static final Logger logger = LoggerFactory.getLogger(EmotionIntensityController.class);
    
    // 强度范围限制
    private static final double MIN_INTENSITY = 0.0;
    private static final double MAX_INTENSITY = 1.0;
    
    // 自动限制阈值
    private static final double AUTO_LIMIT_THRESHOLD = 0.8;
    
    /**
     * 计算情感强度值
     * 需求: 11.1
     */
    public double calculateIntensity(VoiceFeatureAnalyzer.VoiceFeatures features,
                                    EmotionRecognitionService.EmotionType emotion) {
        logger.info("计算情感强度: emotion={}", emotion);
        
        if (features == null || emotion == null) {
            return 0.5; // 默认中等强度
        }
        
        double intensity = 0.5;
        
        // 基于语音特征计算强度
        Double energy = features.getEnergy();
        Double pitchStd = features.getPitchStd();
        Double speechRate = features.getSpeechRate();
        
        if (energy != null && pitchStd != null && speechRate != null) {
            // 能量贡献 (0-0.4)
            double energyContribution = Math.min(0.4, energy * 0.4);
            
            // 音高变化贡献 (0-0.3)
            double pitchContribution = Math.min(0.3, (pitchStd / 50.0) * 0.3);
            
            // 语速贡献 (0-0.3)
            double speedContribution = 0.0;
            if (speechRate > 4.0) {
                speedContribution = Math.min(0.3, ((speechRate - 4.0) / 2.0) * 0.3);
            }
            
            intensity = energyContribution + pitchContribution + speedContribution;
        }
        
        // 根据情绪类型调整
        intensity = adjustIntensityForEmotion(intensity, emotion);
        
        return limitIntensity(intensity);
    }
    
    /**
     * 根据情绪类型调整强度
     */
    private double adjustIntensityForEmotion(double baseIntensity, 
                                            EmotionRecognitionService.EmotionType emotion) {
        switch (emotion) {
            case HAPPY:
            case EXCITED:
                return baseIntensity * 1.1; // 增强积极情绪
            case ANGRY:
                return baseIntensity * 1.2; // 增强愤怒情绪
            case SAD:
                return baseIntensity * 0.9; // 减弱悲伤情绪
            case CALM:
                return baseIntensity * 0.7; // 减弱平静情绪
            case ANXIOUS:
                return baseIntensity * 1.0; // 保持焦虑情绪
            default:
                return baseIntensity;
        }
    }
    
    /**
     * 应用强度限制
     * 需求: 11.2
     */
    public double applyIntensityLimit(double intensity, double maxLimit) {
        logger.info("应用强度限制: intensity={}, maxLimit={}", intensity, maxLimit);
        
        if (maxLimit < MIN_INTENSITY || maxLimit > MAX_INTENSITY) {
            logger.warn("无效的最大限制值: {}", maxLimit);
            maxLimit = MAX_INTENSITY;
        }
        
        return Math.min(intensity, maxLimit);
    }
    
    /**
     * 根据强度调整语音参数
     * 需求: 11.3
     */
    public VoiceParameterAdjuster.AdjustedParameters adjustParametersForIntensity(
            double baseSpeed, double basePitch, double intensity) {
        logger.info("根据强度调整参数: intensity={}", intensity);
        
        VoiceParameterAdjuster.AdjustedParameters params = 
            new VoiceParameterAdjuster.AdjustedParameters();
        
        // 强度越高，参数变化越大
        double speedMultiplier = 1.0 + (intensity - 0.5) * 0.2;
        double pitchMultiplier = 1.0 + (intensity - 0.5) * 0.2;
        
        params.setSpeed(baseSpeed * speedMultiplier);
        params.setPitch(basePitch * pitchMultiplier);
        params.setVolume(0.8 + intensity * 0.2); // 音量随强度增加
        
        // 限制在合理范围内
        params.setSpeed(Math.max(0.5, Math.min(2.0, params.getSpeed())));
        params.setPitch(Math.max(0.5, Math.min(2.0, params.getPitch())));
        params.setVolume(Math.max(0.0, Math.min(1.0, params.getVolume())));
        
        return params;
    }
    
    /**
     * 检查是否需要自动限制
     * 需求: 11.4
     */
    public boolean shouldAutoLimit(double intensity) {
        return intensity > AUTO_LIMIT_THRESHOLD;
    }
    
    /**
     * 自动限制过高的强度
     * 需求: 11.5
     */
    public double autoLimitIntensity(double intensity) {
        logger.info("自动限制强度: original={}", intensity);
        
        if (intensity > AUTO_LIMIT_THRESHOLD) {
            double limited = AUTO_LIMIT_THRESHOLD + 
                           (intensity - AUTO_LIMIT_THRESHOLD) * 0.5;
            logger.info("强度已自动限制: {} -> {}", intensity, limited);
            return limitIntensity(limited);
        }
        
        return intensity;
    }
    
    /**
     * 限制强度在有效范围内
     */
    private double limitIntensity(double intensity) {
        return Math.max(MIN_INTENSITY, Math.min(MAX_INTENSITY, intensity));
    }
    
    /**
     * 获取强度描述
     */
    public String getIntensityDescription(double intensity) {
        if (intensity < 0.2) {
            return "非常弱";
        } else if (intensity < 0.4) {
            return "弱";
        } else if (intensity < 0.6) {
            return "中等";
        } else if (intensity < 0.8) {
            return "强";
        } else {
            return "非常强";
        }
    }
    
    /**
     * 情感强度结果类
     */
    public static class IntensityResult {
        private double originalIntensity;
        private double adjustedIntensity;
        private boolean autoLimited;
        private String description;
        private VoiceParameterAdjuster.AdjustedParameters parameters;
        
        public IntensityResult() {
        }
        
        public IntensityResult(double originalIntensity, double adjustedIntensity, 
                             boolean autoLimited, String description) {
            this.originalIntensity = originalIntensity;
            this.adjustedIntensity = adjustedIntensity;
            this.autoLimited = autoLimited;
            this.description = description;
        }
        
        // Getters and Setters
        public double getOriginalIntensity() { return originalIntensity; }
        public void setOriginalIntensity(double originalIntensity) { 
            this.originalIntensity = originalIntensity; 
        }
        
        public double getAdjustedIntensity() { return adjustedIntensity; }
        public void setAdjustedIntensity(double adjustedIntensity) { 
            this.adjustedIntensity = adjustedIntensity; 
        }
        
        public boolean isAutoLimited() { return autoLimited; }
        public void setAutoLimited(boolean autoLimited) { 
            this.autoLimited = autoLimited; 
        }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { 
            this.description = description; 
        }
        
        public VoiceParameterAdjuster.AdjustedParameters getParameters() { 
            return parameters; 
        }
        public void setParameters(VoiceParameterAdjuster.AdjustedParameters parameters) { 
            this.parameters = parameters; 
        }
        
        @Override
        public String toString() {
            return String.format("IntensityResult{original=%.2f, adjusted=%.2f, autoLimited=%s, desc='%s'}",
                originalIntensity, adjustedIntensity, autoLimited, description);
        }
    }
    
    /**
     * 完整的强度控制流程
     */
    public IntensityResult processIntensity(VoiceFeatureAnalyzer.VoiceFeatures features,
                                           EmotionRecognitionService.EmotionType emotion,
                                           double baseSpeed, double basePitch,
                                           Double maxLimit) {
        // 计算原始强度
        double originalIntensity = calculateIntensity(features, emotion);
        
        // 应用最大限制（如果指定）
        double adjustedIntensity = originalIntensity;
        if (maxLimit != null) {
            adjustedIntensity = applyIntensityLimit(adjustedIntensity, maxLimit);
        }
        
        // 自动限制
        boolean autoLimited = shouldAutoLimit(adjustedIntensity);
        if (autoLimited) {
            adjustedIntensity = autoLimitIntensity(adjustedIntensity);
        }
        
        // 获取描述
        String description = getIntensityDescription(adjustedIntensity);
        
        // 创建结果
        IntensityResult result = new IntensityResult(
            originalIntensity, adjustedIntensity, autoLimited, description);
        
        // 调整参数
        VoiceParameterAdjuster.AdjustedParameters params = 
            adjustParametersForIntensity(baseSpeed, basePitch, adjustedIntensity);
        result.setParameters(params);
        
        return result;
    }
}
