package com.example.voicebox.app.device.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 语音监控报告数据传输对象
 */
public class VoiceMonitoringReportDto {

    /**
     * 整体指标
     */
    public static class OverallMetrics {
        @JsonProperty("successRate")
        private Double successRate;
        
        @JsonProperty("avgResponseTime")
        private Double avgResponseTime;
        
        @JsonProperty("totalRequests")
        private Long totalRequests;
        
        @JsonProperty("failureRate")
        private Double failureRate;
        
        @JsonProperty("uptimeMinutes")
        private Long uptimeMinutes;

        public OverallMetrics() {}

        public OverallMetrics(Double successRate, Double avgResponseTime, Long totalRequests, 
                            Double failureRate, Long uptimeMinutes) {
            this.successRate = successRate;
            this.avgResponseTime = avgResponseTime;
            this.totalRequests = totalRequests;
            this.failureRate = failureRate;
            this.uptimeMinutes = uptimeMinutes;
        }

        public Double getSuccessRate() { return successRate; }
        public void setSuccessRate(Double successRate) { this.successRate = successRate; }
        
        public Double getAvgResponseTime() { return avgResponseTime; }
        public void setAvgResponseTime(Double avgResponseTime) { this.avgResponseTime = avgResponseTime; }
        
        public Long getTotalRequests() { return totalRequests; }
        public void setTotalRequests(Long totalRequests) { this.totalRequests = totalRequests; }
        
        public Double getFailureRate() { return failureRate; }
        public void setFailureRate(Double failureRate) { this.failureRate = failureRate; }
        
        public Long getUptimeMinutes() { return uptimeMinutes; }
        public void setUptimeMinutes(Long uptimeMinutes) { this.uptimeMinutes = uptimeMinutes; }
    }

    /**
     * STT 指标
     */
    public static class SttMetrics {
        @JsonProperty("accuracy")
        private Double accuracy;
        
        @JsonProperty("avgProcessingTime")
        private Double avgProcessingTime;
        
        @JsonProperty("totalProcessed")
        private Long totalProcessed;

        public SttMetrics() {}

        public SttMetrics(Double accuracy, Double avgProcessingTime, Long totalProcessed) {
            this.accuracy = accuracy;
            this.avgProcessingTime = avgProcessingTime;
            this.totalProcessed = totalProcessed;
        }

        public Double getAccuracy() { return accuracy; }
        public void setAccuracy(Double accuracy) { this.accuracy = accuracy; }
        
        public Double getAvgProcessingTime() { return avgProcessingTime; }
        public void setAvgProcessingTime(Double avgProcessingTime) { this.avgProcessingTime = avgProcessingTime; }
        
        public Long getTotalProcessed() { return totalProcessed; }
        public void setTotalProcessed(Long totalProcessed) { this.totalProcessed = totalProcessed; }
    }

    /**
     * TTS 指标
     */
    public static class TtsMetrics {
        @JsonProperty("quality")
        private Double quality;
        
        @JsonProperty("avgGenerationTime")
        private Double avgGenerationTime;
        
        @JsonProperty("totalGenerated")
        private Long totalGenerated;

        public TtsMetrics() {}

        public TtsMetrics(Double quality, Double avgGenerationTime, Long totalGenerated) {
            this.quality = quality;
            this.avgGenerationTime = avgGenerationTime;
            this.totalGenerated = totalGenerated;
        }

        public Double getQuality() { return quality; }
        public void setQuality(Double quality) { this.quality = quality; }
        
        public Double getAvgGenerationTime() { return avgGenerationTime; }
        public void setAvgGenerationTime(Double avgGenerationTime) { this.avgGenerationTime = avgGenerationTime; }
        
        public Long getTotalGenerated() { return totalGenerated; }
        public void setTotalGenerated(Long totalGenerated) { this.totalGenerated = totalGenerated; }
    }

    /**
     * 用户指标
     */
    public static class UserMetrics {
        @JsonProperty("activeUsers")
        private Long activeUsers;
        
        @JsonProperty("newUsers")
        private Long newUsers;
        
        @JsonProperty("avgSessionDuration")
        private Double avgSessionDuration;

        public UserMetrics() {}

        public UserMetrics(Long activeUsers, Long newUsers, Double avgSessionDuration) {
            this.activeUsers = activeUsers;
            this.newUsers = newUsers;
            this.avgSessionDuration = avgSessionDuration;
        }

        public Long getActiveUsers() { return activeUsers; }
        public void setActiveUsers(Long activeUsers) { this.activeUsers = activeUsers; }
        
        public Long getNewUsers() { return newUsers; }
        public void setNewUsers(Long newUsers) { this.newUsers = newUsers; }
        
        public Double getAvgSessionDuration() { return avgSessionDuration; }
        public void setAvgSessionDuration(Double avgSessionDuration) { this.avgSessionDuration = avgSessionDuration; }
    }

    /**
     * 语言指标
     */
    public static class LanguageMetrics {
        @JsonProperty("supportedLanguages")
        private Integer supportedLanguages;
        
