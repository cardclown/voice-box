package com.example.voicebox.app.device.service.emotional;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 情绪识别服务
 * 
 * 基于语音特征和文本内容识别用户的情绪状态
 * 支持识别开心、悲伤、愤怒、平静、焦虑、兴奋、中性等情绪
 */
@Service
public class EmotionRecognitionService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmotionRecognitionService.class);
    
    /**
     * 情绪类型枚举
     */
    public enum EmotionType {
        HAPPY("开心"),
        SAD("悲伤"),
        ANGRY("愤怒"),
        CALM("平静"),
        ANXIOUS("焦虑"),
        EXCITED("兴奋"),
        NEUTRAL("中性");
        
        private final String displayName;
        
        EmotionType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 语气风格枚举
     */
    public enum ToneStyle {
        FORMAL("正式"),
        CASUAL("随意"),
        HUMOROUS("幽默"),
        SERIOUS("严肃");
        
        private final String displayName;
        
        ToneStyle(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 情绪识别结果
     */
    public static class EmotionResult {
        private EmotionType primaryEmotion;           // 主要情绪
        private double confidence;                     // 置信度 (0-1)
        private double intensity;                      // 强度 (0-1)
        private Map<EmotionType, Double> emotionScores; // 各情绪得分
        private ToneStyle toneStyle;                   // 语气风格
        private String description;                    // 描述
        
        public EmotionResult() {
            this.emotionScores = new HashMap<>();
        }
        
        // Getters and Setters
        public EmotionType getPrimaryEmotion() { return primaryEmotion; }
        public void setPrimaryEmotion(EmotionType primaryEmotion) { this.primaryEmotion = primaryEmotion; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        
        public double getIntensity() { return intensity; }
        public void setIntensity(double intensity) { this.intensity = intensity; }
        
        public Map<EmotionType, Double> getEmotionScores() { return emotionScores; }
        public void setEmotionScores(Map<EmotionType, Double> emotionScores) { this.emotionScores = emotionScores; }
        
        public ToneStyle getToneStyle() { return toneStyle; }
        public void setToneStyle(ToneStyle toneStyle) { this.toneStyle = toneStyle; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    /**
     * 识别情绪
     * 
     * @param features 语音特征
     * @param text 文本内容
     * @return 情绪识别结果
     */
    public EmotionResult recognizeEmotion(VoiceFeatureAnalyzer.VoiceFeatures features, String text) {
        logger.info("开始识别情绪");
        
        EmotionResult result = new EmotionResult();
        
        // 1. 基于语音特征计算各情绪得分
        Map<EmotionType, Double> scores = calculateEmotionScores(features, text);
        result.setEmotionScores(scores);
        
        // 2. 确定主要情绪
        EmotionType primaryEmotion = determinePrimaryEmotion(scores);
        result.setPrimaryEmotion(primaryEmotion);
        
        // 3. 计算置信度
        double confidence = calculateConfidence(features, scores);
        result.setConfidence(confidence);
        
        // 4. 计算情绪强度
        double intensity = calculateIntensity(features, primaryEmotion);
        result.setIntensity(intensity);
        
        // 5. 分析语气风格
        ToneStyle toneStyle = analyzeTone(features, text);
        result.setToneStyle(toneStyle);
        
        // 6. 生成描述
        String description = generateDescription(result);
        result.setDescription(description);
        
        logger.info("情绪识别完成: 情绪={}, 置信度={}, 强度={}", 
                   primaryEmotion.getDisplayName(), confidence, intensity);
        
        return result;
    }
    
    /**
     * 计算各情绪得分
     */
    private Map<EmotionType, Double> calculateEmotionScores(
            VoiceFeatureAnalyzer.VoiceFeatures features, String text) {
        
        Map<EmotionType, Double> scores = new HashMap<>();
        
        // 获取关键特征
        double pitchMean = (features.getPitchMean() != null) ? features.getPitchMean() : 150.0;
        double pitchStd = (features.getPitchStd() != null) ? features.getPitchStd() : 20.0;
        double volumeMean = (features.getVolumeMean() != null) ? features.getVolumeMean() : 0.5;
        double energyLevel = (features.getEnergyLevel() != null) ? features.getEnergyLevel() : 0.5;
        Integer speedWpm = features.getSpeedWpm();
        double speechRate = (speedWpm != null) ? speedWpm / 60.0 * 2.0 : 4.0;
        
        // 计算开心情绪得分
        double happyScore = calculateHappyScore(pitchMean, pitchStd, energyLevel, speechRate, text);
        scores.put(EmotionType.HAPPY, happyScore);
        
        // 计算悲伤情绪得分
        double sadScore = calculateSadScore(pitchMean, energyLevel, speechRate, text);
        scores.put(EmotionType.SAD, sadScore);
        
        // 计算愤怒情绪得分
        double angryScore = calculateAngryScore(pitchMean, volumeMean, energyLevel, speechRate, text);
        scores.put(EmotionType.ANGRY, angryScore);
        
        // 计算平静情绪得分
        double calmScore = calculateCalmScore(pitchStd, volumeMean, energyLevel, speechRate);
        scores.put(EmotionType.CALM, calmScore);
        
        // 计算焦虑情绪得分
        double anxiousScore = calculateAnxiousScore(pitchStd, speechRate, text);
        scores.put(EmotionType.ANXIOUS, anxiousScore);
        
        // 计算兴奋情绪得分
        double excitedScore = calculateExcitedScore(pitchMean, energyLevel, speechRate, text);
        scores.put(EmotionType.EXCITED, excitedScore);
        
        // 计算中性情绪得分
        double neutralScore = calculateNeutralScore(scores);
        scores.put(EmotionType.NEUTRAL, neutralScore);
        
        return scores;
    }
    
    /**
     * 计算开心情绪得分
     * 特征：音高较高、音高变化丰富、能量高、语速适中偏快
     */
    private double calculateHappyScore(double pitchMean, double pitchStd, 
                                      double energyLevel, double speechRate, String text) {
        double score = 0.0;
        
        // 音高因素 (权重: 0.3)
        if (pitchMean > 160.0) {
            score += 0.15 * Math.min((pitchMean - 160.0) / 40.0, 1.0);
        }
        
        // 音高变化因素 (权重: 0.2)
        if (pitchStd > 20.0) {
            score += 0.1 * Math.min((pitchStd - 20.0) / 15.0, 1.0);
        }
        
        // 能量因素 (权重: 0.3)
        if (energyLevel > 0.6) {
            score += 0.15 * Math.min((energyLevel - 0.6) / 0.3, 1.0);
        }
        
        // 语速因素 (权重: 0.2)
        if (speechRate >= 4.0 && speechRate <= 5.5) {
            score += 0.1;
        }
        
        // 文本情感词 (权重: 0.2)
        if (text != null && !text.isEmpty()) {
            String[] happyWords = {"开心", "高兴", "快乐", "哈哈", "太好了", "棒", "喜欢"};
            for (String word : happyWords) {
                if (text.contains(word)) {
                    score += 0.05;
                    break;
                }
            }
        }
        
        return Math.min(score, 1.0);
    }
    
    /**
     * 计算悲伤情绪得分
     * 特征：音高较低、能量低、语速慢
     */
    private double calculateSadScore(double pitchMean, double energyLevel, 
                                    double speechRate, String text) {
        double score = 0.0;
        
        // 音高因素 (权重: 0.3)
        if (pitchMean < 140.0) {
            score += 0.15 * Math.min((140.0 - pitchMean) / 30.0, 1.0);
        }
        
        // 能量因素 (权重: 0.4)
        if (energyLevel < 0.4) {
            score += 0.2 * (1.0 - energyLevel / 0.4);
        }
        
        // 语速因素 (权重: 0.3)
        if (speechRate < 3.5) {
            score += 0.15 * (1.0 - speechRate / 3.5);
        }
        
        // 文本情感词 (权重: 0.2)
        if (text != null && !text.isEmpty()) {
            String[] sadWords = {"难过", "伤心", "悲伤", "失望", "痛苦", "哭"};
            for (String word : sadWords) {
                if (text.contains(word)) {
                    score += 0.1;
                    break;
                }
            }
        }
        
        return Math.min(score, 1.0);
    }
    
    /**
     * 计算愤怒情绪得分
     * 特征：音高高、音量大、能量高、语速快
     */
    private double calculateAngryScore(double pitchMean, double volumeMean, 
                                      double energyLevel, double speechRate, String text) {
        double score = 0.0;
        
        // 音高因素 (权重: 0.25)
        if (pitchMean > 170.0) {
            score += 0.125 * Math.min((pitchMean - 170.0) / 30.0, 1.0);
        }
        
        // 音量因素 (权重: 0.25)
        if (volumeMean > 0.7) {
            score += 0.125 * Math.min((volumeMean - 0.7) / 0.2, 1.0);
        }
        
        // 能量因素 (权重: 0.25)
        if (energyLevel > 0.7) {
            score += 0.125 * Math.min((energyLevel - 0.7) / 0.2, 1.0);
        }
        
        // 语速因素 (权重: 0.25)
        if (speechRate > 5.0) {
            score += 0.125 * Math.min((speechRate - 5.0) / 2.0, 1.0);
        }
        
        // 文本情感词 (权重: 0.2)
        if (text != null && !text.isEmpty()) {
            String[] angryWords = {"生气", "愤怒", "讨厌", "烦", "气死了", "混蛋"};
            for (String word : angryWords) {
                if (text.contains(word)) {
                    score += 0.15;
                    break;
                }
            }
        }
        
        return Math.min(score, 1.0);
    }
    
    /**
     * 计算平静情绪得分
     * 特征：音高稳定、音量适中、能量适中、语速平稳
     */
    private double calculateCalmScore(double pitchStd, double volumeMean, 
                                     double energyLevel, double speechRate) {
        double score = 0.5; // 基础分数
        
        // 音高稳定性 (权重: 0.3)
        if (pitchStd < 20.0) {
            score += 0.15 * (1.0 - pitchStd / 20.0);
        } else {
            score -= 0.1 * Math.min((pitchStd - 20.0) / 20.0, 1.0);
        }
        
        // 音量适中 (权重: 0.2)
        if (volumeMean >= 0.4 && volumeMean <= 0.6) {
            score += 0.1;
        }
        
        // 能量适中 (权重: 0.3)
        if (energyLevel >= 0.4 && energyLevel <= 0.6) {
            score += 0.15;
        } else {
            score -= 0.1 * Math.abs(energyLevel - 0.5) / 0.5;
        }
        
        // 语速平稳 (权重: 0.2)
        if (speechRate >= 3.5 && speechRate <= 4.5) {
            score += 0.1;
        }
        
        return Math.max(0.0, Math.min(score, 1.0));
    }
    
    /**
     * 计算焦虑情绪得分
     * 特征：音高变化大、语速快、停顿多
     */
    private double calculateAnxiousScore(double pitchStd, double speechRate, String text) {
        double score = 0.0;
        
        // 音高变化因素 (权重: 0.4)
        if (pitchStd > 25.0) {
            score += 0.2 * Math.min((pitchStd - 25.0) / 20.0, 1.0);
        }
        
        // 语速因素 (权重: 0.4)
        if (speechRate > 5.0) {
            score += 0.2 * Math.min((speechRate - 5.0) / 2.0, 1.0);
        }
        
        // 文本情感词 (权重: 0.2)
        if (text != null && !text.isEmpty()) {
            String[] anxiousWords = {"担心", "焦虑", "紧张", "害怕", "不安", "怎么办"};
            for (String word : anxiousWords) {
                if (text.contains(word)) {
                    score += 0.15;
                    break;
                }
            }
        }
        
        return Math.min(score, 1.0);
    }
    
    /**
     * 计算兴奋情绪得分
     * 特征：音高高、能量高、语速快、音高变化大
     */
    private double calculateExcitedScore(double pitchMean, double energyLevel, 
                                        double speechRate, String text) {
        double score = 0.0;
        
        // 音高因素 (权重: 0.3)
        if (pitchMean > 165.0) {
            score += 0.15 * Math.min((pitchMean - 165.0) / 35.0, 1.0);
        }
        
        // 能量因素 (权重: 0.4)
        if (energyLevel > 0.7) {
            score += 0.2 * Math.min((energyLevel - 0.7) / 0.2, 1.0);
        }
        
        // 语速因素 (权重: 0.3)
        if (speechRate > 5.5) {
            score += 0.15 * Math.min((speechRate - 5.5) / 2.0, 1.0);
        }
        
        // 文本情感词 (权重: 0.2)
        if (text != null && !text.isEmpty()) {
            String[] excitedWords = {"太棒了", "哇", "激动", "兴奋", "厉害", "赞"};
            for (String word : excitedWords) {
                if (text.contains(word)) {
                    score += 0.1;
                    break;
                }
            }
        }
        
        return Math.min(score, 1.0);
    }
    
    /**
     * 计算中性情绪得分
     * 当其他情绪得分都不高时，中性得分较高
     */
    private double calculateNeutralScore(Map<EmotionType, Double> scores) {
        double maxScore = 0.0;
        for (Map.Entry<EmotionType, Double> entry : scores.entrySet()) {
            if (entry.getKey() != EmotionType.NEUTRAL && entry.getValue() > maxScore) {
                maxScore = entry.getValue();
            }
        }
        
        // 如果最高情绪得分低于0.3，则中性得分较高
        if (maxScore < 0.3) {
            return 0.7;
        } else if (maxScore < 0.5) {
            return 0.5;
        } else {
            return 0.3 - maxScore * 0.2;
        }
    }
    
    /**
     * 确定主要情绪
     */
    private EmotionType determinePrimaryEmotion(Map<EmotionType, Double> scores) {
        EmotionType primaryEmotion = EmotionType.NEUTRAL;
        double maxScore = 0.0;
        
        for (Map.Entry<EmotionType, Double> entry : scores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                primaryEmotion = entry.getKey();
            }
        }
        
        return primaryEmotion;
    }
    
    /**
     * 计算置信度
     */
    private double calculateConfidence(VoiceFeatureAnalyzer.VoiceFeatures features, 
                                      Map<EmotionType, Double> scores) {
        double confidence = 0.5; // 基础置信度
        
        // 1. 音频质量因素
        double quality = (features.getAudioQuality() != null) ? features.getAudioQuality() : 0.7;
        confidence += 0.2 * quality;
        
        // 2. 得分差异因素（最高分与次高分差距越大，置信度越高）
        List<Double> sortedScores = new ArrayList<>(scores.values());
        Collections.sort(sortedScores, Collections.reverseOrder());
        if (sortedScores.size() >= 2) {
            double scoreDiff = sortedScores.get(0) - sortedScores.get(1);
            confidence += 0.2 * scoreDiff;
        }
        
        // 3. 最高得分因素
        double maxScore = sortedScores.get(0);
        confidence += 0.1 * maxScore;
        
        return Math.max(0.0, Math.min(1.0, confidence));
    }
    
    /**
     * 计算情绪强度
     */
    public double calculateIntensity(VoiceFeatureAnalyzer.VoiceFeatures features, 
                                    EmotionType emotion) {
        double intensity = 0.5; // 基础强度
        
        // 基于能量水平
        double energyLevel = (features.getEnergyLevel() != null) ? features.getEnergyLevel() : 0.5;
        intensity += 0.3 * (energyLevel - 0.5);
        
        // 基于音高变化
        double pitchStd = (features.getPitchStd() != null) ? features.getPitchStd() : 20.0;
        if (pitchStd > 20.0) {
            intensity += 0.2 * Math.min((pitchStd - 20.0) / 20.0, 1.0);
        }
        
        return Math.max(0.0, Math.min(1.0, intensity));
    }
    
    /**
     * 分析语气风格
     */
    public ToneStyle analyzeTone(VoiceFeatureAnalyzer.VoiceFeatures features, String text) {
        logger.info("开始分析语气风格");
        
        // 获取关键特征
        double pitchStd = (features.getPitchStd() != null) ? features.getPitchStd() : 20.0;
        Integer speedWpm = features.getSpeedWpm();
        double speechRate = (speedWpm != null) ? speedWpm / 60.0 * 2.0 : 4.0;
        
        // 分析文本特征
        boolean hasFormalWords = false;
        boolean hasCasualWords = false;
        boolean hasHumorWords = false;
        
        if (text != null && !text.isEmpty()) {
            String[] formalWords = {"您", "请", "谢谢", "抱歉", "敬请"};
            String[] casualWords = {"哈", "嘿", "哎", "呀", "啊"};
            String[] humorWords = {"哈哈", "呵呵", "笑", "搞笑", "有趣"};
            
            for (String word : formalWords) {
                if (text.contains(word)) {
                    hasFormalWords = true;
                    break;
                }
            }
            
            for (String word : casualWords) {
                if (text.contains(word)) {
                    hasCasualWords = true;
                    break;
                }
            }
            
            for (String word : humorWords) {
                if (text.contains(word)) {
                    hasHumorWords = true;
                    break;
                }
            }
        }
        
        // 判断语气风格
        if (hasHumorWords) {
            return ToneStyle.HUMOROUS;
        } else if (hasFormalWords && pitchStd < 20.0 && speechRate < 4.5) {
            return ToneStyle.FORMAL;
        } else if (hasCasualWords || pitchStd > 25.0) {
            return ToneStyle.CASUAL;
        } else if (pitchStd < 15.0 && speechRate < 4.0) {
            return ToneStyle.SERIOUS;
        } else {
            return ToneStyle.CASUAL; // 默认随意
        }
    }
    
    /**
     * 生成情绪描述
     */
    private String generateDescription(EmotionResult result) {
        StringBuilder desc = new StringBuilder();
        
        desc.append("情绪: ").append(result.getPrimaryEmotion().getDisplayName());
        desc.append(", 强度: ");
        
        if (result.getIntensity() > 0.7) {
            desc.append("强");
        } else if (result.getIntensity() > 0.4) {
            desc.append("中等");
        } else {
            desc.append("弱");
        }
        
        desc.append(", 语气: ").append(result.getToneStyle().getDisplayName());
        
        return desc.toString();
    }
    
    /**
     * 批量识别情绪（用于情绪趋势分析）
     */
    public List<EmotionResult> recognizeEmotionBatch(
            List<VoiceFeatureAnalyzer.VoiceFeatures> featuresList, 
            List<String> textList) {
        
        logger.info("开始批量识别情绪，样本数量: {}", featuresList.size());
        
        List<EmotionResult> results = new ArrayList<>();
        for (int i = 0; i < featuresList.size(); i++) {
            VoiceFeatureAnalyzer.VoiceFeatures features = featuresList.get(i);
            String text = (textList != null && i < textList.size()) ? textList.get(i) : "";
            EmotionResult result = recognizeEmotion(features, text);
            results.add(result);
        }
        
        return results;
    }
}
