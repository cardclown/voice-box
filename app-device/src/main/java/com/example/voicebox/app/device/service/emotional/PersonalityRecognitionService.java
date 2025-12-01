package com.example.voicebox.app.device.service.emotional;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 性格特征识别服务
 * 
 * 基于语音特征和文本内容分析用户的性格特征
 * 包括外向/内向、理性/感性等维度
 */
@Service
public class PersonalityRecognitionService {
    
    private static final Logger logger = LoggerFactory.getLogger(PersonalityRecognitionService.class);
    
    // 性格维度阈值
    private static final double EXTROVERT_SPEED_THRESHOLD = 4.5; // 外向型语速阈值（字/秒）
    private static final double EXTROVERT_VOLUME_THRESHOLD = 0.6; // 外向型音量阈值
    private static final double RATIONAL_THRESHOLD = 0.6; // 理性程度阈值
    
    /**
     * 性格特征结果
     */
    public static class PersonalityTraits {
        private String primaryType; // 主要性格类型
        private double extroversionScore; // 外向性得分 (0-1)
        private double rationalityScore; // 理性程度得分 (0-1)
        private Map<String, Double> dimensionScores; // 各维度得分
        private List<String> tags; // 性格标签
        private double confidence; // 置信度
        
        public PersonalityTraits() {
            this.dimensionScores = new HashMap<>();
            this.tags = new ArrayList<>();
        }
        
        // Getters and Setters
        public String getPrimaryType() { return primaryType; }
        public void setPrimaryType(String primaryType) { this.primaryType = primaryType; }
        
        public double getExtroversionScore() { return extroversionScore; }
        public void setExtroversionScore(double extroversionScore) { this.extroversionScore = extroversionScore; }
        
        public double getRationalityScore() { return rationalityScore; }
        public void setRationalityScore(double rationalityScore) { this.rationalityScore = rationalityScore; }
        
        public Map<String, Double> getDimensionScores() { return dimensionScores; }
        public void setDimensionScores(Map<String, Double> dimensionScores) { this.dimensionScores = dimensionScores; }
        
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }
    
    /**
     * 分析性格特征
     * 
     * @param features 语音特征
     * @param text 文本内容
     * @return 性格特征分析结果
     */
    public PersonalityTraits analyzePersonality(VoiceFeatureAnalyzer.VoiceFeatures features, String text) {
        logger.info("开始分析性格特征");
        
        PersonalityTraits traits = new PersonalityTraits();
        
        // 1. 分析外向性维度
        double extroversionScore = analyzeExtroversion(features, text);
        traits.setExtroversionScore(extroversionScore);
        traits.getDimensionScores().put("extroversion", extroversionScore);
        
        // 2. 分析理性维度
        double rationalityScore = analyzeRationality(features, text);
        traits.setRationalityScore(rationalityScore);
        traits.getDimensionScores().put("rationality", rationalityScore);
        
        // 3. 确定主要性格类型
        String primaryType = determinePrimaryType(extroversionScore, rationalityScore);
        traits.setPrimaryType(primaryType);
        
        // 4. 生成性格标签
        List<String> tags = generatePersonalityTags(traits);
        traits.setTags(tags);
        
        // 5. 计算置信度
        double confidence = calculateConfidence(features, text);
        traits.setConfidence(confidence);
        
        logger.info("性格分析完成: 类型={}, 外向性={}, 理性={}, 置信度={}", 
                   primaryType, extroversionScore, rationalityScore, confidence);
        
        return traits;
    }
    
