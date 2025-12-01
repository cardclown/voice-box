package com.example.voicebox.app.device.service;

import com.example.voicebox.app.device.domain.ConversationFeature;
import com.example.voicebox.app.device.domain.UserProfile;
import com.example.voicebox.app.device.repository.ConversationFeatureRepository;
import com.example.voicebox.app.device.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 个性分析服务
 * 基于大五人格模型分析用户性格特征
 * 
 * @author VoiceBox Team
 * @since 2.0
 */
@Service
public class PersonalityAnalysisService {
    
    @Autowired
    private ConversationFeatureRepository conversationFeatureRepository;
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    /**
     * 分析用户个性并更新画像
     */
    public UserProfile analyzePersonality(Long userId) {
        // 获取用户最近的对话特征（最近100条）
        List<ConversationFeature> recentFeatures = 
            conversationFeatureRepository.findRecentByUserId(userId, 100);
        
        if (recentFeatures.isEmpty()) {
            // 如果没有数据，返回默认画像
            return getOrCreateDefaultProfile(userId);
        }
        
        // 计算大五人格维度
        BigDecimal openness = calculateOpenness(recentFeatures);
        BigDecimal conscientiousness = calculateConscientiousness(recentFeatures);
        BigDecimal extraversion = calculateExtraversion(recentFeatures);
        BigDecimal agreeableness = calculateAgreeableness(recentFeatures);
        BigDecimal neuroticism = calculateNeuroticism(recentFeatures);
        
        // 计算置信度
        BigDecimal confidence = calculateConfidence(recentFeatures.size());
        
        // 分析偏好
        String responseLengthPref = analyzeResponseLengthPreference(recentFeatures);
        String languageStylePref = analyzeLanguageStylePreference(recentFeatures);
        String interactionStyle = analyzeInteractionStyle(recentFeatures);
        
        // 更新或创建用户画像
        UserProfile profile = userProfileRepository.findByUserId(userId);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
        }
        
        profile.setOpenness(openness);
        profile.setConscientiousness(conscientiousness);
        profile.setExtraversion(extraversion);
        profile.setAgreeableness(agreeableness);
        profile.setNeuroticism(neuroticism);
        profile.setConfidenceScore(confidence);
        profile.setResponseLengthPreference(responseLengthPref);
        profile.setLanguageStylePreference(languageStylePref);
        profile.setInteractionStyle(interactionStyle);
        
        // 更新统计信息
        profile.setTotalMessages(recentFeatures.size());
        
