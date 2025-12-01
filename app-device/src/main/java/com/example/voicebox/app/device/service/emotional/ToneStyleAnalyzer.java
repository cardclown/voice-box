package com.example.voicebox.app.device.service.emotional;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.time.LocalDateTime;

/**
 * 语气风格识别服务
 * 
 * 分析用户的语气风格，包括正式、随意、幽默、严肃等
 * 支持基于文本和语音特征的综合判断
 */
@Service
public class ToneStyleAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(ToneStyleAnalyzer.class);
    
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
     * 语气分析结果
     */
    public static class ToneAnalysisResult {
        private ToneStyle primaryTone;              // 主要语气
        private double confidence;                   // 置信度 (0-1)
        private Map<ToneStyle, Double> toneScores;  // 各语气得分
        private List<String> indicators;             // 语气指示词
        private String description;                  // 描述
        private LocalDateTime analyzedAt;            // 分析时间
        
        public ToneAnalysisResult() {
            this.toneScores = new HashMap<>();
            this.indicators = new ArrayList<>();
            this.analyzedAt = LocalDateTime.now();
        }
        
        // Getters and Setters
        public ToneStyle getPrimaryTone() { return primaryTone; }
        public void setPrimaryTone(ToneStyle primaryTone) { this.primaryTone = primaryTone; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        
        public Map<ToneStyle, Double> getToneScores() { return toneScores; }
        public void setToneScores(Map<ToneStyle, Double> toneScores) { this.toneScores = toneScores; }
        
        public List<String> getIndicators() { return indicators; }
        public void setIndicators(List<String> indicators) { this.indicators = indicators; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public LocalDateTime getAnalyzedAt() { return analyzedAt; }
        public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }
    }
    
    /**
     * 语气标签
     */
    public static class ToneTag {
        private ToneStyle toneStyle;
        private double confidence;
        private LocalDateTime createdAt;
        private boolean isActive;
        
        public ToneTag(ToneStyle toneStyle, double confidence) {
            this.toneStyle = toneStyle;
            this.confidence = confidence;
            this.createdAt = LocalDateTime.now();
            this.isActive = true;
        }
        
        // Getters and Setters
        public ToneStyle getToneStyle() { return toneStyle; }
        public void setToneStyle(ToneStyle toneStyle) { this.toneStyle = toneStyle; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        
        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }
    }
    
    /**
     * 语气变化记录
     */
    public static class ToneChange {
        private ToneStyle fromTone;
        private ToneStyle toTone;
        private LocalDateTime changedAt;
        private String reason;
        
        public ToneChange(ToneStyle fromTone, ToneStyle toTone, String reason) {
            this.fromTone = fromTone;
            this.toTone = toTone;
            this.changedAt = LocalDateTime.now();
            this.reason = reason;
        }
        
        // Getters and Setters
        public ToneStyle getFromTone() { return fromTone; }
        public void setFromTone(ToneStyle fromTone) { this.fromTone = fromTone; }
        
        public ToneStyle getToTone() { return toTone; }
        public void setToTone(ToneStyle toTone) { this.toTone = toTone; }
        
        public LocalDateTime getChangedAt() { return changedAt; }
        public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
    
    // 语气关键词库
    private static final Map<ToneStyle, String[]> TONE_KEYWORDS = new HashMap<>();
    
    static {
        TONE_KEYWORDS.put(ToneStyle.FORMAL, new String[]{
            "您", "请", "谢谢", "抱歉", "敬请", "恳请", "拜托", "劳烦",
            "感谢", "不好意思", "打扰", "冒昧", "请问", "请教"
        });
        
        TONE_KEYWORDS.put(ToneStyle.CASUAL, new String[]{
            "哈", "嘿", "哎", "呀", "啊", "嗯", "哦", "喂",
            "咋", "啥", "咱", "俺", "嘛", "呗", "吧"
        });
        
        TONE_KEYWORDS.put(ToneStyle.HUMOROUS, new String[]{
            "哈哈", "呵呵", "嘻嘻", "笑", "搞笑", "有趣", "好玩",
            "逗", "乐", "幽默", "开玩笑", "玩笑", "调侃"
        });
        
        TONE_KEYWORDS.put(ToneStyle.SERIOUS, new String[]{
            "重要", "严肃", "认真", "必须", "务必", "一定", "绝对",
            "关键", "核心", "本质", "根本", "原则", "规则"
        });
    }
    
    /**
     * 分析语气风格
     * 
     * @param features 语音特征
     * @param text 文本内容
     * @return 语气分析结果
     */
    public ToneAnalysisResult analyzeTone(VoiceFeatureAnalyzer.VoiceFeatures features, String text) {
        logger.info("开始分析语气风格");
        
        ToneAnalysisResult result = new ToneAnalysisResult();
        
        // 1. 计算各语气得分
        Map<ToneStyle, Double> scores = calculateToneScores(features, text);
        result.setToneScores(scores);
        
        // 2. 确定主要语气
        ToneStyle primaryTone = determinePrimaryTone(scores);
        result.setPrimaryTone(primaryTone);
        
        // 3. 计算置信度
        double confidence = calculateConfidence(features, scores, text);
        result.setConfidence(confidence);
        
        // 4. 提取语气指示词
        List<String> indicators = extractIndicators(text, primaryTone);
        result.setIndicators(indicators);
        
        // 5. 生成描述
        String description = generateDescription(result);
        result.setDescription(description);
        
        logger.info("语气分析完成: 语气={}, 置信度={}", primaryTone.getDisplayName(), confidence);
        
        return result;
    }
    
    /**
     * 计算各语气得分
     */
    private Map<ToneStyle, Double> calculateToneScores(
            VoiceFeatureAnalyzer.VoiceFeatures features, String text) {
        
        Map<ToneStyle, Double> scores = new HashMap<>();
        
        // 获取关键特征
        double pitchStd = (features.getPitchStd() != null) ? features.getPitchStd() : 20.0;
        double volumeMean = (features.getVolumeMean() != null) ? features.getVolumeMean() : 0.5;
        Integer speedWpm = features.getSpeedWpm();
        double speechRate = (speedWpm != null) ? speedWpm / 60.0 * 2.0 : 4.0;
        double energyLevel = (features.getEnergyLevel() != null) ? features.getEnergyLevel() : 0.5;
        
        // 计算正式语气得分
        double formalScore = calculateFormalScore(pitchStd, speechRate, volumeMean, text);
        scores.put(ToneStyle.FORMAL, formalScore);
        
        // 计算随意语气得分
        double casualScore = calculateCasualScore(pitchStd, speechRate, energyLevel, text);
        scores.put(ToneStyle.CASUAL, casualScore);
        
        // 计算幽默语气得分
        double humorousScore = calculateHumorousScore(pitchStd, energyLevel, text);
        scores.put(ToneStyle.HUMOROUS, humorousScore);
        
        // 计算严肃语气得分
        double seriousScore = calculateSeriousScore(pitchStd, speechRate, volumeMean, text);
        scores.put(ToneStyle.SERIOUS, seriousScore);
        
        return scores;
    }
    
    /**
     * 计算正式语气得分
     * 特征：音高稳定、语速适中偏慢、音量适中、使用敬语
     */
    private double calculateFormalScore(double pitchStd, double speechRate, 
                                       double volumeMean, String text) {
        double score = 0.0;
        
        // 音高稳定性 (权重: 0.25)
        if (pitchStd < 20.0) {
            score += 0.125 * (1.0 - pitchStd / 20.0);
        }
        
        // 语速适中偏慢 (权重: 0.25)
        if (speechRate >= 3.0 && speechRate <= 4.5) {
            score += 0.125;
        }
        
        // 音量适中 (权重: 0.2)
        if (volumeMean >= 0.4 && volumeMean <= 0.6) {
            score += 0.1;
        }
        
        // 文本特征 (权重: 0.3)
        if (text != null && !text.isEmpty()) {
            int formalWordCount = countKeywords(text, ToneStyle.FORMAL);
            int casualWordCount = countKeywords(text, ToneStyle.CASUAL);
            
            if (formalWordCount > 0) {
                score += 0.15 * Math.min(formalWordCount / 3.0, 1.0);
            }
            
            // 如果有随意词汇，降低正式得分
            if (casualWordCount > 0) {
                score -= 0.1 * Math.min(casualWordCount / 2.0, 1.0);
            }
        }
        
        return Math.max(0.0, Math.min(score, 1.0));
    }
    
    /**
     * 计算随意语气得分
     * 特征：音高变化大、语速快、能量高、使用口语化表达
     */
    private double calculateCasualScore(double pitchStd, double speechRate, 
                                       double energyLevel, String text) {
        double score = 0.3; // 基础分数（随意是默认语气）
        
        // 音高变化 (权重: 0.25)
        if (pitchStd > 25.0) {
            score += 0.125 * Math.min((pitchStd - 25.0) / 20.0, 1.0);
        }
        
        // 语速 (权重: 0.25)
        if (speechRate > 4.5) {
            score += 0.125 * Math.min((speechRate - 4.5) / 2.0, 1.0);
        }
        
        // 能量水平 (权重: 0.2)
        if (energyLevel > 0.5) {
            score += 0.1 * (energyLevel - 0.5) / 0.5;
        }
        
        // 文本特征 (权重: 0.3)
        if (text != null && !text.isEmpty()) {
            int casualWordCount = countKeywords(text, ToneStyle.CASUAL);
            int formalWordCount = countKeywords(text, ToneStyle.FORMAL);
            
            if (casualWordCount > 0) {
                score += 0.15 * Math.min(casualWordCount / 3.0, 1.0);
            }
            
            // 如果有正式词汇，降低随意得分
            if (formalWordCount > 0) {
                score -= 0.1 * Math.min(formalWordCount / 2.0, 1.0);
            }
        }
        
        return Math.max(0.0, Math.min(score, 1.0));
    }
    
    /**
     * 计算幽默语气得分
     * 特征：音高变化丰富、能量高、包含幽默词汇
     */
    private double calculateHumorousScore(double pitchStd, double energyLevel, String text) {
        double score = 0.0;
        
        // 音高变化 (权重: 0.3)
        if (pitchStd > 30.0) {
            score += 0.15 * Math.min((pitchStd - 30.0) / 20.0, 1.0);
        }
        
        // 能量水平 (权重: 0.3)
        if (energyLevel > 0.6) {
            score += 0.15 * Math.min((energyLevel - 0.6) / 0.3, 1.0);
        }
        
        // 文本特征 (权重: 0.4)
        if (text != null && !text.isEmpty()) {
            int humorWordCount = countKeywords(text, ToneStyle.HUMOROUS);
            
            if (humorWordCount > 0) {
                score += 0.2 * Math.min(humorWordCount / 2.0, 1.0);
            }
            
            // 检测重复字符（如"哈哈哈"）
            if (text.matches(".*([哈呵嘻])\\1{2,}.*")) {
                score += 0.2;
            }
        }
        
        return Math.min(score, 1.0);
    }
    
    /**
     * 计算严肃语气得分
     * 特征：音高稳定、语速慢、音量适中、使用严肃词汇
     */
    private double calculateSeriousScore(double pitchStd, double speechRate, 
                                        double volumeMean, String text) {
        double score = 0.0;
        
        // 音高稳定性 (权重: 0.3)
        if (pitchStd < 15.0) {
            score += 0.15 * (1.0 - pitchStd / 15.0);
        }
        
        // 语速慢 (权重: 0.3)
        if (speechRate < 4.0) {
            score += 0.15 * (1.0 - speechRate / 4.0);
        }
        
        // 音量适中 (权重: 0.2)
        if (volumeMean >= 0.4 && volumeMean <= 0.6) {
            score += 0.1;
        }
        
        // 文本特征 (权重: 0.2)
        if (text != null && !text.isEmpty()) {
            int seriousWordCount = countKeywords(text, ToneStyle.SERIOUS);
            
            if (seriousWordCount > 0) {
                score += 0.1 * Math.min(seriousWordCount / 2.0, 1.0);
            }
        }
        
        return Math.min(score, 1.0);
    }
    
    /**
     * 统计关键词数量
     */
    private int countKeywords(String text, ToneStyle toneStyle) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        String[] keywords = TONE_KEYWORDS.get(toneStyle);
        if (keywords == null) {
            return 0;
        }
        
        int count = 0;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * 确定主要语气
     */
    private ToneStyle determinePrimaryTone(Map<ToneStyle, Double> scores) {
        ToneStyle primaryTone = ToneStyle.CASUAL; // 默认随意
        double maxScore = 0.0;
        
        for (Map.Entry<ToneStyle, Double> entry : scores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                primaryTone = entry.getKey();
            }
        }
        
        return primaryTone;
    }
    
    /**
     * 计算置信度
     */
    private double calculateConfidence(VoiceFeatureAnalyzer.VoiceFeatures features,
                                      Map<ToneStyle, Double> scores, String text) {
        double confidence = 0.5; // 基础置信度
        
        // 1. 音频质量因素
        double quality = (features.getAudioQuality() != null) ? features.getAudioQuality() : 0.7;
        confidence += 0.2 * quality;
        
        // 2. 得分差异因素
        List<Double> sortedScores = new ArrayList<>(scores.values());
        Collections.sort(sortedScores, Collections.reverseOrder());
        if (sortedScores.size() >= 2) {
            double scoreDiff = sortedScores.get(0) - sortedScores.get(1);
            confidence += 0.2 * scoreDiff;
        }
        
        // 3. 文本长度因素（文本越长，分析越准确）
        if (text != null && !text.isEmpty()) {
            int textLength = text.length();
            if (textLength > 20) {
                confidence += 0.1 * Math.min((textLength - 20) / 80.0, 1.0);
            }
        }
        
        return Math.max(0.0, Math.min(1.0, confidence));
    }
    
    /**
     * 提取语气指示词
     */
    private List<String> extractIndicators(String text, ToneStyle toneStyle) {
        List<String> indicators = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return indicators;
        }
        
        String[] keywords = TONE_KEYWORDS.get(toneStyle);
        if (keywords == null) {
            return indicators;
        }
        
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                indicators.add(keyword);
            }
        }
        
        return indicators;
    }
    
    /**
     * 生成描述
     */
    private String generateDescription(ToneAnalysisResult result) {
        StringBuilder desc = new StringBuilder();
        
        desc.append("语气: ").append(result.getPrimaryTone().getDisplayName());
        desc.append(", 置信度: ");
        
        if (result.getConfidence() > 0.7) {
            desc.append("高");
        } else if (result.getConfidence() > 0.4) {
            desc.append("中等");
        } else {
            desc.append("低");
        }
        
        if (!result.getIndicators().isEmpty()) {
            desc.append(", 指示词: ");
            desc.append(String.join("、", result.getIndicators().subList(
                0, Math.min(3, result.getIndicators().size()))));
        }
        
        return desc.toString();
    }
    
    /**
     * 生成语气标签
     * 
     * @param analysisResult 分析结果
     * @return 语气标签
     */
    public ToneTag generateToneTag(ToneAnalysisResult analysisResult) {
        logger.info("生成语气标签: {}", analysisResult.getPrimaryTone().getDisplayName());
        
        return new ToneTag(
            analysisResult.getPrimaryTone(),
            analysisResult.getConfidence()
        );
    }
    
    /**
     * 追踪语气变化
     * 
     * @param previousTone 之前的语气
     * @param currentTone 当前的语气
     * @return 语气变化记录
     */
    public ToneChange trackToneChange(ToneStyle previousTone, ToneStyle currentTone) {
        if (previousTone == currentTone) {
            return null;
        }
        
        String reason = generateChangeReason(previousTone, currentTone);
        logger.info("检测到语气变化: {} -> {}, 原因: {}", 
                   previousTone.getDisplayName(), currentTone.getDisplayName(), reason);
        
        return new ToneChange(previousTone, currentTone, reason);
    }
    
    /**
     * 生成语气变化原因
     */
    private String generateChangeReason(ToneStyle from, ToneStyle to) {
        if (from == ToneStyle.FORMAL && to == ToneStyle.CASUAL) {
            return "从正式转为随意，可能是话题变轻松或关系更亲近";
        } else if (from == ToneStyle.CASUAL && to == ToneStyle.FORMAL) {
            return "从随意转为正式，可能是话题变严肃或需要表达尊重";
        } else if (to == ToneStyle.HUMOROUS) {
            return "转为幽默语气，可能是想活跃气氛或开玩笑";
        } else if (to == ToneStyle.SERIOUS) {
            return "转为严肃语气，可能是讨论重要话题";
        } else {
            return "语气发生变化";
        }
    }
    
    /**
     * 批量分析语气（用于语气趋势分析）
     * 
     * @param featuresList 语音特征列表
     * @param textList 文本列表
     * @return 语气分析结果列表
     */
    public List<ToneAnalysisResult> analyzeToneBatch(
            List<VoiceFeatureAnalyzer.VoiceFeatures> featuresList,
            List<String> textList) {
        
        logger.info("开始批量分析语气，样本数量: {}", featuresList.size());
        
        List<ToneAnalysisResult> results = new ArrayList<>();
        for (int i = 0; i < featuresList.size(); i++) {
            VoiceFeatureAnalyzer.VoiceFeatures features = featuresList.get(i);
            String text = (textList != null && i < textList.size()) ? textList.get(i) : "";
            ToneAnalysisResult result = analyzeTone(features, text);
            results.add(result);
        }
        
        return results;
    }
    
    /**
     * 分析语气趋势
     * 
     * @param results 语气分析结果列表
     * @return 主导语气
     */
    public ToneStyle analyzeToneTrend(List<ToneAnalysisResult> results) {
        if (results == null || results.isEmpty()) {
            return ToneStyle.CASUAL;
        }
        
        Map<ToneStyle, Integer> toneCount = new HashMap<>();
        for (ToneAnalysisResult result : results) {
            ToneStyle tone = result.getPrimaryTone();
            toneCount.put(tone, toneCount.getOrDefault(tone, 0) + 1);
        }
        
        ToneStyle dominantTone = ToneStyle.CASUAL;
        int maxCount = 0;
        for (Map.Entry<ToneStyle, Integer> entry : toneCount.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                dominantTone = entry.getKey();
            }
        }
        
        logger.info("语气趋势分析完成: 主导语气={}, 出现次数={}", 
                   dominantTone.getDisplayName(), maxCount);
        
        return dominantTone;
    }
}
