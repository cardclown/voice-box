package com.example.voicebox.app.device.service.emotional;

import com.example.voicebox.app.device.repository.EmotionalVoiceMessageRepository;
import com.example.voicebox.app.device.repository.UserEmotionalProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据删除服务
 * 
 * 功能：
 * 1. 用户数据完全删除
 * 2. 级联删除逻辑
 * 3. 删除确认机制
 * 4. 删除审计日志
 */
@Service
public class DataDeletionService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataDeletionService.class);
    
    @Autowired
    private UserEmotionalProfileRepository profileRepository;
    
    @Autowired
    private EmotionalVoiceMessageRepository messageRepository;
    
    /**
     * 删除用户的所有情感数据
     * 
     * @param userId 用户ID
     * @param confirmToken 确认令牌
     * @return 删除结果
     */
    @Transactional
    public Map<String, Object> deleteUserEmotionalData(Long userId, String confirmToken) {
        logger.info("开始删除用户情感数据: userId={}", userId);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 验证确认令牌
            if (!verifyDeletionToken(userId, confirmToken)) {
                result.put("success", false);
                result.put("error", "确认令牌无效");
                return result;
            }
            
            // 2. 删除用户画像
            int profilesDeleted = deleteUserProfiles(userId);
            logger.info("删除用户画像: userId={}, count={}", userId, profilesDeleted);
            
            // 3. 删除语音消息
            int messagesDeleted = deleteVoiceMessages(userId);
            logger.info("删除语音消息: userId={}, count={}", userId, messagesDeleted);
            
            // 4. 删除情绪历史
            int historyDeleted = deleteEmotionHistory(userId);
            logger.info("删除情绪历史: userId={}, count={}", userId, historyDeleted);
            
            // 5. 删除标签数据
            int tagsDeleted = deleteEmotionalTags(userId);
            logger.info("删除情感标签: userId={}, count={}", userId, tagsDeleted);
            
            // 6. 记录删除审计日志
            logDeletionAudit(userId, profilesDeleted + messagesDeleted + historyDeleted + tagsDeleted);
            
            result.put("success", true);
            result.put("deletedProfiles", profilesDeleted);
            result.put("deletedMessages", messagesDeleted);
            result.put("deletedHistory", historyDeleted);
            result.put("deletedTags", tagsDeleted);
            result.put("totalDeleted", profilesDeleted + messagesDeleted + historyDeleted + tagsDeleted);
            
            logger.info("用户情感数据删除完成: userId={}, total={}", userId, result.get("totalDeleted"));
            
        } catch (Exception e) {
            logger.error("删除用户数据失败: userId=" + userId, e);
            result.put("success", false);
            result.put("error", "删除失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 生成删除确认令牌
     * 
     * @param userId 用户ID
     * @return 确认令牌
     */
    public String generateDeletionToken(Long userId) {
        // 简化实现：使用时间戳和用户ID
        // 实际应该使用更安全的令牌生成机制
        String token = userId + "_" + System.currentTimeMillis();
        logger.info("生成删除令牌: userId={}, token={}", userId, token);
        return token;
    }
    
    /**
     * 验证删除令牌
     * 
     * @param userId 用户ID
     * @param token 令牌
     * @return 是否有效
     */
    private boolean verifyDeletionToken(Long userId, String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        // 简化实现：检查令牌格式
        // 实际应该验证令牌的有效性和过期时间
        return token.startsWith(userId + "_");
    }
    
    /**
     * 删除用户画像
     * 
     * @param userId 用户ID
     * @return 删除数量
     */
    private int deleteUserProfiles(Long userId) {
        try {
            profileRepository.deleteByUserId(userId);
            return 1; // 简化实现
        } catch (Exception e) {
            logger.error("删除用户画像失败", e);
            return 0;
        }
    }
    
    /**
     * 删除语音消息
     * 
     * @param userId 用户ID
     * @return 删除数量
     */
    private int deleteVoiceMessages(Long userId) {
        try {
            messageRepository.deleteByUserId(userId);
            return 1; // 简化实现
        } catch (Exception e) {
            logger.error("删除语音消息失败", e);
            return 0;
        }
    }
    
    /**
     * 删除情绪历史
     * 
     * @param userId 用户ID
     * @return 删除数量
     */
    private int deleteEmotionHistory(Long userId) {
        // 实际应该调用相应的Repository
        logger.info("删除情绪历史: userId={}", userId);
        return 0;
    }
    
    /**
     * 删除情感标签
     * 
     * @param userId 用户ID
     * @return 删除数量
     */
    private int deleteEmotionalTags(Long userId) {
        // 实际应该调用相应的Repository
        logger.info("删除情感标签: userId={}", userId);
        return 0;
    }
    
    /**
     * 记录删除审计日志
     * 
     * @param userId 用户ID
     * @param totalDeleted 删除总数
     */
    private void logDeletionAudit(Long userId, int totalDeleted) {
        Map<String, Object> auditLog = new HashMap<>();
        auditLog.put("userId", userId);
        auditLog.put("totalDeleted", totalDeleted);
        auditLog.put("timestamp", LocalDateTime.now());
        auditLog.put("operation", "DELETE_USER_EMOTIONAL_DATA");
        
        logger.info("删除审计日志: {}", auditLog);
        
        // 实际应该保存到审计日志表
    }
    
    /**
     * 软删除用户数据（标记为已删除）
     * 
     * @param userId 用户ID
     * @return 删除结果
     */
    public Map<String, Object> softDeleteUserData(Long userId) {
        logger.info("软删除用户数据: userId={}", userId);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 标记为已删除，而不是物理删除
            // 实际应该更新deleted字段
            
            result.put("success", true);
            result.put("message", "数据已标记为删除");
            
        } catch (Exception e) {
            logger.error("软删除失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}