        return userProfileRepository.createOrUpdate(profile);
    }
    
    /**
     * 计算开放性 (Openness)
     * 基于：词汇丰富度、主题多样性、代码使用
     */
    private BigDecimal calculateOpenness(List<ConversationFeature> features) {
        double score = 0.5; // 默认中等
        
        // 词汇丰富度
        double avgRichness = features.stream()
            .filter(f -> f.getVocabularyRichness() != null)
            .mapToDouble(f -> f.getVocabularyRichness().doubleValue())
            .average()
            .orElse(0.5);
        
        // 主题多样性
        long uniqueTopics = features.stream()
            .flatMap(f -> f.getTopicList().stream())
            .distinct()
            .count();
        double topicDiversity = Math.min(1.0, uniqueTopics / 10.0);
        
        // 代码使用频率
        double codeUsage = features.stream()
            .filter(f -> f.getCodeBlockCount() > 0)
            .count() / (double) features.size();
        
        // 综合计算
        score = (avgRichness * 0.4 + topicDiversity * 0.3 + codeUsage * 0.3);
        
        return BigDecimal.valueOf(score).setScale(3, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算尽责性 (Conscientiousness)
     * 基于：消息长度、句子完整性、问题深度
     */
    private BigDecimal calculateConscientiousness(List<ConversationFeature> features) {
        double score = 0.5;
        
        // 平均消息长度（长消息表示更认真）
        double avgLength = features.stream()
            .filter(f -> f.getMessageLength() != null)
            .mapToInt(ConversationFeature::getMessageLength)
            .average()
            .orElse(50.0);
        double lengthScore = Math.min(1.0, avgLength / 200.0);
        
        // 平均句子数（多句子表示更有条理）
        double avgSentences = features.stream()
            .filter(f -> f.getSentenceCount() != null)
            .mapToInt(ConversationFeature::getSentenceCount)
            .average()
            .orElse(2.0);
        double sentenceScore = Math.min(1.0, avgSentences / 5.0);
        
        // 问题比例（提问多表示更认真思考）
        double questionRatio = features.stream()
            .filter(f -> f.isQuestion())
            .count() / (double) features.size();
        
        score = (lengthScore * 0.4 + sentenceScore * 0.3 + questionRatio * 0.3);
        
        return BigDecimal.valueOf(score).setScale(3, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算外向性 (Extraversion)
     * 基于：表情使用、感叹号使用、情感表达
     */
    private BigDecimal calculateExtraversion(List<ConversationFeature> features) {
        double score = 0.5;
        
        // 表情符号使用频率
        double emojiUsage = features.stream()
            .filter(f -> f.getEmojiCount() > 0)
            .count() / (double) features.size();
        
        // 感叹号使用频率
        double exclamationUsage = features.stream()
            .filter(f -> f.getExclamationCount() > 0)
            .count() / (double) features.size();
        
        // 情感表达强度
        double avgSentiment = Math.abs(features.stream()
            .filter(f -> f.getSentimentScore() != null)
            .mapToDouble(f -> f.getSentimentScore().doubleValue())
            .average()
            .orElse(0.0));
        
        score = (emojiUsage * 0.4 + exclamationUsage * 0.3 + avgSentiment * 0.3);
        
        return BigDecimal.valueOf(score).setScale(3, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算宜人性 (Agreeableness)
     * 基于：正面情感、礼貌用语、感谢表达
     */
    private BigDecimal calculateAgreeableness(List<ConversationFeature> features) {
        double score = 0.5;
        
        // 正面情感比例
        long positiveCount = features.stream()
            .filter(ConversationFeature::isPositiveSentiment)
            .count();
        double positiveRatio = positiveCount / (double) features.size();
        
        // 平均情感分数
        double avgSentiment = features.stream()
            .filter(f -> f.getSentimentScore() != null)
            .mapToDouble(f -> f.getSentimentScore().doubleValue())
            .average()
            .orElse(0.0);
        
        // 归一化到0-1
        double sentimentScore = (avgSentiment + 1.0) / 2.0;
        
        score = (positiveRatio * 0.6 + sentimentScore * 0.4);
        
        return BigDecimal.valueOf(score).setScale(3, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算神经质 (Neuroticism)
     * 基于：负面情感、情绪波动、焦虑表达
     */
    private BigDecimal calculateNeuroticism(List<ConversationFeature> features) {
        double score = 0.5;
        
        // 负面情感比例
        long negativeCount = features.stream()
            .filter(ConversationFeature::isNegativeSentiment)
            .count();
        double negativeRatio = negativeCount / (double) features.size();
        
        // 情感波动（标准差）
        double[] sentiments = features.stream()
            .filter(f -> f.getSentimentScore() != null)
            .mapToDouble(f -> f.getSentimentScore().doubleValue())
            .toArray();
        
        double volatility = 0.0;
        if (sentiments.length > 1) {
            double mean = 0.0;
            for (double s : sentiments) mean += s;
            mean /= sentiments.length;
            
            double variance = 0.0;
            for (double s : sentiments) {
                variance += Math.pow(s - mean, 2);
            }
            volatility = Math.sqrt(variance / sentiments.length);
        }
        
        score = (negativeRatio * 0.6 + volatility * 0.4);
        
        return BigDecimal.valueOf(score).setScale(3, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算置信度
     * 基于数据量
     */
    private BigDecimal calculateConfidence(int dataCount) {
        // 数据越多，置信度越高
        // 10条消息 -> 0.3
        // 50条消息 -> 0.6
        // 100条以上 -> 0.8+
        double confidence = Math.min(0.9, 0.2 + (dataCount / 100.0) * 0.7);
        return BigDecimal.valueOf(confidence).setScale(3, RoundingMode.HALF_UP);
    }
    
    /**
     * 分析回答长度偏好
     */
    private String analyzeResponseLengthPreference(List<ConversationFeature> features) {
        double avgLength = features.stream()
            .filter(f -> f.getMessageLength() != null)
            .mapToInt(ConversationFeature::getMessageLength)
            .average()
            .orElse(50.0);
        
        if (avgLength < 30) {
            return "concise";
        } else if (avgLength > 100) {
            return "detailed";
        } else {
            return "balanced";
        }
    }
    
    /**
     * 分析语言风格偏好
     */
    private String analyzeLanguageStylePreference(List<ConversationFeature> features) {
        // 基于表情和感叹号使用
        double casualScore = features.stream()
            .filter(f -> f.isEmotionallyExpressive())
            .count() / (double) features.size();
        
        if (casualScore > 0.5) {
            return "casual";
        } else if (casualScore < 0.2) {
            return "formal";
        } else {
            return "balanced";
        }
    }
    
    /**
     * 分析互动风格
     */
    private String analyzeInteractionStyle(List<ConversationFeature> features) {
        // 基于问题比例
        double questionRatio = features.stream()
            .filter(ConversationFeature::isQuestion)
            .count() / (double) features.size();
        
        if (questionRatio > 0.5) {
            return "active";
        } else if (questionRatio < 0.2) {
            return "passive";
        } else {
            return "balanced";
        }
    }
    
    /**
     * 获取或创建默认画像
     */
    private UserProfile getOrCreateDefaultProfile(Long userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile = userProfileRepository.create(profile);
        }
        return profile;
    }
}
