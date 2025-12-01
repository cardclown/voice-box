package com.example.voicebox.app.device.controller;

import com.example.voicebox.app.device.dto.VoiceMonitoringReportDto;
import com.example.voicebox.app.device.service.voice.VoiceMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 语音监控控制器
 * 提供语音系统监控相关的 API 端点
 */
@RestController
@RequestMapping("/api/voice/monitoring")
@CrossOrigin(origins = "*")
public class VoiceMonitoringController {

    @Autowired
    private VoiceMonitoringService voiceMonitoringService;

    /**
     * 获取完整的监控报告
     * GET /api/voice/monitoring/report
     */
    @GetMapping("/report")
    public ResponseEntity<VoiceMonitoringReportDto> getMonitoringReport() {
        try {
            VoiceMonitoringReportDto report = voiceMonitoringService.generateMonitoringReport();
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            System.err.println("获取监控报告失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取系统健康状态
     * GET /api/voice/monitoring/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        try {
            Map<String, Object> healthSummary = voiceMonitoringService.getHealthSummary();
            return ResponseEntity.ok(healthSummary);
        } catch (Exception e) {
            System.err.println("获取健康状态失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取整体指标
     * GET /api/voice/monitoring/metrics/overall
     */
    @GetMapping("/metrics/overall")
    public ResponseEntity<VoiceMonitoringReportDto.OverallMetrics> getOverallMetrics() {
        try {
            VoiceMonitoringReportDto report = voiceMonitoringService.generateMonitoringReport();
            return ResponseEntity.ok(report.getOverall());
        } catch (Exception e) {
            System.err.println("获取整体指标失败: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取 STT 指标
     * GET /api/voice/monitoring/metrics/stt
     */
    @GetMapping("/metrics/stt")
    public ResponseEntity<VoiceMonitoringReportDto.SttMetrics> getSttMetrics() {
        try {
            VoiceMonitoringReportDto report = voiceMonitoringService.generateMonitoringReport();
            return ResponseEntity.ok(report.getStt());
        } catch (Exception e) {
            System.err.println("获取 STT 指标失败: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取 TTS 指标
     * GET /api/voice/monitoring/metrics/tts
     */
    @GetMapping("/metrics/tts")
    public ResponseEntity<VoiceMonitoringReportDto.TtsMetrics> getTtsMetrics() {
        try {
            VoiceMonitoringReportDto report = voiceMonitoringService.generateMonitoringReport();
            return ResponseEntity.ok(report.getTts());
        } catch (Exception e) {
            System.err.println("获取 TTS 指标失败: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取用户指标
     * GET /api/voice/monitoring/metrics/users
     */
    @GetMapping("/metrics/users")
    public ResponseEntity<VoiceMonitoringReportDto.UserMetrics> getUserMetrics() {
        try {
            VoiceMonitoringReportDto report = voiceMonitoringService.generateMonitoringReport();
            return ResponseEntity.ok(report.getUsers());
        } catch (Exception e) {
            System.err.println("获取用户指标失败: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取性能指标
     * GET /api/voice/monitoring/metrics/performance
     */
    @GetMapping("/metrics/performance")
    public ResponseEntity<VoiceMonitoringReportDto.PerformanceMetrics> getPerformanceMetrics() {
        try {
            VoiceMonitoringReportDto report = voiceMonitoringService.generateMonitoringReport();
            return ResponseEntity.ok(report.getPerformance());
        } catch (Exception e) {
            System.err.println("获取性能指标失败: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