        @JsonProperty("mostUsedLanguage")
        private String mostUsedLanguage;
        
        @JsonProperty("languageDistribution")
        private Map<String, Long> languageDistribution;

        public LanguageMetrics() {}

        public LanguageMetrics(Integer supportedLanguages, String mostUsedLanguage, 
                             Map<String, Long> languageDistribution) {
            this.supportedLanguages = supportedLanguages;
            this.mostUsedLanguage = mostUsedLanguage;
            this.languageDistribution = languageDistribution;
        }

        public Integer getSupportedLanguages() { return supportedLanguages; }
        public void setSupportedLanguages(Integer supportedLanguages) { this.supportedLanguages = supportedLanguages; }
        
        public String getMostUsedLanguage() { return mostUsedLanguage; }
        public void setMostUsedLanguage(String mostUsedLanguage) { this.mostUsedLanguage = mostUsedLanguage; }
        
        public Map<String, Long> getLanguageDistribution() { return languageDistribution; }
        public void setLanguageDistribution(Map<String, Long> languageDistribution) { this.languageDistribution = languageDistribution; }
    }

    /**
     * 错误指标
     */
    public static class ErrorMetrics {
        @JsonProperty("totalErrors")
        private Long totalErrors;
        
        @JsonProperty("errorRate")
        private Double errorRate;
        
        @JsonProperty("commonErrors")
        private Map<String, Long> commonErrors;

        public ErrorMetrics() {}

        public ErrorMetrics(Long totalErrors, Double errorRate, Map<String, Long> commonErrors) {
            this.totalErrors = totalErrors;
            this.errorRate = errorRate;
            this.commonErrors = commonErrors;
        }

        public Long getTotalErrors() { return totalErrors; }
        public void setTotalErrors(Long totalErrors) { this.totalErrors = totalErrors; }
        
        public Double getErrorRate() { return errorRate; }
        public void setErrorRate(Double errorRate) { this.errorRate = errorRate; }
        
        public Map<String, Long> getCommonErrors() { return commonErrors; }
        public void setCommonErrors(Map<String, Long> commonErrors) { this.commonErrors = commonErrors; }
    }

    /**
     * 性能指标
     */
    public static class PerformanceMetrics {
        @JsonProperty("cpuUsage")
        private Double cpuUsage;
        
        @JsonProperty("memoryUsage")
        private Double memoryUsage;
        
        @JsonProperty("diskUsage")
        private Double diskUsage;
        
        @JsonProperty("networkLatency")
        private Double networkLatency;

        public PerformanceMetrics() {}

        public PerformanceMetrics(Double cpuUsage, Double memoryUsage, Double diskUsage, Double networkLatency) {
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
            this.diskUsage = diskUsage;
            this.networkLatency = networkLatency;
        }

        public Double getCpuUsage() { return cpuUsage; }
        public void setCpuUsage(Double cpuUsage) { this.cpuUsage = cpuUsage; }
        
        public Double getMemoryUsage() { return memoryUsage; }
        public void setMemoryUsage(Double memoryUsage) { this.memoryUsage = memoryUsage; }
        
        public Double getDiskUsage() { return diskUsage; }
        public void setDiskUsage(Double diskUsage) { this.diskUsage = diskUsage; }
        
        public Double getNetworkLatency() { return networkLatency; }
        public void setNetworkLatency(Double networkLatency) { this.networkLatency = networkLatency; }
    }

    // 主要字段
    @JsonProperty("overall")
    private OverallMetrics overall;
    
    @JsonProperty("stt")
    private SttMetrics stt;
    
    @JsonProperty("tts")
    private TtsMetrics tts;
    
    @JsonProperty("users")
    private UserMetrics users;
    
    @JsonProperty("languages")
    private LanguageMetrics languages;
    
    @JsonProperty("errors")
    private ErrorMetrics errors;
    
    @JsonProperty("performance")
    private PerformanceMetrics performance;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    public VoiceMonitoringReportDto() {
        this.timestamp = LocalDateTime.now();
    }

    public OverallMetrics getOverall() { return overall; }
    public void setOverall(OverallMetrics overall) { this.overall = overall; }
    
    public SttMetrics getStt() { return stt; }
    public void setStt(SttMetrics stt) { this.stt = stt; }
    
    public TtsMetrics getTts() { return tts; }
    public void setTts(TtsMetrics tts) { this.tts = tts; }
    
    public UserMetrics getUsers() { return users; }
    public void setUsers(UserMetrics users) { this.users = users; }
    
    public LanguageMetrics getLanguages() { return languages; }
    public void setLanguages(LanguageMetrics languages) { this.languages = languages; }
    
    public ErrorMetrics getErrors() { return errors; }
    public void setErrors(ErrorMetrics errors) { this.errors = errors; }
    
    public PerformanceMetrics getPerformance() { return performance; }
    public void setPerformance(PerformanceMetrics performance) { this.performance = performance; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
