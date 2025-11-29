package com.example.voicebox.app.device.service.voice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

/**
 * 语音服务代理
 * 负责服务降级和自动切换
 */
@Slf4j
@Service
public class VoiceServiceProxy {

    @Autowired
    private DoubaoVoiceService doubaoVoiceService;

    @Value("${voicebox.voice.primary.provider:doubao}")
    private String primaryProvider;

    @Value("${voicebox.voice.max.retries:3}")
    private int maxRetries;

    /**
     * 语音识别 (带重试和降级)
     */
    public String speechToText(InputStream audioStream, String language) throws Exception {
        log.info("调用语音识别服务 - provider: {}, language: {}", primaryProvider, language);
        
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < maxRetries) {
            try {
                // 目前只支持豆包，后续可扩展其他服务商
                CompletableFuture<String> future = doubaoVoiceService.speechToText(audioStream, language);
                String result = future.get();
                
                log.info("语音识别成功 - attempt: {}, result: {}", attempt + 1, result);
                return result;
                
            } catch (Exception e) {
                lastException = e;
                attempt++;
                
                if (attempt < maxRetries) {
                    long delay = (long) Math.pow(2, attempt) * 1000; // 指数退避
                    log.warn("语音识别失败，第{}次重试，延迟{}ms", attempt, delay);
                    Thread.sleep(delay);
                } else {
                    log.error("语音识别最终失败，已重试{}次", maxRetries);
                }
            }
        }
        
        throw new VoiceServiceException("语音识别失败", lastException);
    }

    /**
     * 语音合成 (带重试和降级)
     */
    public byte[] textToSpeech(String text, String language, String voiceName) throws Exception {
        log.info("调用语音合成服务 - provider: {}, text: {}, language: {}, voice: {}", 
                primaryProvider, text, language, voiceName);
        
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < maxRetries) {
            try {
                // 目前只支持豆包，后续可扩展其他服务商
                CompletableFuture<byte[]> future = doubaoVoiceService.textToSpeech(text, language, voiceName);
                byte[] result = future.get();
                
                log.info("语音合成成功 - attempt: {}, size: {} bytes", attempt + 1, result.length);
                return result;
                
            } catch (Exception e) {
                lastException = e;
                attempt++;
                
                if (attempt < maxRetries) {
                    long delay = (long) Math.pow(2, attempt) * 1000; // 指数退避
                    log.warn("语音合成失败，第{}次重试，延迟{}ms", attempt, delay);
                    Thread.sleep(delay);
                } else {
                    log.error("语音合成最终失败，已重试{}次", maxRetries);
                }
            }
        }
        
        throw new VoiceServiceException("语音合成失败", lastException);
    }

    /**
     * 流式语音合成
     */
    public void streamTextToSpeech(String text, String language, String voiceName, 
                                   DoubaoVoiceService.AudioDataCallback callback) {
        log.info("调用流式语音合成服务 - provider: {}, text: {}", primaryProvider, text);
        
        try {
            doubaoVoiceService.streamTextToSpeech(text, language, voiceName, callback);
        } catch (Exception e) {
            log.error("流式语音合成失败", e);
            callback.onError(e);
        }
    }

    /**
     * 语音服务异常
     */
    public static class VoiceServiceException extends Exception {
        public VoiceServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
