package com.example.voicebox.app.device.controller;

import com.example.voicebox.app.device.service.voice.VoiceInputService;
import com.example.voicebox.app.device.service.voice.VoiceOutputService;
import com.example.voicebox.app.device.service.voice.StreamingVoiceService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 语音交互控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/voice")
@CrossOrigin(origins = "*")
public class VoiceController {

    @Autowired
    private VoiceInputService voiceInputService;

    @Autowired
    private VoiceOutputService voiceOutputService;
    
    @Autowired
    private StreamingVoiceService streamingVoiceService;
    
    @Autowired
    private com.example.voicebox.app.device.service.voice.VoiceMetricsService metricsService;

    /**
     * 上传语音文件并转换为文本
     */
    @PostMapping("/upload")
    public ResponseEntity<VoiceUploadResponse> uploadVoice(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId,
            @RequestParam("sessionId") Long sessionId,
            @RequestParam(value = "language", defaultValue = "zh-CN") String language) {
        
        log.info("收到语音上传请求 - userId: {}, sessionId: {}, language: {}, fileSize: {}", 
                userId, sessionId, language, file.getSize());
        
        // 记录请求开始
        String requestId = metricsService.recordRequestStart("stt", userId, language);
        long startTime = System.currentTimeMillis();
        
        try {
            VoiceUploadResponse response = voiceInputService.processVoiceInput(
                    file, userId, sessionId, language);
            
            // 记录请求成功
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordRequestSuccess(requestId, "stt", duration);
            
            log.info("语音上传处理成功 - fileId: {}, text: {}, duration: {}ms", 
                    response.getFileId(), response.getRecognizedText(), duration);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            // 记录请求失败
            metricsService.recordRequestFailure(requestId, "stt", "INVALID_ARGUMENT", e.getMessage());
            
            log.warn("语音上传参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(VoiceUploadResponse.error(e.getMessage()));
                    
        } catch (Exception e) {
            // 记录请求失败
            metricsService.recordRequestFailure(requestId, "stt", "INTERNAL_ERROR", e.getMessage());
            
            log.error("语音上传处理失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(VoiceUploadResponse.error("语音处理失败: " + e.getMessage()));
        }
    }

    /**
     * 将文本转换为语音
     */
    @PostMapping("/synthesize")
    public ResponseEntity<VoiceSynthesisResponse> synthesizeVoice(
            @RequestBody VoiceSynthesisRequest request) {
        
        log.info("收到语音合成请求 - userId: {}, text: {}, language: {}", 
                request.getUserId(), request.getText(), request.getLanguage());
        
        // 记录请求开始
        String requestId = metricsService.recordRequestStart("tts", request.getUserId(), request.getLanguage());
        long startTime = System.currentTimeMillis();
        
        try {
            VoiceSynthesisResponse response = voiceOutputService.synthesizeVoice(
                    request.getText(),
                    request.getUserId(),
                    request.getSessionId(),
                    request.getLanguage(),
                    request.getVoiceName());
            
            // 记录请求成功
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordRequestSuccess(requestId, "tts", duration);
            
            log.info("语音合成成功 - fileId: {}, audioUrl: {}, duration: {}ms", 
                    response.getFileId(), response.getAudioUrl(), duration);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            // 记录请求失败
            metricsService.recordRequestFailure(requestId, "tts", "INTERNAL_ERROR", e.getMessage());
            
            log.error("语音合成失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(VoiceSynthesisResponse.error("语音合成失败: " + e.getMessage()));
        }
    }

    /**
     * 获取语音文件
     */
    @GetMapping("/audio/{fileId}")
    public ResponseEntity<Resource> getAudioFile(@PathVariable String fileId) {
        log.info("请求音频文件 - fileId: {}", fileId);
        
        try {
            Resource resource = voiceInputService.loadAudioFile(fileId);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("audio/mpeg"))
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("获取音频文件失败 - fileId: {}", fileId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 流式语音合成（SSE）
     */
    @GetMapping(value = "/stream/synthesize", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSynthesize(
            @RequestParam("text") String text,
            @RequestParam("userId") Long userId,
            @RequestParam(value = "language", defaultValue = "zh-CN") String language) {
        
        String sessionId = UUID.randomUUID().toString();
        log.info("开始流式语音合成 - sessionId: {}, userId: {}, textLength: {}", 
                sessionId, userId, text.length());
        
        return streamingVoiceService.startStreaming(sessionId, text, userId, language);
    }
    
    /**
     * 停止流式语音合成
     */
    @PostMapping("/stream/stop")
    public ResponseEntity<Void> stopStreaming(@RequestParam("sessionId") String sessionId) {
        log.info("停止流式语音合成 - sessionId: {}", sessionId);
        streamingVoiceService.stopStreaming(sessionId);
        return ResponseEntity.ok().build();
    }

    // ==================== DTO类 ====================

    @Data
    public static class VoiceUploadResponse {
        private boolean success;
        private String fileId;
        private String recognizedText;
        private BigDecimal confidence;
        private Integer duration;
        private String errorMessage;

        public static VoiceUploadResponse error(String message) {
            VoiceUploadResponse response = new VoiceUploadResponse();
            response.setSuccess(false);
            response.setErrorMessage(message);
            return response;
        }
    }

    @Data
    public static class VoiceSynthesisRequest {
        private String text;
        private Long userId;
        private Long sessionId;
        private String language = "zh-CN";
        private String voiceName = "zh_female_qingxin";
    }

    @Data
    public static class VoiceSynthesisResponse {
        private boolean success;
        private String fileId;
        private String audioUrl;
        private Integer duration;
        private String errorMessage;

        public static VoiceSynthesisResponse error(String message) {
            VoiceSynthesisResponse response = new VoiceSynthesisResponse();
            response.setSuccess(false);
            response.setErrorMessage(message);
            return response;
        }
    }
}
