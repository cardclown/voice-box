package com.example.voicebox.app.device.service;

import com.example.voicebox.app.device.domain.UserFeedback;
import com.example.voicebox.app.device.domain.UserProfile;
import com.example.voicebox.app.device.repository.UserFeedbackRepository;
import com.example.voicebox.app.device.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 学习服务
 * 根据用户反馈优化用户画像
 * 
 * @author VoiceBox Team
 * @since 2.0
 */
@Service
public class LearningService {
    
    private static final Logger logger = LoggerFactory.getLogger(LearningService.class);
    
    // 学习率：控制画像更新的速度
    private static final double LEARNING_RATE = 0.1;
    
    @Autowired
    private UserFeedbackRepository userFeedbackRepository;
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    @Autowired
    private PersonalityAnalysisService personalityAnalysisService;
    
    /**
     * 根据反馈学习并更新用户画像
     */
    public void learnFromFeedback(Long userId, UserFeedback feedback) {
        try {
            logger.info("开始从反馈学习 - userId: {}, feedbackType: {}", 
                userId, feedback.getFeedbackType());
            
            UserProfile profile = userProfileRepository.findByUserId(userId);
            if (profile == null) {
                logger.warn("用户画像不存在，跳过学习 - userId: {}", userId);
                return;
            }
            
            // 根据反馈类型调整画像
            switch (feedback.getFeedbackType()) {
                case "like":
                    handlePositiveFeedback(profile, feedback);
                    break;
                case "dislike":
                    handleNegativeFeedback(profile, feedback);
                    break;
                case "regenerate":
                    handleRegenerateFeedback(profile, feedback);
                    break;
                default:
                    logger.debug("未处理的反馈类型: {}", feedback.getFeedbackType());
            }
            
            // 更新画像
            userProfileRepository.update(profile);
            
            logger.info("反馈学习完成 - userId: {}", userId);
            
        } catch (Exception e) {
            logger.error("反馈学习失败 - userId: " + userId, e);
        }
    }
    
    /**
     * 处理正面反馈
     * 强化当前策略
     */
    private void handlePositiveFeedback(UserProfile profile, UserFeedback feedback) {
        // 提高置信度
        BigDecimal currentConfidence = profile.getConfidenceScore();
        BigDecimal newConfidence = currentConfidence.add(
            BigDecimal.valueOf(0.01)
        ).min(BigDecimal.valueOf(1.0));
        profile.setConfidenceScore(newConfidence);
        
        logger.debug("正面反馈：置信度从 {} 提升到 {}", currentConfidence, newConfidence);
    }
    
    /**
     * 处理负面反馈
     * 调整策略
     */
    private void handleNegativeFeedback(UserProfile profile, UserFeedback feedback) {
        // 降低置信度
        BigDecimal currentConfidence = profile.getConfidenceScore();
        BigDecimal newConfidence = currentConfidence.subtract(
            BigDecimal.valueOf(0.02)
        ).max(BigDecimal.valueOf(0.0));
        profile.setConfidenceScore(newConfidence);
        
        // 根据反馈调整偏好
        adjustPreferencesFromNegativeFeedback(profile, feedback);
        
        logger.debug("负面反馈：置信度从 {} 降低到 {}", currentConfidence, newConfidence);
    }
    
    /**
     * 处理重新生成反馈
     * 表示用户对当前回答不满意
     */
    private void handleRegenerateFeedback(UserProfile profile, UserFeedback feedback) {
        // 轻微降低置信度
        BigDecimal currentConfidence = profile.getConfidenceScore();
        BigDecimal newConfidence = currentConfidence.subtract(
            BigDecimal.valueOf(0.01)
        ).max(BigDecimal.valueOf(0.0));
        profile.setConfidenceScore(newConfidence);
        
        logger.debug("重新生成反馈：置信度从 {} 降低到 {}", currentConfidence, newConfidence);
    }
    
    /**
     * 根据负面反馈调整偏好
     */
    private void adjustPreferencesFromNegativeFeedback(UserProfile profile, UserFeedback feedback) {
        // 这里可以根据反馈的具体内容调整偏好
        // 例如：如果用户觉得回答太长，调整长度偏好
        
        String feedbackText = feedback.getFeedbackText();
        if (feedbackText != null) {
            if (feedbackText.contains("太长") || feedbackText.contains("啰嗦")) {
                adjustResponseLengthPreference(profile, "shorter");
            } else if (feedbackText.contains("太短") || feedbackText.contains("不够详细")) {
                adjustResponseLengthPreference(profile, "longer");
            }
            
            if (feedbackText.contains("太正式") || feedbackText.contains("太严肃")) {
                adjustLanguageStylePreference(profile, "casual");
            } else if (feedbackText.contains("太随意") || feedbackText.contains("不够专业")) {
                adjustLanguageStylePreference(profile, "formal");
            }
        }
    }
    
    /**
     * 调整回答长度偏好
     */
    private void adjustResponseLengthPreference(UserProfile profile, String direction) {
        String current = profile.getResponseLengthPreference();
        
        if ("shorter".equals(direction)) {
            if ("detailed".equals(current)) {
                profile.setResponseLengthPreference("balanced");
            } else if ("balanced".equals(current)) {
                profile.setResponseLengthPreference("concise");
            }
        } else if ("longer".equals(direction)) {
            if ("concise".equals(current)) {
                profile.setResponseLengthPreference("balanced");
            } else if ("balanced".equals(current)) {
                profile.setResponseLengthPreference("detailed");
            }
        }
        
        logger.debug("调整回答长度偏好: {} -> {}", current, profile.getResponseLengthPreference());
    }
    
