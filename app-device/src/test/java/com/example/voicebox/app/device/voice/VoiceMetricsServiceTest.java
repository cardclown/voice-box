package com.example.voicebox.app.device.voice;

import com.example.voicebox.app.device.service.voice.VoiceMetricsService;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * VoiceMetricsService单元测试
 * 测试监控指标的记录和统计功能
 */
public class VoiceMetricsServiceTest {
    
    private VoiceMetricsService metricsService;
    
    @Before
    public void setUp() {
        metricsService = new VoiceMetricsService();
    }
    
    @Test
    public void testRequestCounting_STT() {
        // Given: 初始状态
        Map<String, Object> initialMetrics = metricsService.getOverallMetrics();
        assertEquals(0L, initialMetrics.get("totalRequests"));
        
        // When: 记录3个STT请求
        String requestId1 = metricsService.recordRequestStart("stt", 1L, "zh-CN");
        metricsService.recordRequestSuccess(requestId1, "stt", 100L);
        
        String requestId2 = metricsService.recordRequestStart("stt", 1L, "zh-CN");
        metricsService.recordRequestSuccess(requestId2, "stt", 150L);
        
        String requestId3 = metricsService.recordRequestStart("stt", 2L, "en-US");
        metricsService.recordRequestSuccess(requestId3, "stt", 200L);
        
        // Then: 验证计数
        Map<String, Object> overallMetrics = metricsService.getOverallMetrics();
        assertEquals(3L, overallMetrics.get("totalRequests"));
        assertEquals(3L, overallMetrics.get("successfulRequests"));
        assertEquals(0L, overallMetrics.get("failedRequests"));
        
        Map<String, Object> sttMetrics = metricsService.getSttMetrics();
        assertEquals(3L, sttMetrics.get("requests"));
        assertEquals(3L, sttMetrics.get("successes"));
        assertEquals(0L, sttMetrics.get("failures"));
    }
    
    @Test
    public void testRequestCounting_TTS() {
        // When: 记录2个TTS请求
        String requestId1 = metricsService.recordRequestStart("tts", 1L, "zh-CN");
        metricsService.recordRequestSuccess(requestId1, "tts", 300L);
        
        String requestId2 = metricsService.recordRequestStart("tts", 1L, "zh-CN");
        metricsService.recordRequestSuccess(requestId2, "tts", 250L);
        
        // Then: 验证计数
        Map<String, Object> overallMetrics = metricsService.getOverallMetrics();
        assertEquals(2L, overallMetrics.get("totalRequests"));
        
        Map<String, Object> ttsMetrics = metricsService.getTtsMetrics();
        assertEquals(2L, ttsMetrics.get("requests"));
        assertEquals(2L, ttsMetrics.get("successes"));
    }
    
    @Test
    public void testFailureCounting() {
        // When: 记录成功和失败的请求
        String requestId1 = metricsService.recordRequestStart("stt", 1L, "zh-CN");
        metricsService.recordRequestSuccess(requestId1, "stt", 100L);
        
        String requestId2 = metricsService.recordRequestStart("stt", 1L, "zh-CN");
        metricsService.recordRequestFailure(requestId2, "stt", "TIMEOUT", "网络超时");
        
        String requestId3 = metricsService.recordRequestStart("tts", 1L, "zh-CN");
        metricsService.recordRequestFailure(requestId3, "tts", "SERVICE_ERROR", "服务错误");
        
        // Then: 验证失败计数
        Map<String, Object> overallMetrics = metricsService.getOverallMetrics();
        assertEquals(3L, overallMetrics.get("totalRequests"));
        assertEquals(1L, overallMetrics.get("successfulRequests"));
        assertEquals(2L, overallMetrics.get("failedRequests"));
        
        // 验证失败率
        double failureRate = (Double) overallMetrics.get("failureRate");
        assertEquals(66.67, failureRate, 0.01);
        
        // 验证错误类型统计
        Map<String, Integer> errorMetrics = metricsService.getErrorMetrics();
        assertEquals(1, errorMetrics.get("TIMEOUT").intValue());
        assertEquals(1, errorMetrics.get("SERVICE_ERROR").intValue());
    }
    
