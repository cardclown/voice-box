package com.example.voicebox.app.device.service.voice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 语音监控和指标服务
 * 收集和统计语音功能的使用情况和性能指标
 */
@Service
public class VoiceMetricsService {
    
    private static final Logger logger = LoggerFactory.getLogger(VoiceMetricsService.class);
    
    // 计数器
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    
    // STT指标
    private final AtomicLong sttRequests = new AtomicLong(0);
    private final AtomicLong sttSuccesses = new AtomicLong(0);
    private final AtomicLong sttFailures = new AtomicLong(0);
    private final AtomicLong sttTotalDuration = new AtomicLong(0);
    
    // TTS指标
    private final AtomicLong ttsRequests = new AtomicLong(0);
    private final AtomicLong ttsSuccesses = new AtomicLong(0);
    private final AtomicLong ttsFailures = new AtomicLong(0);
    private final AtomicLong ttsTotalDuration = new AtomicLong(0);
    
    // 用户活跃度
    private final Map<Long, AtomicInteger> userRequestCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> languageUsage = new ConcurrentHashMap<>();
    
    // 错误统计
    private final Map<String, AtomicInteger> errorCounts = new ConcurrentHashMap<>();
    
    // 性能指标
    private final List<Long> recentResponseTimes = Collections.synchronizedList(new ArrayList<>());
    private final int MAX_RESPONSE_TIME_SAMPLES = 1000;
    
    // 系统启动时间
    private final LocalDateTime startTime = LocalDateTime.now();
    
    /**
     * 记录请求开始
     */
    public String recordRequestStart(String operation, Long userId, String language) {
        String requestId = generateRequestId();
        
        totalRequests.incrementAndGet();
        
        // 记录用户活跃度
        if (userId != null) {
            userRequestCounts.computeIfAbsent(userId, k -> new AtomicInteger(0)).incrementAndGet();
        }
        
        // 记录语言使用
        if (language != null) {
            languageUsage.computeIfAbsent(language, k -> new AtomicInteger(0)).incrementAndGet();
        }
        
        // 记录操作类型
        if ("stt".equalsIgnoreCase(operation)) {
            sttRequests.incrementAndGet();
        } else if ("tts".equalsIgnoreCase(operation)) {
            ttsRequests.incrementAndGet();
        }
        
        logger.debug("请求开始: requestId={}, operation={}, userId={}, language={}", 
                    requestId, operation, userId, language);
        
        return requestId;
    }
    
    /**
     * 记录请求成功
     */
    public void recordRequestSuccess(String requestId, String operation, long durationMs) {
        successfulRequests.incrementAndGet();
        
        // 记录响应时间
        recordResponseTime(durationMs);
        
        // 记录操作特定指标
        if ("stt".equalsIgnoreCase(operation)) {
            sttSuccesses.incrementAndGet();
            sttTotalDuration.addAndGet(durationMs);
        } else if ("tts".equalsIgnoreCase(operation)) {
            ttsSuccesses.incrementAndGet();
            ttsTotalDuration.addAndGet(durationMs);
        }
        
        logger.debug("请求成功: requestId={}, operation={}, duration={}ms", 
                    requestId, operation, durationMs);
    }
    
    /**
     * 记录请求失败
     */
    public void recordRequestFailure(String requestId, String operation, String errorType, String errorMessage) {
        failedRequests.incrementAndGet();
        
        // 记录错误统计
        errorCounts.computeIfAbsent(errorType, k -> new AtomicInteger(0)).incrementAndGet();
        
        // 记录操作特定失败
        if ("stt".equalsIgnoreCase(operation)) {
            sttFailures.incrementAndGet();
        } else if ("tts".equalsIgnoreCase(operation)) {
            ttsFailures.incrementAndGet();
        }
        
        logger.warn("请求失败: requestId={}, operation={}, errorType={}, message={}", 
                   requestId, operation, errorType, errorMessage);
    }
    
    /**
     * 记录响应时间
     */
    private void recordResponseTime(long durationMs) {
        synchronized (recentResponseTimes) {
            recentResponseTimes.add(durationMs);
            
            if (recentResponseTimes.size() > MAX_RESPONSE_TIME_SAMPLES) {
                recentResponseTimes.remove(0);
            }
        }
    }
    
    /**
     * 获取总体指标
     */
    public Map<String, Object> getOverallMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        long total = totalRequests.get();
        long successful = successfulRequests.get();
        long failed = failedRequests.get();
        
        metrics.put("totalRequests", total);
        metrics.put("successfulRequests", successful);
        metrics.put("failedRequests", failed);
        metrics.put("successRate", total > 0 ? (double) successful / total * 100 : 0.0);
        metrics.put("failureRate", total > 0 ? (double) failed / total * 100 : 0.0);
        
        long uptimeMinutes = ChronoUnit.MINUTES.between(startTime, LocalDateTime.now());
        metrics.put("uptimeMinutes", uptimeMinutes);
        metrics.put("startTime", startTime.toString());
        
