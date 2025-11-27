package com.example.voicebox.app.device.service;

import com.example.voicebox.app.device.domain.UserTag;
import com.example.voicebox.app.device.repository.UserTagRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 标签生成服务
 * 基于用户对话和行为模式生成用户标签
 */
@Service
public class TagGenerationService {

    private final UserTagRepository userTagRepository;
    
    // 常见技术关键词
    private static final Set<String> TECH_KEYWORDS = new HashSet<>(Arrays.asList(
        "java", "python", "javascript", "react", "vue", "spring", "docker", 
        "kubernetes", "ai", "machine learning", "database", "sql", "api"
    ));
    
    // 常见主题关键词
    private static final Map<String, Set<String>> TOPIC_KEYWORDS = new HashMap<>();
    static {
        TOPIC_KEYWORDS.put("技术", new HashSet<>(Arrays.asList("编程", "代码", "开发", "bug", "算法")));
        TOPIC_KEYWORDS.put("娱乐", new HashSet<>(Arrays.asList("电影", "音乐", "游戏", "小说", "动漫")));
        TOPIC_KEYWORDS.put("教育", new HashSet<>(Arrays.asList("学习", "课程", "考试", "作业", "教程")));
        TOPIC_KEYWORDS.put("生活", new HashSet<>(Arrays.asList("美食", "旅游", "健康", "运动", "购物")));
    }

    public TagGenerationService(UserTagRepository userTagRepository) {
        this.userTagRepository = userTagRepository;
    }

    /**
     * 从对话中生成语义标签
     */
    public List<UserTag> generateTagsFromConversation(Long userId, Long sessionId, String conversationText) {
        List<UserTag> tags = new ArrayList<>();
        
        if (conversationText == null || conversationText.trim().isEmpty()) {
            return tags;
        }
        
        String lowerText = conversationText.toLowerCase();
        
        // 1. 提取技术标签
        for (String keyword : TECH_KEYWORDS) {
            if (lowerText.contains(keyword)) {
                UserTag tag = createTag(userId, "semantic", "tech_" + keyword, 0.7, "auto");
                tags.add(tag);
            }
        }
        
        // 2. 提取主题标签
        for (Map.Entry<String, Set<String>> entry : TOPIC_KEYWORDS.entrySet()) {
            String topic = entry.getKey();
            Set<String> keywords = entry.getValue();
            
            long matchCount = keywords.stream()
                .filter(lowerText::contains)
                .count();
            
            if (matchCount > 0) {
                double confidence = Math.min(0.9, 0.5 + (matchCount * 0.1));
                UserTag tag = createTag(userId, "semantic", "topic_" + topic, confidence, "auto");
                tags.add(tag);
            }
        }
        
        // 3. 提取问题类型标签
        if (lowerText.contains("怎么") || lowerText.contains("如何") || lowerText.contains("?")) {
            UserTag tag = createTag(userId, "semantic", "question_asker", 0.6, "auto");
            tags.add(tag);
        }
        
        return tags;
    }

    /**
     * 基于行为模式生成行为标签
     */
    public List<UserTag> generateBehavioralTags(Long userId, Map<String, Object> behaviorData) {
        List<UserTag> tags = new ArrayList<>();
        
        // 1. 活跃度标签
        Integer messageCount = (Integer) behaviorData.getOrDefault("messageCount", 0);
        if (messageCount > 100) {
            tags.add(createTag(userId, "behavioral", "frequent_user", 0.9, "auto"));
        } else if (messageCount > 50) {
            tags.add(createTag(userId, "behavioral", "active_user", 0.7, "auto"));
        }
        
        // 2. 时间偏好标签
        Integer nightMessages = (Integer) behaviorData.getOrDefault("nightMessages", 0);
        if (nightMessages > messageCount * 0.5) {
            tags.add(createTag(userId, "behavioral", "night_owl", 0.8, "auto"));
        }
        
        // 3. 会话长度标签
        Double avgSessionDuration = (Double) behaviorData.getOrDefault("avgSessionDuration", 0.0);
        if (avgSessionDuration > 30.0) {
            tags.add(createTag(userId, "behavioral", "deep_thinker", 0.7, "auto"));
        }
        
        // 4. 响应速度标签
        Double avgResponseTime = (Double) behaviorData.getOrDefault("avgResponseTime", 0.0);
        if (avgResponseTime < 5.0) {
            tags.add(createTag(userId, "behavioral", "quick_responder", 0.6, "auto"));
        }
        
        return tags;
    }

