package com.example.voicebox.app.device.service.emotional;

import com.example.voicebox.app.device.domain.UserEmotionalProfile;
import com.example.voicebox.app.device.repository.UserEmotionalProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

/**
 * 用户语言偏好服务
 * 管理用户的语言选择和偏好设置
 */
@Service
public class UserLanguagePreferenceService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserLanguagePreferenceService.class);
    
    @Autowired
    private UserEmotionalProfileRepository profileRepository;
    
    @Autowired
    private MultiLanguageService multiLanguageService;
    
    /**
     * 获取用户的语言偏好
     */
    public String getUserLanguage(Long userId) {
        Optional<UserEmotionalProfile> profileOpt = profileRepository.findByUserId(userId);
        
        if (profileOpt.isPresent()) {
            String language = profileOpt.get().getPreferredLanguage();
            if (language != null && multiLanguageService.isLanguageSupported(language)) {
                return language;
            }
        }
        
        // 返回默认语言
        return multiLanguageService.getDefaultLanguage();
    }
    
    /**
     * 设置用户的语言偏好
     */
    public void setUserLanguage(Long userId, String language) {
        // 验证语言是否支持
        if (!multiLanguageService.isLanguageSupported(language)) {
            logger.warn("尝试设置不支持的语言: {} for user: {}", language, userId);
            throw new IllegalArgumentException("不支持的语言: " + language);
        }
        
        // 获取或创建用户画像
        UserEmotionalProfile profile = profileRepository.findByUserId(userId)
            .orElseGet(() -> {
                UserEmotionalProfile newProfile = new UserEmotionalProfile();
                newProfile.setUserId(userId);
                return newProfile;
            });
        
        // 更新语言偏好
        String oldLanguage = profile.getPreferredLanguage();
        profile.setPreferredLanguage(language);
        profileRepository.save(profile);
        
        logger.info("用户 {} 的语言偏好从 {} 更新为 {}", userId, oldLanguage, language);
    }
    
    /**
     * 切换用户语言（保持画像数据）
     * 验收标准 19.5: 切换语言时保持用户画像数据
     */
    public void switchUserLanguage(Long userId, String newLanguage) {
        // 验证语言
        if (!multiLanguageService.isLanguageSupported(newLanguage)) {
            throw new IllegalArgumentException("不支持的语言: " + newLanguage);
        }
        
        Optional<UserEmotionalProfile> profileOpt = profileRepository.findByUserId(userId);
        
        if (profileOpt.isPresent()) {
            UserEmotionalProfile profile = profileOpt.get();
            String oldLanguage = profile.getPreferredLanguage();
            
            // 更新语言偏好，但保持所有其他画像数据不变
            profile.setPreferredLanguage(newLanguage);
            profileRepository.save(profile);
            
            logger.info("用户 {} 切换语言: {} -> {}, 画像数据已保留", 
                userId, oldLanguage, newLanguage);
        } else {
            // 如果用户还没有画像，创建新的
            setUserLanguage(userId, newLanguage);
        }
    }
    
    /**
     * 获取用户语言的完整配置
     */
    public java.util.Map<String, Object> getUserLanguageConfig(Long userId) {
        String language = getUserLanguage(userId);
        return multiLanguageService.getLanguageConfig(language);
    }
}
