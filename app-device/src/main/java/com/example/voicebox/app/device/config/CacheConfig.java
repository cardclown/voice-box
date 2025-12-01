package com.example.voicebox.app.device.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 缓存配置
 * 
 * 启用Spring缓存和异步支持
 */
@Configuration
@EnableCaching
@EnableAsync
public class CacheConfig {
    
    /**
     * 配置缓存管理器
     * 
     * @return CacheManager
     */
    @Bean
    public CacheManager cacheManager() {
        // 使用简单的内存缓存
        // 生产环境可以使用Redis等分布式缓存
        return new ConcurrentMapCacheManager(
            "userProfiles",      // 用户画像缓存
            "emotionAnalysis",   // 情绪分析结果缓存
            "voiceFeatures"      // 语音特征缓存
        );
    }
}
