package com.example.voicebox.app.device.service.voice;

import com.example.voicebox.app.device.dto.VoiceMonitoringReportDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 语音监控服务
 * 负责收集和生成语音系统的监控数据
 * 使用 VoiceMetricsService 收集的真实数据
 */
@Service
public class VoiceMonitoringService {

    @Autowired
    private VoiceMetricsService voiceMetricsService;

    /**
     * 生成完整的监控报告
     */
    public VoiceMonitoringReportDto generateMonitoringReport() {
        VoiceMonitoringReportDto report = new VoiceMonitoringReportDto();
        
        report.setOverall(generateOverallMetrics());
        report.setStt(generateSttMetrics());
        report.setTts(generateTtsMetrics());
        report.setUsers(generateUserMetrics());
        report.setLanguages(generateLanguageMetrics());
        report.setErrors(generateErrorMetrics());
        report.setPerformance(generatePerformanceMetrics());
        
        return report;
    }

    /**
     * 生成整体指标（使用真实数据）
     */
    private VoiceMonitoringReportDto.OverallMetrics generateOverallMetrics() {
        Map<String, Object> overallMetrics = voiceMetricsService.getOverallMetrics();
        Map<String, Object> performanceMetrics = voiceMetricsService.getPerformanceMetrics();
        
        double successRate = (Double) overallMetrics.getOrDefault("successRate", 0.0);
        double avgResponseTime = (Double) performanceMetrics.getOrDefault("averageResponseTime", 0.0);
        long totalRequests = ((Number) overallMetrics.getOrDefault("totalRequests", 0L)).longValue();
        double failureRate = (Double) overallMetrics.getOrDefault("failureRate", 0.0);
        long uptimeMinutes = ((Number) overallMetrics.getOrDefault("uptimeMinutes", 0L)).longValue();
        
        return new VoiceMonitoringReportDto.OverallMetrics(
            successRate, avgResponseTime, totalRequests, failureRate, uptimeMinutes
        );
    }

    /**
     * 生成 STT 指标（使用真实数据）
     */
    private VoiceMonitoringReportDto.SttMetrics generateSttMetrics() {
        Map<String, Object> sttMetrics = voiceMetricsService.getSttMetrics();
        
        double accuracy = (Double) sttMetrics.getOrDefault("successRate", 0.0);
        double avgProcessingTime = (Double) sttMetrics.getOrDefault("averageResponseTime", 0.0);
        long totalProcessed = ((Number) sttMetrics.getOrDefault("requests", 0L)).longValue();
        
        return new VoiceMonitoringReportDto.SttMetrics(
            accuracy, avgProcessingTime, totalProcessed
        );
    }

    /**
     * 生成 TTS 指标（使用真实数据）
     */
    private VoiceMonitoringReportDto.TtsMetrics generateTtsMetrics() {
        Map<String, Object> ttsMetrics = voiceMetricsService.getTtsMetrics();
        
        double quality = (Double) ttsMetrics.getOrDefault("successRate", 0.0);
        double avgGenerationTime = (Double) ttsMetrics.getOrDefault("averageResponseTime", 0.0);
        long totalGenerated = ((Number) ttsMetrics.getOrDefault("requests", 0L)).longValue();
        
        return new VoiceMonitoringReportDto.TtsMetrics(
            quality, avgGenerationTime, totalGenerated
        );
    }

    /**
     * 生成用户指标（使用真实数据）
     */
    private VoiceMonitoringReportDto.UserMetrics generateUserMetrics() {
        Map<String, Object> userMetrics = voiceMetricsService.getUserMetrics();
        
        long activeUsers = ((Number) userMetrics.getOrDefault("activeUsers", 0L)).longValue();
        // 新用户数据需要额外的跟踪逻辑，暂时设为0
        long newUsers = 0L;
        // 平均会话时长需要额外的跟踪逻辑，暂时设为0
        double avgSessionDuration = 0.0;
        
        return new VoiceMonitoringReportDto.UserMetrics(
            activeUsers, newUsers, avgSessionDuration
        );
    }

    /**
     * 生成语言指标（使用真实数据）
     */
    private VoiceMonitoringReportDto.LanguageMetrics generateLanguageMetrics() {
        Map<String, Integer> languageMetrics = voiceMetricsService.getLanguageMetrics();
        
        Map<String, Long> languageDistribution = new HashMap<>();
        String mostUsedLanguage = "zh-CN";
        int maxCount = 0;
        
        for (Map.Entry<String, Integer> entry : languageMetrics.entrySet()) {
            languageDistribution.put(entry.getKey(), entry.getValue().longValue());
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostUsedLanguage = entry.getKey();
            }
        }
        
        int supportedLanguages = languageDistribution.size();
        if (supportedLanguages == 0) {
            // 如果没有数据，返回默认值
            supportedLanguages = 4;
            mostUsedLanguage = "zh-CN";
        }
        
        return new VoiceMonitoringReportDto.LanguageMetrics(
            supportedLanguages, mostUsedLanguage, languageDistribution
        );
    }

    /**
     * 生成错误指标（使用真实数据）
     */
    private VoiceMonitoringReportDto.ErrorMetrics generateErrorMetrics() {
        Map<String, Integer> errorMetrics = voiceMetricsService.getErrorMetrics();
        Map<String, Object> overallMetrics = voiceMetricsService.getOverallMetrics();
        
        long totalErrors = 0;
        Map<String, Long> commonErrors = new HashMap<>();
        
        for (Map.Entry<String, Integer> entry : errorMetrics.entrySet()) {
            long count = entry.getValue().longValue();
            totalErrors += count;
            commonErrors.put(entry.getKey(), count);
        }
        
        double errorRate = (Double) overallMetrics.getOrDefault("failureRate", 0.0);
        
        return new VoiceMonitoringReportDto.ErrorMetrics(
            totalErrors, errorRate, commonErrors
        );
    }

    /**
     * 生成性能指标（使用真实数据）
     */
    private VoiceMonitoringReportDto.PerformanceMetrics generatePerformanceMetrics() {
        Map<String, Object> performanceMetrics = voiceMetricsService.getPerformanceMetrics();
        
        // 系统性能指标（CPU、内存等）需要额外的监控工具，暂时返回0
        double cpuUsage = 0.0;
        double memoryUsage = 0.0;
        double diskUsage = 0.0;
        
        // 网络延迟使用平均响应时间
        double networkLatency = (Double) performanceMetrics.getOrDefault("averageResponseTime", 0.0);
        
        return new VoiceMonitoringReportDto.PerformanceMetrics(
            cpuUsage, memoryUsage, diskUsage, networkLatency
        );
    }

    /**
     * 获取系统健康状态
     */
    public String getHealthStatus() {
        VoiceMonitoringReportDto.OverallMetrics overall = generateOverallMetrics();
        double successRate = overall.getSuccessRate();
        
        if (successRate > 95.0) {
            return "HEALTHY";
        } else if (successRate > 90.0) {
            return "WARNING";
        } else {
            return "CRITICAL";
        }
    }

    /**
     * 获取简化的监控数据（用于健康检查）
     */
    public Map<String, Object> getHealthSummary() {
        VoiceMonitoringReportDto.OverallMetrics overall = generateOverallMetrics();
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("status", getHealthStatus());
        summary.put("successRate", overall.getSuccessRate());
        summary.put("avgResponseTime", overall.getAvgResponseTime());
        summary.put("totalRequests", overall.getTotalRequests());
        summary.put("uptime", overall.getUptimeMinutes());
        summary.put("timestamp", LocalDateTime.now());
        
        return summary;
    }
}
