package com.example.voicebox.app.device.controller;

import com.example.voicebox.app.device.service.voice.VoiceInputService;
import com.example.voicebox.app.device.service.voice.VoiceOutputService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

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
        
        try {
            VoiceUploadResponse response = voiceInputService.processVoiceInput(
                    file, userId, sessionId, language);
            
            log.info("语音上传处理成功 - fileId: {}, text: {}", 
                    response.getFileId(), response.getRecognizedText());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("语音上传参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(VoiceUploadResponse.error(e.getMessage()));
                    
        } catch (Exception e) {
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
        
        try {
            VoiceSynthesisResponse response = voiceOutputService.synthesizeVoice(
                    request.getText(),
                    request.getUserId(),
                    request.getLanguage(),
                    request.getVoiceName());
            
            log.info("语音合成成功 - fileId: {}, audioUrl: {}", 
                    response.getFileId(), response.getAudioUrl());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
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