    @Test
    public void testResponseTimeRecording() {
        // When: 记录多个请求的响应时间
        String requestId1 = metricsService.recordRequestStart("stt", 1L, "zh-CN");
        metricsService.recordRequestSuccess(requestId1, "stt", 100L);
        
        String requestId2 = metricsService.recordRequestStart("stt", 1L, "zh-CN");
        metricsService.recordRequestSuccess(requestId2, "stt", 200L);
        
        String requestId3 = metricsService.recordRequestStart("stt", 1L, "zh-CN");
        metricsService.recordRequestSuccess(requestId3, "stt", 300L);
        
        // Then: 验证平均响应时间
        Map<String, Object> sttMetrics = metricsService.getSttMetrics();
        double avgResponseTime = (Double) sttMetrics.get("averageResponseTime");
        assertEquals(200.0, avgResponseTime, 0.01); // (100+200+300)/3 = 200
        
        // 验证性能指标
        Map<String, Object> perfMetrics = metricsService.getPerformanceMetrics();
        assertEquals(200.0, (Double) perfMetrics.get("averageResponseTime"), 0.01);
        assertEquals(100.0, (Double) perfMetrics.get("minResponseTime"), 0.01);
        assertEquals(300.0, (Double) perfMetrics.get("maxResponseTime"), 0.01);
    }
    
    @Test
    public void testErrorRateStatistics() {
        // When: 记录10个请求，其中2个失败
        for (int i = 0; i < 8; i++) {
            String requestId = metricsService.recordRequestStart("stt", 1L, "zh-CN");
            metricsService.recordRequestSuccess(requestId, "stt", 100L);
        }
        
        for (int i = 0; i < 2; i++) {
            String requestId = metricsService.recordRequestStart("stt", 1L, "zh-CN");
            metricsService.recordRequestFailure(requestId, "stt", "ERROR", "测试错误");
        }
        
        // Then: 验证错误率
        Map<String, Object> overallMetrics = metricsService.getOverallMetrics();
        assertEquals(10L, overallMetrics.get("totalRequests"));
        assertEquals(8L, overallMetrics.get("successfulRequests"));
        assertEquals(2L, overallMetrics.get("failedRequests"));
        
        double successRate = (Double) overallMetrics.get("successRate");
        double failureRate = (Double) overallMetrics.get("failureRate");
        
        assertEquals(80.0, successRate, 0.01);
        assertEquals(20.0, failureRate, 0.01);
    }
    
    @Test
    public void testUserActivityStatistics() {
        // When: 不同用户发起请求
        String requestId1 = metricsService.recordRequestStart("stt", 1L, "zh-CN");
        metricsService.recordRequestSuccess(requestId1, "stt", 100L);
        
        String requestId2 = metricsService.recordRequestStart("stt", 2L, "zh-CN");
        metricsService.recordRequestSuccess(requestId2, "stt", 100L);
        
        String requestId3 = metricsService.recordRequestStart("stt", 1L, "zh-CN");
        metricsService.recordRequestSuccess(requestId3, "stt", 100L);
        
        String requestId4 = metricsService.recordRequestStart("stt", 3L, "en-US");
        metricsService.recordRequestSuccess(requestId4, "stt", 100L);
        
        // Then: 验证活跃用户数
        Map<String, Object> userMetrics = metricsService.getUserMetrics();
        assertEquals(3, userMetrics.get("activeUsers")); // 用户1, 2, 3
    }
    
