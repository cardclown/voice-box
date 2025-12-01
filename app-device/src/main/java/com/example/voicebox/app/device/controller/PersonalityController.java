package com.example.voicebox.app.device.controller;

import com.example.voicebox.app.device.domain.ConversationFeature;
import com.example.voicebox.app.device.domain.UserFeedback;
import com.example.voicebox.app.device.domain.UserProfile;
import com.example.voicebox.app.device.repository.ConversationFeatureRepository;
import com.example.voicebox.app.device.repository.UserFeedbackRepository;
import com.example.voicebox.app.device.repository.UserProfileRepository;
import com.example.voicebox.app.device.service.FeatureExtractionService;
import com.example.voicebox.app.device.service.LearningService;
import com.example.voicebox.app.device.service.PersonalityAnalysisService;
import com.example.voicebox.app.device.service.ResponseStrategyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 个性分析API控制器
 * 
 * @author VoiceBox Team
 * @since 2.0
 */
@RestController
@RequestMapping("/api/personality")
@CrossOrigin(origins = "*")
public class PersonalityController {
    
    private static final Logger logger = LoggerFactory.getLogger(PersonalityController.class);
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    @Autowired
    private ConversationFeatureRepository conversationFeatureRepository;
    
    @Autowired
    private UserFeedbackRepository userFeedbackRepository;
    
    @Autowired
    private PersonalityAnalysisService personalityAnalysisService;
    
    @Autowired
    private FeatureExtractionService featureExtractionService;
    
    @Autowired
    private ResponseStrategyService responseStrategyService;
    
    @Autowired
    private LearningService learningService;
    
