package com.example.voicebox.app.device.service.emotional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 情感语音错误处理服务
 * 
 * 功能：
 * 1. 情感识别失败降级
 * 2. 语音合成失败降级
 * 3. 友好错误提示
 * 4. 详细错误日志
 */
@Service
public class EmotionalVoiceErrorHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(EmotionalVoiceErrorHandler.class);
    
    /**
     * 处理情感识别失败
     * 
     * @param error 错误信息
     * @return 降级后的结果
     */
    public Map<String, Object> handleEmotionRecognitionFailure(Exception error) {
        logger.error("情感识别失败，使用降级策略", error);
        
        Map<String, Object> fallbackResult = new HashMap<>();
        fallbackResult.put("primaryEmotion", "NEUTRAL");
        fallbackResult.put("confidence", 0.5);
        fallbackResult.put("fallback", true);
        fallbackResult.put("message", "情感识别暂时不可用，返回默认结果");
        
        Map<String, Double> emotionScores = new HashMap<>();
        emotionScores.put("NEUTRAL", 1.0);
        fallbackResult.put("emotionScores", emotionScores);
        
        return fallbackResult;
    }
    
    /**
     * 处理语音合成失败
     * 
     * @param error 错误信息
     * @return 降级后的结果
     */
    public byte[] handleVoiceSynthesisFailure(Exception error) {
        logger.error("语音合成失败，使用降级策略", error);
        
        // 返回空音频或默认音频
        // 实际应该返回一个预先录制的默认音频
        return new byte[0];
    }
    
    /**
     * 处理特征提取失败
     * 
     * @param error 错误信息
     * @return 降级后的特征
     */
    public Map<String, Object> handleFeatureExtractionFailure(Exception error) {
        logger.error("特征提取失败，使用降级策略", error);
        
        Map<String, Object> fallbackFeatures = new HashMap<>();
        fallbackFeatures.put("pitch", 200.0);
        fallbackFeatures.put("volume", 0.5);
        fallbackFeatures.put("speed", 150.0);
        fallbackFeatures.put("fallback", true);
        
        return fallbackFeatures;
    }
    
    /**
     * 生成友好的错误消息
     * 
     * @param errorType 错误类型
     * @param error 原始错误
     * @return 友好的错误消息
     */
    public String generateFriendlyErrorMessage(String errorType, Exception error) {
        switch (errorType) {
            case "EMOTION_RECOGNITION":
                return "抱歉，情绪识别服务暂时不可用，请稍后再试";
            case "VOICE_SYNTHESIS":
                return "抱歉，语音合成服务暂时不可用，请稍后再试";
            case "FEATURE_EXTRACTION":
                return "抱歉，语音分析服务暂时不可用，请稍后再试";
            case "NETWORK_ERROR":
                return "网络连接失败，请检查网络设置后重试";
            case "TIMEOUT":
                return "请求超时，请稍后再试";
            case "INVALID_INPUT":
                return "输入数据格式不正确，请检查后重试";
            default:
                return "服务暂时不可用，请稍后再试";
        }
    }
    
    /**
     * 记录详细错误日志
     * 
     * @param operation 操作名称
     * @param error 错误信息
     * @param context 上下文信息
     */
    public void logDetailedError(String operation, Exception error, Map<String, Object> context) {
        logger.error("操作失败: operation={}, error={}, context={}", 
            operation, error.getMessage(), context, error);
        
        // 可以发送到监控系统
        // 例如：Sentry, CloudWatch, etc.
    }
    
    /**
     * 判断是否应该重试
     * 
     * @param error 错误信息
     * @return 是否应该重试
     */
    public boolean shouldRetry(Exception error) {
        // 网络错误、超时等可以重试
        String message = error.getMessage();
        if (message == null) {
            return false;
        }
        
        return message.contains("timeout") || 
               message.contains("connection") ||
               message.contains("network");
    }
    
    /**
     * 获取重试延迟时间（毫秒）
     * 
     * @param attemptNumber 重试次数
     * @return 延迟时间
     */
    public long getRetryDelay(int attemptNumber) {
        // 指数退避策略
        return (long) Math.min(1000 * Math.pow(2, attemptNumber), 10000);
    }
}
