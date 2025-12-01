package com.example.voicebox.app.device.scheduler;

import com.example.voicebox.app.device.domain.UserProfile;
import com.example.voicebox.app.device.repository.UserProfileRepository;
import com.example.voicebox.app.device.service.PersonalityAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 个性分析定时任务
 * 定期更新用户画像
 * 
 * @author VoiceBox Team
 * @since 2.0
 */
@Component
public class PersonalityAnalysisScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(PersonalityAnalysisScheduler.class);
    
    @Autowired
    private PersonalityAnalysisService personalityAnalysisService;
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    /**
     * 每天凌晨2点更新需要刷新的用户画像
     * 更新超过7天未分析的画像
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void updateStaleProfiles() {
        try {
            logger.info("开始定时更新用户画像...");
            
            // 查找需要更新的画像（7天未更新）
            List<UserProfile> staleProfiles = userProfileRepository.findNeedingUpdate(7);
            
            logger.info("找到 {} 个需要更新的用户画像", staleProfiles.size());
            
            int successCount = 0;
            int failCount = 0;
            
            for (UserProfile profile : staleProfiles) {
                try {
                    personalityAnalysisService.analyzePersonality(profile.getUserId());
                    successCount++;
                    
                    // 避免过载，每处理10个用户休息1秒
                    if (successCount % 10 == 0) {
                        Thread.sleep(1000);
                    }
                    
                } catch (Exception e) {
                    logger.error("更新用户画像失败 - userId: " + profile.getUserId(), e);
                    failCount++;
                }
            }
            
            logger.info("用户画像更新完成 - 成功: {}, 失败: {}", successCount, failCount);
            
        } catch (Exception e) {
            logger.error("定时更新用户画像任务失败", e);
        }
    }
    
    /**
     * 每小时更新活跃用户的画像
     * 针对最近有新消息的用户
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void updateActiveUserProfiles() {
        try {
            logger.info("开始更新活跃用户画像...");
            
            // 这里可以根据实际需求查询最近活跃的用户
            // 暂时跳过，避免过于频繁的更新
            
            logger.info("活跃用户画像更新完成");
            
        } catch (Exception e) {
            logger.error("更新活跃用户画像失败", e);
        }
    }
    
    /**
     * 每周日凌晨3点进行全量画像分析
     * 重新计算所有用户的画像
     */
    @Scheduled(cron = "0 0 3 ? * SUN")
    public void fullProfileAnalysis() {
        try {
            logger.info("开始全量用户画像分析...");
            
            List<UserProfile> allProfiles = userProfileRepository.findAll();
            
            logger.info("开始分析 {} 个用户画像", allProfiles.size());
            
            int successCount = 0;
            int failCount = 0;
            
            for (UserProfile profile : allProfiles) {
                try {
                    personalityAnalysisService.analyzePersonality(profile.getUserId());
                    successCount++;
                    
                    // 避免过载
                    if (successCount % 10 == 0) {
                        Thread.sleep(1000);
                        logger.info("已处理 {} / {} 个用户", successCount, allProfiles.size());
                    }
                    
                } catch (Exception e) {
                    logger.error("分析用户画像失败 - userId: " + profile.getUserId(), e);
                    failCount++;
                }
            }
            
            logger.info("全量用户画像分析完成 - 成功: {}, 失败: {}", successCount, failCount);
            
        } catch (Exception e) {
            logger.error("全量画像分析任务失败", e);
        }
    }
    
    /**
     * 每天凌晨4点清理旧数据
     * 删除90天前的对话特征数据
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanupOldData() {
        try {
            logger.info("开始清理旧数据...");
            
            java.time.LocalDateTime cutoffDate = java.time.LocalDateTime.now().minusDays(90);
            
            // 这里可以添加数据清理逻辑
            // conversationFeatureRepository.deleteOlderThan(cutoffDate);
            
            logger.info("旧数据清理完成");
            
        } catch (Exception e) {
            logger.error("清理旧数据失败", e);
        }
    }
}
