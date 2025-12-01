package com.example.voicebox.app.device.service;

import com.example.voicebox.app.device.chat.ChatMessage;
import com.example.voicebox.app.device.chat.ChatSession;
import com.example.voicebox.app.device.domain.UserProfile;
import com.example.voicebox.app.device.interceptor.MessageFeatureInterceptor;
import com.example.voicebox.app.device.repository.UserProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 聊天个性化集成服务
 * 将个性分析功能集成到聊天流程中
 * 
 * @author VoiceBox Team
 * @since 2.0
 */
@Service
public class ChatPersonalityIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatPersonalityIntegrationService.class);
    
    @Autowired
    private MessageFeatureInterceptor messageFeatureInterceptor;
    
    @Autowired
    private ResponseStrategyService responseStrategyService;
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    @Autowired
    private PersonalityAnalysisService personalityAnalysisService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 处理用户消息
     * 在用户发送消息后调用，异步提取特征
     */
    public void handleUserMessage(ChatMessage message) {
        try {
            if (message == null || message.getSessionId() == null) {
                return;
            }
            
            // 获取用户ID（从session中获取）
            Long userId = getUserIdFromMessage(message);
            if (userId == null) {
                logger.warn("无法获取用户ID，跳过个性化处理");
                return;
            }
            
            // 异步提取消息特征
            messageFeatureInterceptor.processMessage(
                userId,
                message.getSessionId(),
                message.getId(),
                message.getContent()
            );
            
            logger.debug("已触发消息特征提取 - userId: {}, messageId: {}", userId, message.getId());
            
        } catch (Exception e) {
            logger.error("处理用户消息失败 - messageId: " + message.getId(), e);
        }
    }
    
    /**
     * 生成个性化的AI响应提示词
     * 在调用AI生成响应前调用
     */
    public String generatePersonalizedPrompt(Long userId, String basePrompt) {
        try {
            // 获取用户的响应策略
            ResponseStrategyService.ResponseStrategy strategy = 
                responseStrategyService.generateStrategy(userId);
            
            // 构建个性化提示词
            StringBuilder prompt = new StringBuilder();
            
            // 基础提示词
            if (basePrompt != null && !basePrompt.isEmpty()) {
                prompt.append(basePrompt);
            } else {
                prompt.append("你是一个智能助手。");
            }
            
            // 根据策略调整
            prompt.append(buildStrategyPrompt(strategy));
            
            logger.debug("生成个性化提示词 - userId: {}, strategy: {}", userId, strategy);
            
            return prompt.toString();
            
        } catch (Exception e) {
            logger.error("生成个性化提示词失败 - userId: " + userId, e);
            return basePrompt != null ? basePrompt : "你是一个智能助手。";
        }
    }
    
    /**
     * 根据策略构建提示词
     */
    private String buildStrategyPrompt(ResponseStrategyService.ResponseStrategy strategy) {
        StringBuilder prompt = new StringBuilder();
        
        // 回答长度
        switch (strategy.getResponseLength()) {
            case "concise":
                prompt.append(" 请提供简洁明了的回答，避免冗长。");
                break;
            case "detailed":
                prompt.append(" 请提供详细深入的解释，包含充分的细节和例子。");
                break;
            case "balanced":
                prompt.append(" 请提供适度详细的回答，平衡简洁性和完整性。");
                break;
        }
        
        // 语言风格
        switch (strategy.getLanguageStyle()) {
            case "casual":
                prompt.append(" 使用轻松友好的语气，像朋友一样交流。");
                break;
            case "formal":
                prompt.append(" 使用正式专业的语气，保持严谨。");
                break;
            case "balanced":
                prompt.append(" 使用自然得体的语气。");
                break;
        }
        
        // 互动语气
        switch (strategy.getInteractionTone()) {
            case "friendly":
                prompt.append(" 保持热情友好，积极互动。");
                break;
            case "professional":
                prompt.append(" 保持专业严谨，注重准确性。");
                break;
        }
        
        // 示例使用
        if (strategy.isExampleUsage()) {
            prompt.append(" 适当使用示例来说明概念。");
        }
        
        // 代码格式化
        if (strategy.isCodeFormatting()) {
            prompt.append(" 代码要格式规范，添加必要的注释。");
        }
        
        // 个性化调整
        if (strategy.getPromptAdjustment() != null && !strategy.getPromptAdjustment().isEmpty()) {
            prompt.append(" ").append(strategy.getPromptAdjustment());
        }
        
        return prompt.toString();
    }
    
    /**
     * 更新会话的个性化上下文
     * 在会话开始或用户画像更新时调用
     */
    public void updateSessionPersonalizationContext(ChatSession session) {
        try {
            Long userId = session.getUserId();
            if (userId == null) {
                return;
            }
            
            // 获取用户画像
            UserProfile profile = userProfileRepository.findByUserId(userId);
            if (profile == null || !profile.isConfident()) {
                logger.debug("用户画像不存在或置信度不足 - userId: {}", userId);
                return;
            }
            
            // 构建个性化上下文
            Map<String, Object> context = new HashMap<>();
            context.put("profileConfidence", profile.getConfidenceScore());
            context.put("personalityType", profile.getPersonalityType());
            context.put("responseLengthPreference", profile.getResponseLengthPreference());
            context.put("languageStylePreference", profile.getLanguageStylePreference());
            context.put("interactionStyle", profile.getInteractionStyle());
            
            // 大五人格维度
            Map<String, Object> personality = new HashMap<>();
            personality.put("openness", profile.getOpenness());
            personality.put("conscientiousness", profile.getConscientiousness());
            personality.put("extraversion", profile.getExtraversion());
            personality.put("agreeableness", profile.getAgreeableness());
            personality.put("neuroticism", profile.getNeuroticism());
            context.put("personality", personality);
            
            // 序列化为JSON
            String contextJson = objectMapper.writeValueAsString(context);
            session.setPersonalizationContext(contextJson);
            
            logger.debug("更新会话个性化上下文 - sessionId: {}, userId: {}", 
                session.getId(), userId);
            
        } catch (Exception e) {
            logger.error("更新会话个性化上下文失败 - sessionId: " + session.getId(), e);
        }
    }
    
    /**
     * 检查用户画像是否需要更新
     * 在会话开始时调用
     */
    public boolean shouldUpdateProfile(Long userId) {
        try {
            UserProfile profile = userProfileRepository.findByUserId(userId);
            
            if (profile == null) {
                // 没有画像，需要创建
                return true;
            }
            
            // 检查是否需要更新
            return profile.needsUpdate();
            
        } catch (Exception e) {
            logger.error("检查画像更新状态失败 - userId: " + userId, e);
            return false;
        }
    }
    
    /**
     * 触发用户画像分析
     * 可以在后台异步执行
     */
    public void triggerProfileAnalysis(Long userId) {
        try {
            logger.info("触发用户画像分析 - userId: {}", userId);
            
            // 异步执行分析
            new Thread(() -> {
                try {
                    personalityAnalysisService.analyzePersonality(userId);
                    logger.info("用户画像分析完成 - userId: {}", userId);
                } catch (Exception e) {
                    logger.error("用户画像分析失败 - userId: " + userId, e);
                }
            }).start();
            
        } catch (Exception e) {
            logger.error("触发画像分析失败 - userId: " + userId, e);
        }
    }
    
    /**
     * 获取用户的个性化建议
     * 用于前端展示
     */
    public Map<String, Object> getPersonalizationSuggestions(Long userId) {
        Map<String, Object> suggestions = new HashMap<>();
        
        try {
            UserProfile profile = userProfileRepository.findByUserId(userId);
            
            if (profile == null || !profile.isConfident()) {
                suggestions.put("available", false);
                suggestions.put("message", "需要更多对话数据来分析您的偏好");
                suggestions.put("minMessages", 50);
                suggestions.put("currentMessages", profile != null ? profile.getTotalMessages() : 0);
                return suggestions;
            }
            
            suggestions.put("available", true);
            suggestions.put("personalityType", profile.getPersonalityType());
            suggestions.put("confidence", profile.getConfidenceScore());
            
            // 根据性格特征提供建议
            java.util.List<String> tips = new java.util.ArrayList<>();
            
            if (profile.getOpenness().compareTo(java.math.BigDecimal.valueOf(0.6)) > 0) {
                tips.add("您对新想法很感兴趣，我会为您提供创新的解决方案");
            }
            
            if (profile.getConscientiousness().compareTo(java.math.BigDecimal.valueOf(0.6)) > 0) {
                tips.add("您注重细节，我会提供更详细准确的信息");
            }
            
            if (profile.getExtraversion().compareTo(java.math.BigDecimal.valueOf(0.6)) > 0) {
                tips.add("您喜欢互动交流，我会使用更友好的语气");
            }
            
            suggestions.put("tips", tips);
            
            // 当前偏好设置
            Map<String, String> preferences = new HashMap<>();
            preferences.put("responseLength", profile.getResponseLengthPreference());
            preferences.put("languageStyle", profile.getLanguageStylePreference());
            preferences.put("interactionStyle", profile.getInteractionStyle());
            suggestions.put("preferences", preferences);
            
        } catch (Exception e) {
            logger.error("获取个性化建议失败 - userId: " + userId, e);
            suggestions.put("available", false);
            suggestions.put("error", e.getMessage());
        }
        
        return suggestions;
    }
    
    /**
     * 从消息中获取用户ID
     * 这里需要根据实际的session管理方式来实现
     */
    private Long getUserIdFromMessage(ChatMessage message) {
        // TODO: 实现从session获取userId的逻辑
        // 这里暂时返回null，需要根据实际情况实现
        return null;
    }
}