    @Test
    public void testUserDistribution() {
        // When: 创建不同活跃度的用户
        // 轻度用户（1-10次）
        for (int i = 0; i < 5; i++) {
            String requestId = metricsService.recordRequestStart("stt", 1L, "zh-CN");
            metricsService.recordRequestSuccess(requestId, "stt", 100L);
        }
        
        // 中度用户（11-50次）
        for (int i = 0; i < 20; i++) {
            String requestId = metricsService.recordRequestStart("stt", 2L, "zh-CN");
            metricsService.recordRequestSuccess(requestId, "stt", 100L);
        }
        
        // 重度用户（50+次）
        for (int i = 0; i < 60; i++) {
            String requestId = metricsService.recordRequestStart("stt", 3L, "zh-CN");
            metricsService.recordRequestSuccess(requestId, "stt", 100L);
        }
        
        // Then: 验证用户分布
        Map<String, Object> userMetrics = metricsService.getUserMetrics();
        @SuppressWarnings("unchecked")
        Map<String, Integer> distribution = (Map<String, Integer>) userMetrics.get("userDistribution");
        
        assertEquals(1, distribution.get("light").intValue());
        assertEquals(1, distribution.get("moderate").intValue());
        assertEquals(1, distribution.get("heavy").intValue());
    }
    
    @Test
    public void testLanguageUsageStatistics() {
        // When: 使用不同语言发起请求
        String requestId1 = metricsService.recordRequestStart("stt", 1L, "zh-CN");
        metricsService.recordRequestSuccess(requestId1, "stt", 100L);
        
        String requestId2 = metricsService.recordRequestStart("stt", 1L, "zh-CN");
        metricsService.recordRequestSuccess(requestId2, "stt", 100L);
        
        String requestId3 = metricsService.recordRequestStart("stt", 1L, "en-US");
        metricsService.recordRequestSuccess(requestId3, "stt", 100L);
        
        String requestId4 = metricsService.recordRequestStart("stt", 1L, "ja-JP");
        metricsService.recordRequestSuccess(requestId4, "stt", 100L);
        
        // Then: 验证语言统计
        Map<String, Integer> languageMetrics = metricsService.getLanguageMetrics();
        assertEquals(2, languageMetrics.get("zh-CN").intValue());
        assertEquals(1, languageMetrics.get("en-US").intValue());
        assertEquals(1, languageMetrics.get("ja-JP").intValue());
    }
    
    @Test
    public void testPerformancePercentiles() {
        // When: 记录100个请求，响应时间从1ms到100ms
        for (int i = 1; i <= 100; i++) {
            String requestId = metricsService.recordRequestStart("stt", 1L, "zh-CN");
            metricsService.recordRequestSuccess(requestId, "stt", i);
        }
        
        // Then: 验证百分位数
        Map<String, Object> perfMetrics = metricsService.getPerformanceMetrics();
        
        double p95 = (Double) perfMetrics.get("p95ResponseTime");
        double p99 = (Double) perfMetrics.get("p99ResponseTime");
        
        // P95应该接近95ms
        assertTrue("P95应该在94-96之间，实际: " + p95, p95 >= 94 && p95 <= 96);
        
        // P99应该接近99ms
        assertTrue("P99应该在98-100之间，实际: " + p99, p99 >= 98 && p99 <= 100);
    }
    
    @Test
    public void testHealthStatus() {
        // Test 1: 健康状态（失败率 < 1%）
        for (int i = 0; i < 100; i++) {
            String requestId = metricsService.recordRequestStart("stt", 1L, "zh-CN");
            metricsService.recordRequestSuccess(requestId, "stt", 100L);
        }
        
        Map<String, Object> health = metricsService.getHealthStatus();
        assertEquals("HEALTHY", health.get("status"));
        
        // Test 2: 警告状态（1% <= 失败率 < 5%）
        metricsService.resetMetrics();
        for (int i = 0; i < 97; i++) {
            String requestId = metricsService.recordRequestStart("stt", 1L, "zh-CN");
            metricsService.recordRequestSuccess(requestId, "stt", 100L);
        }
        for (int i = 0; i < 3; i++) {
            String requestId = metricsService.recordRequestStart("stt", 1L, "zh-CN");
            metricsService.recordRequestFailure(requestId, "stt", "ERROR", "测试");
        }
        
        health = metricsService.getHealthStatus();
        assertEquals("WARNING", health.get("status"));
        
        // Test 3: 严重状态（失败率 >= 5%）
        metricsService.resetMetrics();
        for (int i = 0; i < 90; i++) {
            String requestId = metricsService.recordRequestStart("stt", 1L, "zh-CN");
            metricsService.recordRequestSuccess(requestId, "stt", 100L);
        }
        for (int i = 0; i < 10; i++) {
            String requestId = metricsService.recordRequestStart("stt", 1L, "zh-CN");
            metricsService.recordRequestFailure(requestId, "stt", "ERROR", "测试");
        }
        
        health = metricsService.getHealthStatus();
        assertEquals("CRITICAL", health.get("status"));
    }
    
