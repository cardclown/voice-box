package com.example.voicebox.app.device.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 个性分析系统配置
 * 
 * @author VoiceBox Team
 * @since 2.0
 */
@Configuration
@EnableAsync
@EnableScheduling
public class PersonalityConfig {
    
    /**
     * 配置异步任务执行器
     * 用于异步处理消息特征提取
     */
    @Bean(name = "personalityTaskExecutor")
    public Executor personalityTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(5);
        
        // 最大线程数
        executor.setMaxPoolSize(10);
        
        // 队列容量
        executor.setQueueCapacity(100);
        
        // 线程名称前缀
        executor.setThreadNamePrefix("personality-");
        
        // 拒绝策略：由调用线程处理
        executor.setRejectedExecutionHandler(
            new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }
}