    /**
     * 分析外向性维度
     * 
     * 外向型特征：
     * - 语速较快
     * - 音量较大
     * - 语气变化丰富
     * - 用词活泼
     */
    private double analyzeExtroversion(VoiceFeatureAnalyzer.VoiceFeatures features, String text) {
        double score = 0.5; // 基础分数
        
        // 1. 语速因素 (权重: 0.4)
        // 将每分钟词数转换为每秒字数（假设平均每词2个字）
        double speechRate = (features.getSpeedWpm() != null) ? features.getSpeedWpm() / 60.0 * 2.0 : 4.0;
        if (speechRate > EXTROVERT_SPEED_THRESHOLD) {
            score += 0.2 * Math.min((speechRate - EXTROVERT_SPEED_THRESHOLD) / 2.0, 1.0);
        } else {
            score -= 0.2 * Math.min((EXTROVERT_SPEED_THRESHOLD - speechRate) / 2.0, 1.0);
        }
        
        // 2. 音量因素 (权重: 0.3)
        double volumeMean = (features.getVolumeMean() != null) ? features.getVolumeMean() : 0.5;
        if (volumeMean > EXTROVERT_VOLUME_THRESHOLD) {
            score += 0.15 * Math.min((volumeMean - EXTROVERT_VOLUME_THRESHOLD) / 0.3, 1.0);
        } else {
            score -= 0.15 * Math.min((EXTROVERT_VOLUME_THRESHOLD - volumeMean) / 0.3, 1.0);
        }
        
        // 3. 音量变化因素 (权重: 0.2)
        // 使用音量标准差作为变化指标
        double volumeVariance = (features.getVolumeStd() != null) ? features.getVolumeStd() : 0.1;
        if (volumeVariance > 0.1) {
            score += 0.1 * Math.min(volumeVariance / 0.2, 1.0);
        }
        
        // 4. 文本因素 (权重: 0.1)
        if (text != null && !text.isEmpty()) {
            // 检查感叹号、问号等活泼标点
            long exclamationCount = text.chars().filter(ch -> ch == '!' || ch == '！').count();
            if (exclamationCount > 0) {
                score += 0.05 * Math.min(exclamationCount / 3.0, 1.0);
            }
        }
        
        // 确保分数在 [0, 1] 范围内
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * 分析理性维度
     * 
     * 理性型特征：
     * - 语速平稳
     * - 音量稳定
     * - 逻辑词汇多
     * - 情绪波动小
     */
    private double analyzeRationality(VoiceFeatureAnalyzer.VoiceFeatures features, String text) {
        double score = 0.5; // 基础分数
        
        // 1. 语速稳定性 (权重: 0.3)
        // 将每分钟词数转换为每秒字数
        double speechRate = (features.getSpeedWpm() != null) ? features.getSpeedWpm() / 60.0 * 2.0 : 4.0;
        // 理性型语速适中 (3.5-5.0 字/秒)
        if (speechRate >= 3.5 && speechRate <= 5.0) {
            score += 0.15;
        } else {
            score -= 0.1 * Math.abs(speechRate - 4.25) / 2.0;
        }
        
        // 2. 音量稳定性 (权重: 0.3)
        // 使用音量标准差作为变化指标
        double volumeVariance = (features.getVolumeStd() != null) ? features.getVolumeStd() : 0.1;
        // 理性型音量变化小
        if (volumeVariance < 0.1) {
            score += 0.15 * (1.0 - volumeVariance / 0.1);
        } else {
            score -= 0.1 * Math.min((volumeVariance - 0.1) / 0.2, 1.0);
        }
        
        // 3. 音高稳定性 (权重: 0.2)
        // 使用音高标准差作为变化指标
        double pitchVariance = (features.getPitchStd() != null) ? features.getPitchStd() : 20.0;
        // 理性型音高变化小
        if (pitchVariance < 30.0) {
            score += 0.1 * (1.0 - pitchVariance / 30.0);
        }
        
        // 4. 文本逻辑性 (权重: 0.2)
        if (text != null && !text.isEmpty()) {
            // 检查逻辑连接词
            String[] logicalWords = {"因为", "所以", "但是", "然而", "因此", "由于", "如果", "那么"};
            long logicalWordCount = 0;
            for (String word : logicalWords) {
                if (text.contains(word)) {
                    logicalWordCount++;
                }
            }
            if (logicalWordCount > 0) {
                score += 0.1 * Math.min(logicalWordCount / 3.0, 1.0);
            }
            
            // 检查情感词汇（理性型较少）
            String[] emotionalWords = {"好开心", "太棒了", "讨厌", "生气", "伤心"};
            long emotionalWordCount = 0;
            for (String word : emotionalWords) {
                if (text.contains(word)) {
                    emotionalWordCount++;
                }
            }
            if (emotionalWordCount > 0) {
                score -= 0.05 * Math.min(emotionalWordCount / 2.0, 1.0);
            }
        }
        
        // 确保分数在 [0, 1] 范围内
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * 确定主要性格类型
     * 
     * 基于外向性和理性维度的组合
     */
    private String determinePrimaryType(double extroversionScore, double rationalityScore) {
        boolean isExtrovert = extroversionScore > 0.5;
        boolean isRational = rationalityScore > RATIONAL_THRESHOLD;
        
        if (isExtrovert && isRational) {
            return "理性外向型";
        } else if (isExtrovert && !isRational) {
            return "感性外向型";
        } else if (!isExtrovert && isRational) {
            return "理性内向型";
        } else {
            return "感性内向型";
        }
    }
    
    /**
     * 生成性格标签
     */
    private List<String> generatePersonalityTags(PersonalityTraits traits) {
        List<String> tags = new ArrayList<>();
        
        // 外向性标签
        double extroversion = traits.getExtroversionScore();
        if (extroversion > 0.7) {
            tags.add("非常外向");
        } else if (extroversion > 0.5) {
            tags.add("外向");
        } else if (extroversion < 0.3) {
            tags.add("非常内向");
        } else if (extroversion < 0.5) {
            tags.add("内向");
        }
        
        // 理性标签
        double rationality = traits.getRationalityScore();
        if (rationality > 0.7) {
            tags.add("非常理性");
        } else if (rationality > RATIONAL_THRESHOLD) {
            tags.add("理性");
        } else if (rationality < 0.3) {
            tags.add("非常感性");
        } else if (rationality < 0.5) {
            tags.add("感性");
        }
        
        // 综合特征标签
        if (extroversion > 0.6 && rationality > 0.6) {
            tags.add("逻辑清晰");
            tags.add("善于表达");
        } else if (extroversion > 0.6 && rationality < 0.4) {
            tags.add("热情洋溢");
            tags.add("情感丰富");
        } else if (extroversion < 0.4 && rationality > 0.6) {
            tags.add("深思熟虑");
            tags.add("沉稳冷静");
        } else if (extroversion < 0.4 && rationality < 0.4) {
            tags.add("细腻敏感");
            tags.add("内心丰富");
        }
        
        return tags;
    }
    
    /**
     * 计算置信度
     * 
     * 基于数据质量和特征明显程度
     */
    private double calculateConfidence(VoiceFeatureAnalyzer.VoiceFeatures features, String text) {
        double confidence = 0.5; // 基础置信度
        
        // 1. 音频质量因素
        double quality = (features.getAudioQuality() != null) ? features.getAudioQuality() : 0.5;
        confidence += 0.2 * quality;
        
        // 2. 音频时长因素（通过停顿时长和语速估算）
        double pauseDuration = (features.getPauseDuration() != null) ? features.getPauseDuration() : 0.0;
        Integer speedWpm = features.getSpeedWpm();
        double estimatedDuration = 3.0; // 默认估算3秒
        if (speedWpm != null && speedWpm > 0 && text != null) {
            // 根据文本长度和语速估算时长
            int wordCount = text.length() / 2; // 假设平均每词2个字
            estimatedDuration = (wordCount / (double)speedWpm) * 60.0 + pauseDuration;
        }
        
        if (estimatedDuration >= 3.0) {
            confidence += 0.15;
        } else if (estimatedDuration >= 1.0) {
            confidence += 0.1 * (estimatedDuration / 3.0);
        }
        
        // 3. 文本长度因素
        if (text != null && !text.isEmpty()) {
            int textLength = text.length();
            if (textLength >= 20) {
                confidence += 0.1;
            } else if (textLength >= 10) {
                confidence += 0.05;
            }
        }
        
        // 4. 特征明显程度
        double pitchVariance = (features.getPitchStd() != null) ? features.getPitchStd() : 0.0;
        double volumeVariance = (features.getVolumeStd() != null) ? features.getVolumeStd() : 0.0;
        if (pitchVariance > 20.0 || volumeVariance > 0.15) {
            confidence += 0.05; // 特征明显，置信度提高
        }
        
        // 确保置信度在 [0, 1] 范围内
        return Math.max(0.0, Math.min(1.0, confidence));
    }
    
    /**
     * 批量分析性格特征
     * 
     * 基于多次语音输入的综合分析
     */
    public PersonalityTraits analyzePersonalityBatch(List<VoiceFeatureAnalyzer.VoiceFeatures> featuresList, 
                                                     List<String> textList) {
        if (featuresList == null || featuresList.isEmpty()) {
            throw new IllegalArgumentException("特征列表不能为空");
        }
        
        logger.info("开始批量分析性格特征，样本数量: {}", featuresList.size());
        
        // 分析每个样本
        List<PersonalityTraits> traitsList = new ArrayList<>();
        for (int i = 0; i < featuresList.size(); i++) {
            VoiceFeatureAnalyzer.VoiceFeatures features = featuresList.get(i);
            String text = (textList != null && i < textList.size()) ? textList.get(i) : "";
            PersonalityTraits traits = analyzePersonality(features, text);
            traitsList.add(traits);
        }
        
        // 综合分析结果
        return aggregatePersonalityTraits(traitsList);
    }
    
    /**
     * 聚合多次分析结果
     */
    private PersonalityTraits aggregatePersonalityTraits(List<PersonalityTraits> traitsList) {
        PersonalityTraits aggregated = new PersonalityTraits();
        
        // 计算平均外向性得分
        double avgExtroversion = traitsList.stream()
            .mapToDouble(PersonalityTraits::getExtroversionScore)
            .average()
            .orElse(0.5);
        aggregated.setExtroversionScore(avgExtroversion);
        
        // 计算平均理性得分
        double avgRationality = traitsList.stream()
            .mapToDouble(PersonalityTraits::getRationalityScore)
            .average()
            .orElse(0.5);
        aggregated.setRationalityScore(avgRationality);
        
        // 确定主要类型
        String primaryType = determinePrimaryType(avgExtroversion, avgRationality);
        aggregated.setPrimaryType(primaryType);
        
        // 聚合标签（取出现频率最高的）
        Map<String, Integer> tagFrequency = new HashMap<>();
        for (PersonalityTraits traits : traitsList) {
            for (String tag : traits.getTags()) {
                tagFrequency.put(tag, tagFrequency.getOrDefault(tag, 0) + 1);
            }
        }
        
        List<String> topTags = tagFrequency.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toList());
        aggregated.setTags(topTags);
        
        // 计算综合置信度（样本越多，置信度越高）
        double avgConfidence = traitsList.stream()
            .mapToDouble(PersonalityTraits::getConfidence)
            .average()
            .orElse(0.5);
        double sampleBonus = Math.min(0.2, traitsList.size() * 0.05);
        aggregated.setConfidence(Math.min(1.0, avgConfidence + sampleBonus));
        
        // 设置维度得分
        aggregated.getDimensionScores().put("extroversion", avgExtroversion);
        aggregated.getDimensionScores().put("rationality", avgRationality);
        
        logger.info("批量分析完成: 类型={}, 外向性={}, 理性={}, 置信度={}", 
                   primaryType, avgExtroversion, avgRationality, aggregated.getConfidence());
        
        return aggregated;
    }
}
