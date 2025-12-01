package com.example.voicebox.app.device.controller;

import com.example.voicebox.app.device.chat.ChatMessage;
import com.example.voicebox.app.device.chat.ChatSession;
import com.example.voicebox.app.device.service.ChatPersonalityIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 聊天集成控制器示例
 * 展示如何在聊天流程中使用个性化功能
 * 
 * @author VoiceBox Team
 * @since 2.0
 */
@RestController
@RequestMapping("/api/chat-integration")
@CrossOrigin(origins = "*")
public class ChatIntegrationController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatIntegrationController.class);
    
    @Autowired
    private ChatPersonalityIntegrationService integrationService;
    
    /**
     * 示例：发送消息并触发个性化处理
     */
    @PostMapping("/send-message")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody Map<String, Object> request) {
        try {
            // 1. 创建消息对象（实际应该从数据库保存后获取）
            ChatMessage message = new ChatMessage();
            message.setId(getLongValue(request.get("messageId")));
            message.setSessionId(getLongValue(request.get("sessionId")));
            message.setContent((String) request.get("content"));
            message.setRole("user");
            
            // 2. 处理用户消息（异步提取特征）
            integrationService.handleUserMessage(message);
            
            // 3. 获取用户ID
            Long userId = getLongValue(request.get("userId"));
            
            // 4. 生成个性化提示词
            String basePrompt = "你是一个智能助手。";
            String personalizedPrompt = integrationService.generatePersonalizedPrompt(userId, basePrompt);
            
            // 5. 这里应该调用AI生成响应
            // String aiResponse = callAI(personalizedPrompt, message.getContent());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "消息已发送");
            response.put("personalizedPrompt", personalizedPrompt);
            // response.put("aiResponse", aiResponse);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("发送消息失败", e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * 示例：开始新会话
     */
    @PostMapping("/start-session")
    public ResponseEntity<Map<String, Object>> startSession(@RequestBody Map<String, Object> request) {
        try {
            Long userId = getLongValue(request.get("userId"));
            
            // 1. 创建会话对象
            ChatSession session = new ChatSession();
            session.setUserId(userId);
            session.setTitle((String) request.get("title"));
            
            // 2. 检查是否需要更新用户画像
            boolean needsUpdate = integrationService.shouldUpdateProfile(userId);
            if (needsUpdate) {
                logger.info("用户画像需要更新 - userId: {}", userId);
                // 可以选择在后台触发分析
                integrationService.triggerProfileAnalysis(userId);
            }
            
            // 3. 更新会话的个性化上下文
            integrationService.updateSessionPersonalizationContext(session);
            
            // 4. 获取个性化建议
            Map<String, Object> suggestions = integrationService.getPersonalizationSuggestions(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("session", session);
            response.put("needsProfileUpdate", needsUpdate);
            response.put("personalizationSuggestions", suggestions);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("开始会话失败", e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * 获取个性化建议
     */
    @GetMapping("/suggestions/{userId}")
    public ResponseEntity<Map<String, Object>> getPersonalizationSuggestions(@PathVariable Long userId) {
        try {
            Map<String, Object> suggestions = integrationService.getPersonalizationSuggestions(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", suggestions);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取个性化建议失败 - userId: " + userId, e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * 生成个性化提示词（用于测试）
     */
    @PostMapping("/generate-prompt")
    public ResponseEntity<Map<String, Object>> generatePrompt(@RequestBody Map<String, Object> request) {
        try {
            Long userId = getLongValue(request.get("userId"));
            String basePrompt = (String) request.get("basePrompt");
            
            String personalizedPrompt = integrationService.generatePersonalizedPrompt(userId, basePrompt);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("basePrompt", basePrompt);
            response.put("personalizedPrompt", personalizedPrompt);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("生成提示词失败", e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * 触发画像分析
     */
    @PostMapping("/trigger-analysis/{userId}")
    public ResponseEntity<Map<String, Object>> triggerAnalysis(@PathVariable Long userId) {
        try {
            integrationService.triggerProfileAnalysis(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "画像分析已触发，将在后台执行");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("触发画像分析失败 - userId: " + userId, e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }
    
    // 辅助方法
    
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
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