    /**
     * 调整语言风格偏好
     */
    private void adjustLanguageStylePreference(UserProfile profile, String target) {
        String current = profile.getLanguageStylePreference();
        
        if ("casual".equals(target) && !"casual".equals(current)) {
            if ("formal".equals(current)) {
                profile.setLanguageStylePreference("balanced");
            } else {
                profile.setLanguageStylePreference("casual");
            }
        } else if ("formal".equals(target) && !"formal".equals(current)) {
            if ("casual".equals(current)) {
                profile.setLanguageStylePreference("balanced");
            } else {
                profile.setLanguageStylePreference("formal");
            }
        }
        
        logger.debug("调整语言风格偏好: {} -> {}", current, profile.getLanguageStylePreference());
    }
    
    /**
     * 批量学习用户的历史反馈
     */
    public void batchLearnFromHistory(Long userId) {
        try {
            logger.info("开始批量学习历史反馈 - userId: {}", userId);
            
            // 获取用户最近的反馈（最近50条）
            List<UserFeedback> recentFeedback = 
                userFeedbackRepository.findRecentByUserId(userId, 50);
            
            if (recentFeedback.isEmpty()) {
                logger.info("没有历史反馈数据 - userId: {}", userId);
                return;
            }
            
            // 分析反馈模式
            Map<String, Object> feedbackStats = 
                userFeedbackRepository.getFeedbackStatistics(userId);
            
            logger.info("反馈统计 - userId: {}, stats: {}", userId, feedbackStats);
            
            // 根据统计结果调整画像
            adjustProfileFromStatistics(userId, feedbackStats);
            
            logger.info("批量学习完成 - userId: {}, 处理反馈数: {}", 
                userId, recentFeedback.size());
            
        } catch (Exception e) {
            logger.error("批量学习失败 - userId: " + userId, e);
        }
    }
    
    /**
     * 根据反馈统计调整画像
     */
    private void adjustProfileFromStatistics(Long userId, Map<String, Object> stats) {
        UserProfile profile = userProfileRepository.findByUserId(userId);
        if (profile == null) {
            return;
        }
        
        // 计算正面反馈比例
        Long positiveCount = getLongValue(stats.get("positive_count"));
        Long totalCount = getLongValue(stats.get("total_count"));
        
        if (totalCount > 0) {
            double positiveRatio = (double) positiveCount / totalCount;
            
            // 如果正面反馈比例高，提高置信度
            if (positiveRatio > 0.7) {
                BigDecimal newConfidence = profile.getConfidenceScore()
                    .add(BigDecimal.valueOf(0.05))
                    .min(BigDecimal.valueOf(1.0));
                profile.setConfidenceScore(newConfidence);
            } else if (positiveRatio < 0.3) {
                // 如果负面反馈多，降低置信度并重新分析
                BigDecimal newConfidence = profile.getConfidenceScore()
                    .subtract(BigDecimal.valueOf(0.05))
                    .max(BigDecimal.valueOf(0.0));
                profile.setConfidenceScore(newConfidence);
                
                // 触发重新分析
                personalityAnalysisService.analyzePersonality(userId);
            }
            
            userProfileRepository.update(profile);
        }
    }
    
    /**
     * 安全获取Long值
     */
    private Long getLongValue(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).longValue();
        }
        return 0L;
    }
    
    /**
     * 评估学习效果
     */
    public Map<String, Object> evaluateLearningEffect(Long userId) {
        Map<String, Object> result = new java.util.HashMap<>();
        
        try {
            // 获取反馈统计
            Map<String, Object> stats = userFeedbackRepository.getFeedbackStatistics(userId);
            
            Long positiveCount = getLongValue(stats.get("positive_count"));
            Long negativeCount = getLongValue(stats.get("negative_count"));
            Long totalCount = getLongValue(stats.get("total_count"));
            
            if (totalCount > 0) {
                double positiveRatio = (double) positiveCount / totalCount;
                double negativeRatio = (double) negativeCount / totalCount;
                
                result.put("totalFeedback", totalCount);
                result.put("positiveRatio", positiveRatio);
                result.put("negativeRatio", negativeRatio);
                result.put("learningQuality", calculateLearningQuality(positiveRatio));
            }
            
            // 获取画像置信度
            UserProfile profile = userProfileRepository.findByUserId(userId);
            if (profile != null) {
                result.put("profileConfidence", profile.getConfidenceScore());
            }
            
        } catch (Exception e) {
            logger.error("评估学习效果失败 - userId: " + userId, e);
        }
        
        return result;
    }
    
    /**
     * 计算学习质量
     */
    private String calculateLearningQuality(double positiveRatio) {
        if (positiveRatio > 0.7) {
            return "excellent";
        } else if (positiveRatio > 0.5) {
            return "good";
        } else if (positiveRatio > 0.3) {
            return "fair";
        } else {
            return "poor";
        }
    }
}
