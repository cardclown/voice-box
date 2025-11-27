package com.example.voicebox.app.device.service;

import com.example.voicebox.app.device.domain.UserTag;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 个性化服务
 * 基于用户标签和历史行为定制 AI 响应
 */
@Service
public class PersonalizationService {

    private final TagGenerationService tagGenerationService;

    public PersonalizationService(TagGenerationService tagGenerationService) {
        this.tagGenerationService = tagGenerationService;
    }

    /**
     * 构建个性化提示词
     * 将用户标签和偏好融入 AI 提示
     */
    public String buildPersonalizedPrompt(Long userId, String userMessage) {
        List<UserTag> tags = tagGenerationService.getUserTags(userId);
        
        if (tags.isEmpty()) {
            return userMessage;
        }

        StringBuilder promptBuilder = new StringBuilder();
        
        // 1. 添加用户偏好上下文
        String preferences = extractPreferences(tags);
        if (!preferences.isEmpty()) {
            promptBuilder.append("[用户偏好: ").append(preferences).append("]\n");
        }
        
        // 2. 添加用户兴趣上下文
        String interests = extractInterests(tags);
        if (!interests.isEmpty()) {
            promptBuilder.append("[用户兴趣: ").append(interests).append("]\n");
        }
        
        // 3. 添加用户特征上下文
        String characteristics = extractCharacteristics(tags);
        if (!characteristics.isEmpty()) {
            promptBuilder.append("[用户特征: ").append(characteristics).append("]\n");
        }
        
        // 4. 添加原始用户消息
        promptBuilder.append("\n用户消息: ").append(userMessage);
        
        return promptBuilder.toString();
    }

    /**
     * 增强聊天请求（添加个性化上下文）
     */
    public Map<String, Object> enhanceWithUserContext(Long userId, Map<String, Object> chatRequest) {
        List<UserTag> tags = tagGenerationService.getUserTags(userId);
        
        Map<String, Object> enhanced = new HashMap<>(chatRequest);
        
        // 添加个性化参数
        ResponseStyle style = determineResponseStyle(userId, tags);
        enhanced.put("maxTokens", style.getMaxTokens());
        enhanced.put("temperature", style.getTemperature());
        enhanced.put("tone", style.getTone());
        enhanced.put("detailLevel", style.getDetailLevel());
        
        // 添加用户上下文
        Map<String, Object> userContext = new HashMap<>();
        userContext.put("preferences", extractPreferences(tags));
        userContext.put("interests", extractInterests(tags));
        userContext.put("characteristics", extractCharacteristics(tags));
        enhanced.put("userContext", userContext);
        
        return enhanced;
    }

    /**
     * 确定响应风格
     */
    public ResponseStyle determineResponseStyle(Long userId, List<UserTag> tags) {
        ResponseStyle style = new ResponseStyle();
        
        // 默认值
        style.setMaxTokens(2000);
        style.setTemperature(0.7);
        style.setTone("balanced");
        style.setDetailLevel("balanced");
        
        // 根据标签调整
        for (UserTag tag : tags) {
            String tagName = tag.getTagName();
            double confidence = tag.getConfidence().doubleValue();
            
            // 详细程度偏好
            if ("prefers_concise".equals(tagName) && confidence > 0.6) {
                style.setMaxTokens(1000);
                style.setDetailLevel("concise");
            } else if ("prefers_detailed".equals(tagName) && confidence > 0.6) {
                style.setMaxTokens(3000);
                style.setDetailLevel("detailed");
            }
            
            // 语气偏好
            if ("prefers_formal_tone".equals(tagName) && confidence > 0.6) {
                style.setTone("formal");
                style.setTemperature(0.5);
            } else if ("prefers_casual_tone".equals(tagName) && confidence > 0.6) {
                style.setTone("casual");
                style.setTemperature(0.8);
            }
            
            // 技术用户
            if (tagName.startsWith("tech_") && confidence > 0.7) {
                style.addPreferredFormat("code");
                style.setDetailLevel("detailed");
            }
            
            // 喜欢列表
            if ("likes_lists".equals(tagName) && confidence > 0.6) {
                style.addPreferredFormat("lists");
            }
        }
        
        return style;
    }

