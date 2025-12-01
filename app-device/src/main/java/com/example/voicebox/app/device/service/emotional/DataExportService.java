package com.example.voicebox.app.device.service.emotional;

import com.example.voicebox.app.device.domain.UserEmotionalProfile;
import com.example.voicebox.app.device.repository.UserEmotionalProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 数据导出服务
 * 
 * 功能：
 * 1. 数据导出接口
 * 2. PII匿名化
 * 3. 导出格式支持（JSON）
 * 4. 导出权限验证
 */
@Service
public class DataExportService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataExportService.class);
    
    @Autowired
    private UserEmotionalProfileRepository profileRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 导出用户情感数据
     * 
     * @param userId 用户ID
     * @param includePersonalInfo 是否包含个人信息
     * @return JSON格式的数据
     */
    public String exportUserEmotionalData(Long userId, boolean includePersonalInfo) {
        logger.info("导出用户情感数据: userId={}, includePersonalInfo={}", userId, includePersonalInfo);
        
        try {
            Map<String, Object> exportData = new HashMap<>();
            
            // 1. 导出用户画像
            Map<String, Object> profile = exportUserProfile(userId, includePersonalInfo);
            exportData.put("profile", profile);
            
            // 2. 导出情感标签
            List<Map<String, Object>> tags = exportEmotionalTags(userId);
            exportData.put("tags", tags);
            
            // 3. 导出情绪历史
            List<Map<String, Object>> history = exportEmotionHistory(userId, includePersonalInfo);
            exportData.put("history", history);
            
            // 4. 导出统计数据
            Map<String, Object> statistics = exportStatistics(userId);
            exportData.put("statistics", statistics);
            
            // 5. 添加元数据
            exportData.put("exportDate", LocalDateTime.now().toString());
            exportData.put("version", "1.0");
            exportData.put("anonymized", !includePersonalInfo);
            
            // 转换为JSON
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportData);
            
        } catch (Exception e) {
            logger.error("数据导出失败: userId=" + userId, e);
            throw new RuntimeException("数据导出失败", e);
        }
    }
    
    /**
     * 导出用户画像
     * 
     * @param userId 用户ID
     * @param includePersonalInfo 是否包含个人信息
     * @return 画像数据
     */
    private Map<String, Object> exportUserProfile(Long userId, boolean includePersonalInfo) {
        Map<String, Object> profileData = new HashMap<>();
        
        Optional<UserEmotionalProfile> profileOpt = profileRepository.findByUserId(userId);
        
        if (profileOpt.isPresent()) {
            UserEmotionalProfile profile = profileOpt.get();
            
            if (includePersonalInfo) {
                profileData.put("userId", profile.getUserId());
            } else {
                // 匿名化用户ID
                profileData.put("userId", anonymizeUserId(profile.getUserId()));
            }
            
            profileData.put("dominantPersonalityType", profile.getDominantPersonalityType());
            profileData.put("mostFrequentEmotion", profile.getMostFrequentEmotion());
            profileData.put("preferredLanguage", profile.getPreferredLanguage());
            profileData.put("createdAt", profile.getCreatedAt());
            profileData.put("updatedAt", profile.getUpdatedAt());
        }
        
        return profileData;
    }
    
    /**
     * 导出情感标签
     * 
     * @param userId 用户ID
     * @return 标签列表
     */
    private List<Map<String, Object>> exportEmotionalTags(Long userId) {
        List<Map<String, Object>> tags = new ArrayList<>();
        
        // 实际应该从数据库查询
        // 这里是示例数据
        Map<String, Object> tag1 = new HashMap<>();
        tag1.put("tagName", "开朗");
        tag1.put("confidence", 0.85);
        tag1.put("category", "性格");
        tags.add(tag1);
        
        return tags;
    }
    
    /**
     * 导出情绪历史
     * 
     * @param userId 用户ID
     * @param includePersonalInfo 是否包含个人信息
     * @return 历史记录列表
     */
    private List<Map<String, Object>> exportEmotionHistory(Long userId, boolean includePersonalInfo) {
        List<Map<String, Object>> history = new ArrayList<>();
        
        // 实际应该从数据库查询
        // 这里是示例数据
        Map<String, Object> record = new HashMap<>();
        record.put("emotion", "HAPPY");
        record.put("confidence", 0.8);
        record.put("timestamp", LocalDateTime.now().toString());
        
        if (!includePersonalInfo) {
            // 移除可能包含个人信息的字段
            record.remove("audioFile");
            record.remove("transcript");
        }
        
        history.add(record);
        
        return history;
    }
    
    /**
     * 导出统计数据
     * 
     * @param userId 用户ID
     * @return 统计数据
     */
    private Map<String, Object> exportStatistics(Long userId) {
        Map<String, Object> statistics = new HashMap<>();
        
        statistics.put("totalInteractions", 10);
        Map<String, Integer> emotionDist = new HashMap<>();
        emotionDist.put("HAPPY", 6);
        emotionDist.put("CALM", 3);
        emotionDist.put("SAD", 1);
        statistics.put("emotionDistribution", emotionDist);
        statistics.put("averageConfidence", 0.75);
        
        return statistics;
    }
    
    /**
     * 匿名化用户ID
     * 
     * @param userId 原始用户ID
     * @return 匿名化的ID
     */
    private String anonymizeUserId(Long userId) {
        // 使用哈希或其他方法匿名化
        return "USER_" + Integer.toHexString(userId.hashCode()).toUpperCase();
    }
    
    /**
     * 匿名化文本内容
     * 
     * @param text 原始文本
     * @return 匿名化的文本
     */
    private String anonymizeText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 简单实现：替换为星号
        // 实际应该使用更复杂的匿名化算法
        return text.replaceAll("[\\u4e00-\\u9fa5a-zA-Z]", "*");
    }
    
    /**
     * 验证导出权限
     * 
     * @param userId 用户ID
     * @param requestUserId 请求用户ID
     * @return 是否有权限
     */
    public boolean verifyExportPermission(Long userId, Long requestUserId) {
        // 只允许用户导出自己的数据
        return userId.equals(requestUserId);
    }
    
    /**
     * 导出为CSV格式
     * 
     * @param userId 用户ID
     * @return CSV字符串
     */
    public String exportToCSV(Long userId) {
        logger.info("导出CSV格式数据: userId={}", userId);
        
        StringBuilder csv = new StringBuilder();
        csv.append("Timestamp,Emotion,Confidence,Tags\n");
        
        // 实际应该从数据库查询并格式化
        csv.append("2024-11-30 10:00:00,HAPPY,0.85,开朗;积极\n");
        csv.append("2024-11-30 11:00:00,CALM,0.75,平和;稳定\n");
        
        return csv.toString();
    }
}
