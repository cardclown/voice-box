package com.example.voicebox.app.device.service;

import com.example.voicebox.app.device.domain.UserProfile;
import com.example.voicebox.app.device.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 响应策略服务
 * 根据用户画像调整AI响应策略
 * 
 * @author VoiceBox Team
 * @since 2.0
 */
@Service
public class ResponseStrategyService {
    
    private static final Logger logger = LoggerFactory.getLogger(ResponseStrategyService.class);
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    /**
     * 为用户生成响应策略
     */
    public ResponseStrategy generateStrategy(Long userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId);
        
        if (profile == null || !profile.isConfident()) {
            // 如果没有画像或置信度不够，返回默认策略
            return getDefaultStrategy();
        }
        
        ResponseStrategy strategy = new ResponseStrategy();
        
        // 根据画像调整策略
        strategy.setResponseLength(determineResponseLength(profile));
        strategy.setLanguageStyle(determineLanguageStyle(profile));
        strategy.setDetailLevel(determineDetailLevel(profile));
        strategy.setExampleUsage(determineExampleUsage(profile));
        strategy.setInteractionTone(determineInteractionTone(profile));
        strategy.setCodeFormatting(determineCodeFormatting(profile));
        
        // 生成提示词调整
        strategy.setPromptAdjustment(generatePromptAdjustment(profile));
        
        logger.debug("为用户 {} 生成响应策略: {}", userId, strategy);
        