    @Test
    public void testMetricsReset() {
        // Given: 记录一些数据
        String requestId1 = metricsService.recordRequestStart("stt", 1L, "zh-CN");
        metricsService.recordRequestSuccess(requestId1, "stt", 100L);
        
        String requestId2 = metricsService.recordRequestStart("tts", 1L, "zh-CN");
        metricsService.recordRequestSuccess(requestId2, "tts", 200L);
        
        // When: 重置指标
        metricsService.resetMetrics();
        
        // Then: 验证所有指标归零
        Map<String, Object> overallMetrics = metricsService.getOverallMetrics();
        assertEquals(0L, overallMetrics.get("totalRequests"));
        assertEquals(0L, overallMetrics.get("successfulRequests"));
        assertEquals(0L, overallMetrics.get("failedRequests"));
        
        Map<String, Object> sttMetrics = metricsService.getSttMetrics();
        assertEquals(0L, sttMetrics.get("requests"));
        
        Map<String, Object> ttsMetrics = metricsService.getTtsMetrics();
        assertEquals(0L, ttsMetrics.get("requests"));
        
        Map<String, Object> userMetrics = metricsService.getUserMetrics();
        assertEquals(0, userMetrics.get("activeUsers"));
        
        Map<String, Integer> languageMetrics = metricsService.getLanguageMetrics();
        assertTrue(languageMetrics.isEmpty());
        
        Map<String, Integer> errorMetrics = metricsService.getErrorMetrics();
        assertTrue(errorMetrics.isEmpty());
    }
    
    @Test
    public void testFullReport() {
        // Given: 记录一些数据
        String requestId1 = metricsService.recordRequestStart("stt", 1L, "zh-CN");
        metricsService.recordRequestSuccess(requestId1, "stt", 100L);
        
        String requestId2 = metricsService.recordRequestStart("tts", 2L, "en-US");
        metricsService.recordRequestSuccess(requestId2, "tts", 200L);
        
        String requestId3 = metricsService.recordRequestStart("stt", 1L, "zh-CN");
        metricsService.recordRequestFailure(requestId3, "stt", "TIMEOUT", "超时");
        
        // When: 获取完整报告
        Map<String, Object> report = metricsService.getFullReport();
        
        // Then: 验证报告包含所有部分
        assertNotNull(report.get("timestamp"));
        assertNotNull(report.get("overall"));
        assertNotNull(report.get("stt"));
        assertNotNull(report.get("tts"));
        assertNotNull(report.get("users"));
        assertNotNull(report.get("languages"));
        assertNotNull(report.get("errors"));
        assertNotNull(report.get("performance"));
        
        // 验证数据正确性
        @SuppressWarnings("unchecked")
        Map<String, Object> overall = (Map<String, Object>) report.get("overall");
        assertEquals(3L, overall.get("totalRequests"));
    }
    
    @Test
    public void testConcurrentSafety() throws InterruptedException {
        // When: 多线程同时记录指标
        int threadCount = 10;
        int requestsPerThread = 100;
        
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    String requestId = metricsService.recordRequestStart("stt", (long) threadId, "zh-CN");
                    metricsService.recordRequestSuccess(requestId, "stt", 100L);
                }
            });
            threads[i].start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Then: 验证总数正确
        Map<String, Object> overallMetrics = metricsService.getOverallMetrics();
        assertEquals((long) threadCount * requestsPerThread, overallMetrics.get("totalRequests"));
        assertEquals((long) threadCount * requestsPerThread, overallMetrics.get("successfulRequests"));
    }
}