    /**
     * 检索历史上下文
     */
    public String retrieveHistoricalContext(Long userId, String currentTopic) {
        // 简化实现：返回相关的用户标签作为上下文
        List<UserTag> tags = tagGenerationService.getUserTags(userId);
        
        // 过滤与当前主题相关的标签
        List<UserTag> relevantTags = tags.stream()
            .filter(tag -> isRelevantToTopic(tag, currentTopic))
            .collect(Collectors.toList());
        
        if (relevantTags.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder("历史上下文: ");
        for (UserTag tag : relevantTags) {
            context.append(tag.getTagName()).append(" ");
        }
        
        return context.toString();
    }

    /**
     * 动态适应（基于标签更新）
     */
    public void adaptToUserFeedback(Long userId, String feedback, boolean positive) {
        // 根据用户反馈调整标签置信度
        List<UserTag> tags = tagGenerationService.getUserTags(userId);
        
        for (UserTag tag : tags) {
            if (tag.getCategory().equals("preference")) {
                double currentConfidence = tag.getConfidence().doubleValue();
                double adjustment = positive ? 0.05 : -0.05;
                double newConfidence = Math.max(0.0, Math.min(1.0, currentConfidence + adjustment));
                
                tagGenerationService.updateTagConfidence(tag.getId(), newConfidence);
            }
        }
    }

    // ========== 辅助方法 ==========

    private String extractPreferences(List<UserTag> tags) {
        return tags.stream()
            .filter(tag -> "preference".equals(tag.getCategory()))
            .filter(tag -> tag.getConfidence().doubleValue() > 0.5)
            .map(UserTag::getTagName)
            .collect(Collectors.joining(", "));
    }

    private String extractInterests(List<UserTag> tags) {
        return tags.stream()
            .filter(tag -> "semantic".equals(tag.getCategory()))
            .filter(tag -> tag.getConfidence().doubleValue() > 0.6)
            .map(UserTag::getTagName)
            .collect(Collectors.joining(", "));
    }

    private String extractCharacteristics(List<UserTag> tags) {
        return tags.stream()
            .filter(tag -> "behavioral".equals(tag.getCategory()))
            .filter(tag -> tag.getConfidence().doubleValue() > 0.6)
            .map(UserTag::getTagName)
            .collect(Collectors.joining(", "));
    }

    private boolean isRelevantToTopic(UserTag tag, String topic) {
        if (topic == null || topic.isEmpty()) {
            return false;
        }
        
        String lowerTopic = topic.toLowerCase();
        String lowerTagName = tag.getTagName().toLowerCase();
        
        return lowerTopic.contains(lowerTagName) || lowerTagName.contains(lowerTopic);
    }

    /**
     * 响应风格配置类
     */
    public static class ResponseStyle {
        private int maxTokens;
        private double temperature;
        private String tone;
        private String detailLevel;
        private List<String> preferredFormats;

        public ResponseStyle() {
            this.preferredFormats = new java.util.ArrayList<>();
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public String getTone() {
            return tone;
        }

        public void setTone(String tone) {
            this.tone = tone;
        }

        public String getDetailLevel() {
            return detailLevel;
        }

        public void setDetailLevel(String detailLevel) {
            this.detailLevel = detailLevel;
        }

        public List<String> getPreferredFormats() {
            return preferredFormats;
        }

        public void setPreferredFormats(List<String> preferredFormats) {
            this.preferredFormats = preferredFormats;
        }

        public void addPreferredFormat(String format) {
            if (!this.preferredFormats.contains(format)) {
                this.preferredFormats.add(format);
            }
        }
    }
}