        return strategy;
    }
    
    /**
     * 确定响应长度
     */
    private String determineResponseLength(UserProfile profile) {
        // 基于用户的消息长度偏好
        String pref = profile.getResponseLengthPreference();
        if (pref != null) {
            return pref;
        }
        
        // 基于尽责性：高尽责性的用户可能喜欢详细的回答
        if (profile.getConscientiousness().compareTo(new BigDecimal("0.6")) > 0) {
            return "detailed";
        } else if (profile.getConscientiousness().compareTo(new BigDecimal("0.4")) < 0) {
            return "concise";
        }
        
        return "balanced";
    }
    
    /**
     * 确定语言风格
     */
    private String determineLanguageStyle(UserProfile profile) {
        String pref = profile.getLanguageStylePreference();
        if (pref != null) {
            return pref;
        }
        
        // 基于外向性：高外向性的用户可能喜欢轻松的风格
        if (profile.getExtraversion().compareTo(new BigDecimal("0.6")) > 0) {
            return "casual";
        } else if (profile.getExtraversion().compareTo(new BigDecimal("0.4")) < 0) {
            return "formal";
        }
        
        return "balanced";
    }
    
    /**
     * 确定细节层次
     */
    private String determineDetailLevel(UserProfile profile) {
        // 基于开放性和尽责性
        BigDecimal openness = profile.getOpenness();
        BigDecimal conscientiousness = profile.getConscientiousness();
        
        double detailScore = (openness.doubleValue() + conscientiousness.doubleValue()) / 2.0;
        
        if (detailScore > 0.6) {
            return "high";
        } else if (detailScore < 0.4) {
            return "low";
        }
        
        return "medium";
    }
    
    /**
     * 确定示例使用
     */
    private boolean determineExampleUsage(UserProfile profile) {
        // 开放性高的用户可能更喜欢看到示例
        return profile.getOpenness().compareTo(new BigDecimal("0.5")) > 0;
    }
    
    /**
     * 确定互动语气
     */
    private String determineInteractionTone(UserProfile profile) {
        // 基于宜人性和外向性
        BigDecimal agreeableness = profile.getAgreeableness();
        BigDecimal extraversion = profile.getExtraversion();
        
        double toneScore = (agreeableness.doubleValue() + extraversion.doubleValue()) / 2.0;
        
        if (toneScore > 0.6) {
            return "friendly"; // 友好热情
        } else if (toneScore < 0.4) {
            return "professional"; // 专业严谨
        }
        
        return "neutral"; // 中性
    }
    
    /**
     * 确定代码格式化偏好
     */
    private boolean determineCodeFormatting(UserProfile profile) {
        // 尽责性高的用户可能更注重代码格式
        return profile.getConscientiousness().compareTo(new BigDecimal("0.5")) > 0;
    }
    
    /**
     * 生成提示词调整
     */
    private String generatePromptAdjustment(UserProfile profile) {
        StringBuilder adjustment = new StringBuilder();
        
        // 根据性格特征调整提示词
        if (profile.getOpenness().compareTo(new BigDecimal("0.6")) > 0) {
            adjustment.append("用户对新想法和创新方法感兴趣。");
        }
        
        if (profile.getConscientiousness().compareTo(new BigDecimal("0.6")) > 0) {
            adjustment.append("用户注重细节和准确性。");
        }
        
        if (profile.getExtraversion().compareTo(new BigDecimal("0.6")) > 0) {
            adjustment.append("用户喜欢互动和交流。");
        }
        
        if (profile.getAgreeableness().compareTo(new BigDecimal("0.6")) > 0) {
            adjustment.append("用户友好且合作。");
        }
        
        if (profile.getNeuroticism().compareTo(new BigDecimal("0.6")) > 0) {
            adjustment.append("用户可能需要更多的支持和鼓励。");
        }
        
        // 根据偏好调整
        String lengthPref = profile.getResponseLengthPreference();
        if ("concise".equals(lengthPref)) {
            adjustment.append("请提供简洁的回答。");
        } else if ("detailed".equals(lengthPref)) {
            adjustment.append("请提供详细的解释。");
        }
        
        String stylePref = profile.getLanguageStylePreference();
        if ("casual".equals(stylePref)) {
            adjustment.append("使用轻松友好的语气。");
        } else if ("formal".equals(stylePref)) {
            adjustment.append("使用正式专业的语气。");
        }
        
        return adjustment.toString();
    }
    
    /**
     * 获取默认策略
     */
    private ResponseStrategy getDefaultStrategy() {
        ResponseStrategy strategy = new ResponseStrategy();
        strategy.setResponseLength("balanced");
        strategy.setLanguageStyle("balanced");
        strategy.setDetailLevel("medium");
        strategy.setExampleUsage(true);
        strategy.setInteractionTone("neutral");
        strategy.setCodeFormatting(true);
        strategy.setPromptAdjustment("");
        return strategy;
    }
    
    /**
     * 响应策略类
     */
    public static class ResponseStrategy {
        private String responseLength;      // concise/balanced/detailed
        private String languageStyle;       // formal/balanced/casual
        private String detailLevel;         // low/medium/high
        private boolean exampleUsage;       // 是否使用示例
        private String interactionTone;     // professional/neutral/friendly
        private boolean codeFormatting;     // 是否注重代码格式
        private String promptAdjustment;    // 提示词调整
        
        // Getters and Setters
        public String getResponseLength() {
            return responseLength;
        }
        
        public void setResponseLength(String responseLength) {
            this.responseLength = responseLength;
        }
        
        public String getLanguageStyle() {
            return languageStyle;
        }
        
        public void setLanguageStyle(String languageStyle) {
            this.languageStyle = languageStyle;
        }
        
        public String getDetailLevel() {
            return detailLevel;
        }
        
        public void setDetailLevel(String detailLevel) {
            this.detailLevel = detailLevel;
        }
        
        public boolean isExampleUsage() {
            return exampleUsage;
        }
        
        public void setExampleUsage(boolean exampleUsage) {
            this.exampleUsage = exampleUsage;
        }
        
        public String getInteractionTone() {
            return interactionTone;
        }
        
        public void setInteractionTone(String interactionTone) {
            this.interactionTone = interactionTone;
        }
        
        public boolean isCodeFormatting() {
            return codeFormatting;
        }
        
        public void setCodeFormatting(boolean codeFormatting) {
            this.codeFormatting = codeFormatting;
        }
        
        public String getPromptAdjustment() {
            return promptAdjustment;
        }
        
        public void setPromptAdjustment(String promptAdjustment) {
            this.promptAdjustment = promptAdjustment;
        }
        
        /**
         * 转换为Map格式（用于JSON序列化）
         */
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("responseLength", responseLength);
            map.put("languageStyle", languageStyle);
            map.put("detailLevel", detailLevel);
            map.put("exampleUsage", exampleUsage);
            map.put("interactionTone", interactionTone);
            map.put("codeFormatting", codeFormatting);
            map.put("promptAdjustment", promptAdjustment);
            return map;
        }
        
        @Override
        public String toString() {
            return "ResponseStrategy{" +
                    "responseLength='" + responseLength + '\'' +
                    ", languageStyle='" + languageStyle + '\'' +
                    ", detailLevel='" + detailLevel + '\'' +
                    ", exampleUsage=" + exampleUsage +
                    ", interactionTone='" + interactionTone + '\'' +
                    ", codeFormatting=" + codeFormatting +
                    '}';
        }
    }
}
