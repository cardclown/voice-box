package com.example.voicebox.app.device.service.voice;

import com.example.voicebox.app.device.controller.VoiceController.VoiceUploadResponse;
import com.example.voicebox.app.device.domain.VoiceMessage;
import com.example.voicebox.app.device.repository.VoiceMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 语音输入服务
 */
@Slf4j
@Service
public class VoiceInputService {

    @Autowired
    private VoiceServiceProxy voiceServiceProxy;

    @Autowired
    private VoiceStorageService storageService;

    @Autowired
    private VoiceMessageRepository messageRepository;

    @Value("${voicebox.voice.max.file.size:10485760}") // 10MB
    private long maxFileSize;

    /**
     * 处理语音输入
     */
    public VoiceUploadResponse processVoiceInput(
            MultipartFile file,
            Long userId,
            Long sessionId,
            String language) throws Exception {

        log.info("开始处理语音输入 - userId: {}, sessionId: {}, language: {}", 
                userId, sessionId, language);

        // 1. 验证文件
        validateAudioFile(file);

        // 2. 保存原始音频文件
        String fileId = storageService.saveAudioFile(file, userId);
        log.info("音频文件已保存 - fileId: {}", fileId);

        // 3. 调用STT服务
        String recognizedText;
        try {
            recognizedText = voiceServiceProxy.speechToText(file.getInputStream(), language);
            log.info("语音识别成功 - text: {}", recognizedText);
        } catch (Exception e) {
            log.error("语音识别失败", e);
            // 即使识别失败，也保存文件记录
            recognizedText = "[识别失败]";
        }

        // 4. 计算音频时长
        int duration = estimateAudioDuration(file);

        // 5. 保存语音消息记录
        VoiceMessage voiceMessage = new VoiceMessage();
        voiceMessage.setUserId(userId);
        voiceMessage.setSessionId(sessionId);
        voiceMessage.setFileId(fileId);
        voiceMessage.setFilePath(storageService.getFilePath(fileId));
        voiceMessage.setFileSize(file.getSize());
        voiceMessage.setDuration(duration);
        voiceMessage.setFormat(getFileFormat(file));
        voiceMessage.setSampleRate(16000); // 默认采样率
        voiceMessage.setRecognizedText(recognizedText);
        voiceMessage.setConfidence(BigDecimal.valueOf(0.95)); // 默认置信度
        voiceMessage.setLanguage(language);
        voiceMessage.setIsInput(true);
        voiceMessage.setCreatedAt(LocalDateTime.now());

        messageRepository.save(voiceMessage);
        log.info("语音消息记录已保存 - id: {}", voiceMessage.getId());

        // 6. 返回结果
        VoiceUploadResponse response = new VoiceUploadResponse();
        response.setSuccess(true);
        response.setFileId(fileId);
        response.setRecognizedText(recognizedText);
        response.setConfidence(BigDecimal.valueOf(0.95));
        response.setDuration(duration);

        return response;
    }

    /**
     * 验证音频文件
     */
    private void validateAudioFile(MultipartFile file) {
        // 检查文件是否为空
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        // 检查文件大小
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                    String.format("文件大小超过限制 (最大 %d MB)", maxFileSize / 1024 / 1024));
        }

        // 检查文件格式
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            throw new IllegalArgumentException("不支持的文件格式，仅支持音频文件");
        }

        log.info("文件验证通过 - size: {}, type: {}", file.getSize(), contentType);
    }

    /**
     * 估算音频时长（秒）
     */
    private int estimateAudioDuration(MultipartFile file) {
        // 简单估算：假设比特率为 128kbps
        long fileSizeBytes = file.getSize();
        int bitrateKbps = 128;
        int durationSeconds = (int) (fileSizeBytes * 8 / (bitrateKbps * 1000));
        
        log.debug("估算音频时长 - size: {} bytes, duration: {} seconds", fileSizeBytes, durationSeconds);
        return durationSeconds;
    }

    /**
     * 获取文件格式
     */
    private String getFileFormat(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            if (contentType.contains("mpeg")) return "mp3";
            if (contentType.contains("wav")) return "wav";
            if (contentType.contains("ogg")) return "ogg";
            if (contentType.contains("webm")) return "webm";
        }
        
        // 从文件名获取扩展名
        String filename = file.getOriginalFilename();
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf(".") + 1);
        }
        
        return "unknown";
    }

    /**
     * 加载音频文件
     */
    public Resource loadAudioFile(String fileId) throws Exception {
        return storageService.loadAudioFile(fileId);
    }
}
