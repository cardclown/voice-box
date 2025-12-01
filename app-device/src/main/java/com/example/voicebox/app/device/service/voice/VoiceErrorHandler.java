package com.example.voicebox.app.device.service.voice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 语音错误处理服务
 * 提供统一的错误处理和降级策略
 */
@Service
public class VoiceErrorHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(VoiceErrorHandler.class);
    
    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 1000; // 1秒基础延迟
    
    /**
     * 错误类型枚举
     */
    public enum ErrorType {
        PERMISSION_DENIED("权限被拒绝", "PERMISSION_DENIED", 403, false),
        FILE_TOO_LARGE("文件过大", "FILE_TOO_LARGE", 413, false),
        UNSUPPORTED_FORMAT("格式不支持", "UNSUPPORTED_FORMAT", 415, false),
        DURATION_EXCEEDED("时长超限", "DURATION_EXCEEDED", 400, false),
        STT_SERVICE_ERROR("语音识别服务错误", "STT_SERVICE_ERROR", 502, true),
        TTS_SERVICE_ERROR("语音合成服务错误", "TTS_SERVICE_ERROR", 502, true),
        STORAGE_ERROR("存储失败", "STORAGE_ERROR", 500, true),
        NETWORK_TIMEOUT("网络超时", "TIMEOUT", 504, true),
        DEVICE_ERROR("设备故障", "DEVICE_ERROR", 500, false),
        UNKNOWN_ERROR("未知错误", "INTERNAL_ERROR", 500, true);
        
        private final String message;
        private final String code;
        private final int httpStatus;
        private final boolean retryable;
        
        ErrorType(String message, String code, int httpStatus, boolean retryable) {
            this.message = message;
            this.code = code;
            this.httpStatus = httpStatus;
            this.retryable = retryable;
        }
        
        public String getMessage() { return message; }
        public String getCode() { return code; }
        public int getHttpStatus() { return httpStatus; }
        public boolean isRetryable() { return retryable; }
    }
    
    /**
     * 语音异常类
     */
    public static class VoiceException extends RuntimeException {
        private final ErrorType errorType;
        private final Map<String, Object> context;
        
        public VoiceException(ErrorType errorType, String message) {
            super(message);
            this.errorType = errorType;
            this.context = new HashMap<>();
        }
        
        public VoiceException(ErrorType errorType, String message, Throwable cause) {
            super(message, cause);
            this.errorType = errorType;
            this.context = new HashMap<>();
        }
        
        public VoiceException addContext(String key, Object value) {
            this.context.put(key, value);
            return this;
        }
        
        public ErrorType getErrorType() { return errorType; }
        public Map<String, Object> getContext() { return context; }
    }
    
    /**
     * 带重试的执行
     * 
     * @param operation 要执行的操作
     * @param operationName 操作名称（用于日志）
     * @return 操作结果
     */
    public <T> T executeWithRetry(Supplier<T> operation, String operationName) {
        return executeWithRetry(operation, operationName, MAX_RETRIES);
    }
    
    /**
     * 带重试的执行（指定重试次数）
     */
    public <T> T executeWithRetry(Supplier<T> operation, String operationName, int maxRetries) {
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < maxRetries) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                attempt++;
                
                // 判断是否应该重试
                if (!shouldRetry(e) || attempt >= maxRetries) {
                    break;
                }
                
                // 指数退避
                long delay = calculateBackoffDelay(attempt);
                logger.warn("操作失败，第{}次重试: {}, 延迟{}ms", attempt, operationName, delay);
                
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new VoiceException(ErrorType.UNKNOWN_ERROR, "重试被中断", ie);
                }
            }
        }
        
        // 所有重试都失败
        logger.error("操作最终失败: {}, 已重试{}次", operationName, maxRetries);
        throw wrapException(lastException, operationName);
    }
    
    /**
     * 处理语音错误
     * 
     * @param e 异常
     * @param context 上下文信息
     * @return 错误响应
     */
    public Map<String, Object> handleError(Exception e, String context) {
        ErrorType errorType = determineErrorType(e);
        
        // 记录错误日志
        logError(e, errorType, context);
        
        // 构建错误响应
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorCode", errorType.getCode());
        errorResponse.put("errorMessage", errorType.getMessage());
        errorResponse.put("httpStatus", errorType.getHttpStatus());
        errorResponse.put("retryable", errorType.isRetryable());
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        
        // 添加用户友好的提示
        errorResponse.put("userMessage", getUserFriendlyMessage(errorType));
        
        // 添加建议操作
        errorResponse.put("suggestedAction", getSuggestedAction(errorType));
        
        return errorResponse;
    }
    
    /**
     * 判断是否应该重试
     */
    private boolean shouldRetry(Exception e) {
        ErrorType errorType = determineErrorType(e);
        return errorType.isRetryable();
    }
    
    /**
     * 计算退避延迟（指数退避）
     */
    private long calculateBackoffDelay(int attempt) {
        return (long) (BASE_DELAY_MS * Math.pow(2, attempt - 1));
    }
    
    /**
     * 确定错误类型
     */
    private ErrorType determineErrorType(Exception e) {
        if (e instanceof VoiceException) {
            return ((VoiceException) e).getErrorType();
        }
        
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        
        if (message.contains("permission") || message.contains("权限")) {
            return ErrorType.PERMISSION_DENIED;
        } else if (message.contains("file too large") || message.contains("文件过大")) {
            return ErrorType.FILE_TOO_LARGE;
        } else if (message.contains("unsupported") || message.contains("不支持")) {
            return ErrorType.UNSUPPORTED_FORMAT;
        } else if (message.contains("timeout") || message.contains("超时")) {
            return ErrorType.NETWORK_TIMEOUT;
        } else if (message.contains("stt") || message.contains("speech to text")) {
            return ErrorType.STT_SERVICE_ERROR;
        } else if (message.contains("tts") || message.contains("text to speech")) {
            return ErrorType.TTS_SERVICE_ERROR;
        } else if (message.contains("storage") || message.contains("存储")) {
            return ErrorType.STORAGE_ERROR;
        } else if (message.contains("device") || message.contains("设备")) {
            return ErrorType.DEVICE_ERROR;
        }
        
        return ErrorType.UNKNOWN_ERROR;
    }
    
    /**
     * 包装异常
     */
    private VoiceException wrapException(Exception e, String context) {
        if (e instanceof VoiceException) {
            return (VoiceException) e;
        }
        
        ErrorType errorType = determineErrorType(e);
        return new VoiceException(errorType, context + ": " + e.getMessage(), e);
    }
    
    /**
     * 记录错误日志
     */
    private void logError(Exception e, ErrorType errorType, String context) {
        Map<String, Object> logContext = new HashMap<>();
        logContext.put("errorType", errorType.getCode());
        logContext.put("errorMessage", e.getMessage());
        logContext.put("context", context);
        logContext.put("timestamp", LocalDateTime.now());
        
        if (e instanceof VoiceException) {
            logContext.putAll(((VoiceException) e).getContext());
        }
        
        logger.error("语音错误: {}", logContext, e);
    }
    
    /**
     * 获取用户友好的错误消息
     */
    private String getUserFriendlyMessage(ErrorType errorType) {
        switch (errorType) {
            case PERMISSION_DENIED:
                return "请授予麦克风权限以使用语音功能";
            case FILE_TOO_LARGE:
                return "音频文件过大，请录制较短的语音（最大10MB）";
            case UNSUPPORTED_FORMAT:
                return "不支持的音频格式，请使用MP3、WAV或OGG格式";
            case DURATION_EXCEEDED:
                return "录音时长超过限制（最长5分钟）";
            case STT_SERVICE_ERROR:
                return "语音识别暂时不可用，请稍后重试或使用文字输入";
            case TTS_SERVICE_ERROR:
                return "语音合成暂时不可用，将仅显示文本回复";
            case STORAGE_ERROR:
                return "保存语音文件失败，请检查存储空间";
            case NETWORK_TIMEOUT:
                return "网络连接超时，请检查网络连接后重试";
            case DEVICE_ERROR:
                return "麦克风设备故障，请检查设备连接";
            default:
                return "处理失败，请重试";
        }
    }
    
    /**
     * 获取建议操作
     */
    private String getSuggestedAction(ErrorType errorType) {
        switch (errorType) {
            case PERMISSION_DENIED:
                return "在浏览器设置中允许麦克风权限";
            case FILE_TOO_LARGE:
                return "录制较短的语音或压缩音频文件";
            case UNSUPPORTED_FORMAT:
                return "使用支持的音频格式";
            case DURATION_EXCEEDED:
                return "分段录制或使用文字输入";
            case STT_SERVICE_ERROR:
            case TTS_SERVICE_ERROR:
                return "稍后重试或使用文字输入";
            case STORAGE_ERROR:
                return "清理存储空间或联系管理员";
            case NETWORK_TIMEOUT:
                return "检查网络连接后重试";
            case DEVICE_ERROR:
                return "检查麦克风连接或使用其他设备";
            default:
                return "刷新页面或联系技术支持";
        }
    }
    
    /**
     * 创建权限错误
     */
    public static VoiceException permissionDenied(String message) {
        return new VoiceException(ErrorType.PERMISSION_DENIED, message);
    }
    
    /**
     * 创建文件过大错误
     */
    public static VoiceException fileTooLarge(long fileSize, long maxSize) {
        return new VoiceException(
            ErrorType.FILE_TOO_LARGE,
            String.format("文件大小%d超过限制%d", fileSize, maxSize)
        ).addContext("fileSize", fileSize).addContext("maxSize", maxSize);
    }
    
    /**
     * 创建格式不支持错误
     */
    public static VoiceException unsupportedFormat(String format) {
        return new VoiceException(
            ErrorType.UNSUPPORTED_FORMAT,
            "不支持的格式: " + format
        ).addContext("format", format);
    }
    
    /**
     * 创建服务错误
     */
    public static VoiceException serviceError(String serviceName, String message) {
        ErrorType errorType = serviceName.toLowerCase().contains("stt") 
            ? ErrorType.STT_SERVICE_ERROR 
            : ErrorType.TTS_SERVICE_ERROR;
        return new VoiceException(errorType, message)
            .addContext("service", serviceName);
    }
}