    /**
     * 获取用户画像
     */
    @GetMapping("/profile/{userId}")
    public ResponseEntity<Map<String, Object>> getUserProfile(@PathVariable Long userId) {
        try {
            UserProfile profile = userProfileRepository.findByUserId(userId);
            
            if (profile == null) {
                return ResponseEntity.ok(createResponse(false, "用户画像不存在", null));
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("profile", profile);
            data.put("personalityType", profile.getPersonalityType());
            data.put("isConfident", profile.isConfident());
            data.put("needsUpdate", profile.needsUpdate());
            
            return ResponseEntity.ok(createResponse(true, "获取成功", data));
            
        } catch (Exception e) {
            logger.error("获取用户画像失败 - userId: " + userId, e);
            return ResponseEntity.ok(createResponse(false, "获取失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 分析用户个性
     */
    @PostMapping("/analyze/{userId}")
    public ResponseEntity<Map<String, Object>> analyzePersonality(@PathVariable Long userId) {
        try {
            UserProfile profile = personalityAnalysisService.analyzePersonality(userId);
            
            Map<String, Object> data = new HashMap<>();
            data.put("profile", profile);
            data.put("message", "分析完成");
            
            return ResponseEntity.ok(createResponse(true, "分析成功", data));
            
        } catch (Exception e) {
            logger.error("分析用户个性失败 - userId: " + userId, e);
            return ResponseEntity.ok(createResponse(false, "分析失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 提取消息特征
     */
    @PostMapping("/extract-features")
    public ResponseEntity<Map<String, Object>> extractFeatures(@RequestBody Map<String, Object> request) {
        try {
            Long userId = getLongValue(request.get("userId"));
            Long sessionId = getLongValue(request.get("sessionId"));
            Long messageId = getLongValue(request.get("messageId"));
            String content = (String) request.get("content");
            
            ConversationFeature feature = featureExtractionService.extractFeatures(
                userId, sessionId, messageId, content
            );
            
            conversationFeatureRepository.create(feature);
            
            return ResponseEntity.ok(createResponse(true, "特征提取成功", feature));
            
        } catch (Exception e) {
            logger.error("提取消息特征失败", e);
            return ResponseEntity.ok(createResponse(false, "提取失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取响应策略
     */
    @GetMapping("/strategy/{userId}")
    public ResponseEntity<Map<String, Object>> getResponseStrategy(@PathVariable Long userId) {
        try {
            ResponseStrategyService.ResponseStrategy strategy = 
                responseStrategyService.generateStrategy(userId);
            
            return ResponseEntity.ok(createResponse(true, "获取成功", strategy.toMap()));
            
        } catch (Exception e) {
            logger.error("获取响应策略失败 - userId: " + userId, e);
            return ResponseEntity.ok(createResponse(false, "获取失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 提交用户反馈
     */
    @PostMapping("/feedback")
    public ResponseEntity<Map<String, Object>> submitFeedback(@RequestBody UserFeedback feedback) {
        try {
            // 保存反馈
            userFeedbackRepository.create(feedback);
            
            // 触发学习
            learningService.learnFromFeedback(feedback.getUserId(), feedback);
            
            return ResponseEntity.ok(createResponse(true, "反馈提交成功", null));
            
        } catch (Exception e) {
            logger.error("提交反馈失败", e);
            return ResponseEntity.ok(createResponse(false, "提交失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取用户统计信息
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<Map<String, Object>> getUserStats(@PathVariable Long userId) {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 画像信息
            UserProfile profile = userProfileRepository.findByUserId(userId);
            if (profile != null) {
                stats.put("profile", profile);
            }
            
            // 对话特征统计
            Map<String, Object> featureStats = 
                conversationFeatureRepository.getAverageFeaturesByUserId(userId);
            stats.put("averageFeatures", featureStats);
            
            // 情感分布
            Map<String, Object> sentimentDist = 
                conversationFeatureRepository.getSentimentDistribution(userId);
            stats.put("sentimentDistribution", sentimentDist);
            
            // 反馈统计
            Map<String, Object> feedbackStats = 
                userFeedbackRepository.getFeedbackStatistics(userId);
            stats.put("feedbackStatistics", feedbackStats);
            
            // 学习效果
            Map<String, Object> learningEffect = 
                learningService.evaluateLearningEffect(userId);
            stats.put("learningEffect", learningEffect);
            
            return ResponseEntity.ok(createResponse(true, "获取成功", stats));
            
        } catch (Exception e) {
            logger.error("获取用户统计失败 - userId: " + userId, e);
            return ResponseEntity.ok(createResponse(false, "获取失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取对话特征历史
     */
    @GetMapping("/features/{userId}")
    public ResponseEntity<Map<String, Object>> getConversationFeatures(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<ConversationFeature> features = 
                conversationFeatureRepository.findRecentByUserId(userId, limit);
            
            return ResponseEntity.ok(createResponse(true, "获取成功", features));
            
        } catch (Exception e) {
            logger.error("获取对话特征失败 - userId: " + userId, e);
            return ResponseEntity.ok(createResponse(false, "获取失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取反馈历史
     */
    @GetMapping("/feedback/{userId}")
    public ResponseEntity<Map<String, Object>> getFeedbackHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<UserFeedback> feedbacks = 
                userFeedbackRepository.findRecentByUserId(userId, limit);
            
            return ResponseEntity.ok(createResponse(true, "获取成功", feedbacks));
            
        } catch (Exception e) {
            logger.error("获取反馈历史失败 - userId: " + userId, e);
            return ResponseEntity.ok(createResponse(false, "获取失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 批量学习历史反馈
     */
    @PostMapping("/learn/{userId}")
    public ResponseEntity<Map<String, Object>> batchLearn(@PathVariable Long userId) {
        try {
            learningService.batchLearnFromHistory(userId);
            
            return ResponseEntity.ok(createResponse(true, "学习完成", null));
            
        } catch (Exception e) {
            logger.error("批量学习失败 - userId: " + userId, e);
            return ResponseEntity.ok(createResponse(false, "学习失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取性格维度统计
     */
    @GetMapping("/personality-stats")
    public ResponseEntity<Map<String, Object>> getPersonalityStatistics() {
        try {
            Map<String, Object> stats = userProfileRepository.getPersonalityStatistics();
            
            return ResponseEntity.ok(createResponse(true, "获取成功", stats));
            
        } catch (Exception e) {
            logger.error("获取性格统计失败", e);
            return ResponseEntity.ok(createResponse(false, "获取失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "ok");
        health.put("service", "personality-analysis");
        health.put("version", "2.0");
        
        return ResponseEntity.ok(createResponse(true, "服务正常", health));
    }
    
    // 辅助方法
    
    private Map<String, Object> createResponse(boolean success, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        response.put("data", data);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
    
    private Long getLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        return null;
    }
}
