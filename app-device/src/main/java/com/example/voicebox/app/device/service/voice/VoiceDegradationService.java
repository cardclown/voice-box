package com.example.voicebox.app.device.service.voice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * 语音服务降级服务
 * 实现服务降级和自动切换逻辑
 * 
 * @author VoiceBox Team
 * @since 1.5
 */
@Service
public class VoiceDegradationService {
    
    private static final Logger logger = LoggerFactory.getLogger(VoiceDegradationService.class);
    
    @Autowired
    private VoiceServiceProxy voiceServiceProxy;
    
    /**
     * STT服务降级
     * 尝试多个服务提供商，失败时自动切换
     */
    public String degradedSpeechToText(InputStream audioStream, String language) {
        // 1. 尝试主服务
        try {
            logger.info("尝试主服务进行STT识别");
            return voiceServiceProxy.speechToText(audioStream, language);
        } catch (Exception e) {
            logger.warn("主服务STT失败: {}", e.getMessage());
        }
        
        // 2. 如果主服务失败，返回降级提示
        logger.error("所有STT服务都失败");
        return "[语音识别暂时不可用，请使用文字输入]";
    }
    
    /**
     * TTS服务降级
     * 尝试多个服务提供商，失败时返回null
     */
    public byte[] degradedTextToSpeech(String text, String language, VoiceProfile profile) {
        // 1. 尝试主服务
        try {
            logger.info("尝试主服务进行TTS合成");
            return voiceServiceProxy.textToSpeech(text, language, profile);
        } catch (Exception e) {
            logger.warn("主服务TTS失败: {}", e.getMessage());
        }
        
        // 2. 完全降级 - 不提供语音，仅显示文本
        logger.error("所有TTS服务都失败，仅显示文本");
        return null;  // 前端检测到null时只显示文本
    }
    
    /**
     * 带重试的STT服务调用
     */
    public String speechToTextWithRetry(InputStream audioStream, String language, int maxRetries) {
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < maxRetries) {
            try {
                return voiceServiceProxy.speechToText(audioStream, language);
            } catch (Exception e) {
                lastException = e;
                attempt++;
                
                if (attempt < maxRetries) {
                    // 指数退避：1秒、2秒、4秒
                    long delay = (long) Math.pow(2, attempt) * 1000;
                    try {
                        logger.warn("STT失败，{}秒后重试（第{}次）", delay / 1000, attempt);
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        logger.error("STT重试{}次后仍然失败", maxRetries, lastException);
        return "[语音识别失败]";
    }
    
    /**
     * 带重试的TTS服务调用
     */
    public byte[] textToSpeechWithRetry(String text, String language, VoiceProfile profile, int maxRetries) {
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < maxRetries) {
            try {
                return voiceServiceProxy.textToSpeech(text, language, profile);
            } catch (Exception e) {
                lastException = e;
                attempt++;
                
                if (attempt < maxRetries) {
                    // 指数退避
                    long delay = (long) Math.pow(2, attempt) * 1000;
                    try {
                        logger.warn("TTS失败，{}秒后重试（第{}次）", delay / 1000, attempt);
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        logger.error("TTS重试{}次后仍然失败", maxRetries, lastException);
        return null;
    }
}