        return metrics;
    }
    
    /**
     * 获取STT指标
     */
    public Map<String, Object> getSttMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        long requests = sttRequests.get();
        long successes = sttSuccesses.get();
        long failures = sttFailures.get();
        long totalDuration = sttTotalDuration.get();
        
        metrics.put("requests", requests);
        metrics.put("successes", successes);
        metrics.put("failures", failures);
        metrics.put("successRate", requests > 0 ? (double) successes / requests * 100 : 0.0);
        metrics.put("averageResponseTime", successes > 0 ? (double) totalDuration / successes : 0.0);
        
        return metrics;
    }
    
    /**
     * 获取TTS指标
     */
    public Map<String, Object> getTtsMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        long requests = ttsRequests.get();
        long successes = ttsSuccesses.get();
        long failures = ttsFailures.get();
        long totalDuration = ttsTotalDuration.get();
        
        metrics.put("requests", requests);
        metrics.put("successes", successes);
        metrics.put("failures", failures);
        metrics.put("successRate", requests > 0 ? (double) successes / requests * 100 : 0.0);
        metrics.put("averageResponseTime", successes > 0 ? (double) totalDuration / successes : 0.0);
        
        return metrics;
    }
    
    /**
     * 获取用户活跃度指标
     */
    public Map<String, Object> getUserMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        metrics.put("activeUsers", userRequestCounts.size());
        
        Map<String, Integer> userDistribution = new HashMap<>();
        int lightUsers = 0;
        int moderateUsers = 0;
        int heavyUsers = 0;
        
        for (AtomicInteger count : userRequestCounts.values()) {
            int requests = count.get();
            if (requests <= 10) {
                lightUsers++;
            } else if (requests <= 50) {
                moderateUsers++;
            } else {
                heavyUsers++;
            }
        }
        
        userDistribution.put("light", lightUsers);
        userDistribution.put("moderate", moderateUsers);
        userDistribution.put("heavy", heavyUsers);
        
        metrics.put("userDistribution", userDistribution);
        
        return metrics;
    }
    
    /**
     * 获取语言使用统计
     */
    public Map<String, Integer> getLanguageMetrics() {
        Map<String, Integer> metrics = new HashMap<>();
        
        for (Map.Entry<String, AtomicInteger> entry : languageUsage.entrySet()) {
            metrics.put(entry.getKey(), entry.getValue().get());
        }
        
        return metrics;
    }
    
    /**
     * 获取错误统计
     */
    public Map<String, Integer> getErrorMetrics() {
        Map<String, Integer> metrics = new HashMap<>();
        
        for (Map.Entry<String, AtomicInteger> entry : errorCounts.entrySet()) {
            metrics.put(entry.getKey(), entry.getValue().get());
        }
        
        return metrics;
    }
    
    /**
     * 获取性能指标
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        synchronized (recentResponseTimes) {
            if (recentResponseTimes.isEmpty()) {
                metrics.put("averageResponseTime", 0.0);
                metrics.put("minResponseTime", 0.0);
                metrics.put("maxResponseTime", 0.0);
                metrics.put("p95ResponseTime", 0.0);
                metrics.put("p99ResponseTime", 0.0);
            } else {
                List<Long> sortedTimes = new ArrayList<>(recentResponseTimes);
                Collections.sort(sortedTimes);
                
                double average = sortedTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
                long min = sortedTimes.get(0);
                long max = sortedTimes.get(sortedTimes.size() - 1);
                
                int p95Index = (int) (sortedTimes.size() * 0.95);
                int p99Index = (int) (sortedTimes.size() * 0.99);
                
                long p95 = sortedTimes.get(Math.min(p95Index, sortedTimes.size() - 1));
                long p99 = sortedTimes.get(Math.min(p99Index, sortedTimes.size() - 1));
                
                metrics.put("averageResponseTime", average);
                metrics.put("minResponseTime", (double) min);
                metrics.put("maxResponseTime", (double) max);
                metrics.put("p95ResponseTime", (double) p95);
                metrics.put("p99ResponseTime", (double) p99);
                metrics.put("sampleCount", sortedTimes.size());
            }
        }
        
        return metrics;
    }
    
    /**
     * 获取完整的监控报告
     */
    public Map<String, Object> getFullReport() {
        Map<String, Object> report = new HashMap<>();
        
        report.put("timestamp", LocalDateTime.now().toString());
        report.put("overall", getOverallMetrics());
        report.put("stt", getSttMetrics());
        report.put("tts", getTtsMetrics());
        report.put("users", getUserMetrics());
        report.put("languages", getLanguageMetrics());
        report.put("errors", getErrorMetrics());
        report.put("performance", getPerformanceMetrics());
        
        return report;
    }
    
    /**
     * 重置所有指标
     */
    public void resetMetrics() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        
        sttRequests.set(0);
        sttSuccesses.set(0);
        sttFailures.set(0);
        sttTotalDuration.set(0);
        
        ttsRequests.set(0);
        ttsSuccesses.set(0);
        ttsFailures.set(0);
        ttsTotalDuration.set(0);
        
        userRequestCounts.clear();
        languageUsage.clear();
        errorCounts.clear();
        
        synchronized (recentResponseTimes) {
            recentResponseTimes.clear();
        }
        
        logger.info("所有监控指标已重置");
    }
    
    /**
     * 生成请求ID
     */
    private String generateRequestId() {
        return "req_" + System.currentTimeMillis() + "_" + 
               Integer.toHexString(new Random().nextInt());
    }
    
    /**
     * 记录自定义指标
     */
    public void recordCustomMetric(String metricName, double value) {
        logger.info("自定义指标: {}={}", metricName, value);
    }
    
    /**
     * 检查系统健康状态
     */
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();
        
        long total = totalRequests.get();
        long failed = failedRequests.get();
        
        double failureRate = total > 0 ? (double) failed / total * 100 : 0.0;
        
        String status;
        if (failureRate < 1.0) {
            status = "HEALTHY";
        } else if (failureRate < 5.0) {
            status = "WARNING";
        } else {
            status = "CRITICAL";
        }
        
        health.put("status", status);
        health.put("failureRate", failureRate);
        health.put("totalRequests", total);
        health.put("uptime", ChronoUnit.MINUTES.between(startTime, LocalDateTime.now()));
        
        return health;
    }
}