    /**
     * 基于偏好生成偏好标签
     */
    public List<UserTag> generatePreferenceTags(Long userId, Map<String, Object> preferenceData) {
        List<UserTag> tags = new ArrayList<>();
        
        // 1. 回复风格偏好
        Integer shortResponses = (Integer) preferenceData.getOrDefault("shortResponses", 0);
        Integer longResponses = (Integer) preferenceData.getOrDefault("longResponses", 0);
        
        if (shortResponses > longResponses * 2) {
            tags.add(createTag(userId, "preference", "prefers_concise", 0.8, "auto"));
        } else if (longResponses > shortResponses * 2) {
            tags.add(createTag(userId, "preference", "prefers_detailed", 0.8, "auto"));
        }
        
        // 2. 内容格式偏好
        Boolean likesCode = (Boolean) preferenceData.getOrDefault("likesCode", false);
        if (likesCode) {
            tags.add(createTag(userId, "preference", "likes_code_examples", 0.7, "auto"));
        }
        
        Boolean likesLists = (Boolean) preferenceData.getOrDefault("likesLists", false);
        if (likesLists) {
            tags.add(createTag(userId, "preference", "likes_lists", 0.7, "auto"));
        }
        
        // 3. 语言风格偏好
        String tonePreference = (String) preferenceData.getOrDefault("tonePreference", "neutral");
        if ("formal".equals(tonePreference)) {
            tags.add(createTag(userId, "preference", "prefers_formal_tone", 0.8, "auto"));
        } else if ("casual".equals(tonePreference)) {
            tags.add(createTag(userId, "preference", "prefers_casual_tone", 0.8, "auto"));
        }
        
        return tags;
    }

    /**
     * 保存标签到数据库（带置信度和时间戳）
     */
    public void saveTags(List<UserTag> tags) {
        for (UserTag tag : tags) {
            // 检查是否已存在相同标签
            UserTag existing = userTagRepository.findByUserIdAndCategoryAndTagName(
                tag.getUserId(), tag.getCategory(), tag.getTagName()
            );
            
            if (existing != null) {
                // 更新置信度（取平均值）
                double newConfidence = (existing.getConfidence().doubleValue() + tag.getConfidence().doubleValue()) / 2.0;
                existing.setConfidence(BigDecimal.valueOf(newConfidence));
                userTagRepository.update(existing);
            } else {
                // 创建新标签
                userTagRepository.create(tag);
            }
        }
    }

    /**
     * 更新标签置信度
     */
    public void updateTagConfidence(Long tagId, double confidence) {
        UserTag tag = userTagRepository.findById(tagId);
        if (tag != null) {
            tag.setConfidence(BigDecimal.valueOf(Math.max(0.0, Math.min(1.0, confidence))));
            userTagRepository.update(tag);
        }
    }

    /**
     * 获取用户的所有标签
     */
    public List<UserTag> getUserTags(Long userId) {
        return userTagRepository.findByUserId(userId);
    }

    /**
     * 获取用户特定类别的标签
     */
    public List<UserTag> getUserTagsByCategory(Long userId, String category) {
        return userTagRepository.findByUserIdAndCategory(userId, category);
    }

    /**
     * 删除低置信度标签
     */
    public int cleanupLowConfidenceTags(Long userId, double minConfidence) {
        List<UserTag> tags = userTagRepository.findByUserId(userId);
        int deletedCount = 0;
        
        for (UserTag tag : tags) {
            if (tag.getConfidence().doubleValue() < minConfidence) {
                userTagRepository.delete(tag.getId());
                deletedCount++;
            }
        }
        
        return deletedCount;
    }

    // ========== 辅助方法 ==========

    private UserTag createTag(Long userId, String category, String tagName, double confidence, String source) {
        UserTag tag = new UserTag();
        tag.setUserId(userId);
        tag.setCategory(category);
        tag.setTagName(tagName);
        tag.setConfidence(BigDecimal.valueOf(confidence));
        tag.setSource(source);
        return tag;
    }

    /**
     * 分析文本情感（简单实现）
     */
    private String analyzeSentiment(String text) {
        String lowerText = text.toLowerCase();
        
        int positiveCount = 0;
        int negativeCount = 0;
        
        String[] positiveWords = {"好", "棒", "喜欢", "感谢", "excellent", "great", "good", "thanks"};
        String[] negativeWords = {"不好", "差", "讨厌", "问题", "错误", "bad", "poor", "error", "problem"};
        
        for (String word : positiveWords) {
            if (lowerText.contains(word)) positiveCount++;
        }
        
        for (String word : negativeWords) {
            if (lowerText.contains(word)) negativeCount++;
        }
        
        if (positiveCount > negativeCount) return "positive";
        if (negativeCount > positiveCount) return "negative";
        return "neutral";
    }
}
