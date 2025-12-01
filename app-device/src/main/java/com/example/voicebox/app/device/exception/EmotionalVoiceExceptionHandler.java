package com.example.voicebox.app.device.exception;

import com.example.voicebox.app.device.service.emotional.EmotionalVoiceErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 情感语音模块全局异常处理器
 */
@RestControllerAdvice
public class EmotionalVoiceExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(EmotionalVoiceExceptionHandler.class);
    
    @Autowired
    private EmotionalVoiceErrorHandler errorHandler;
    
    /**
     * 处理情感识别异常
     */
    @ExceptionHandler(EmotionRecognitionException.class)
    public ResponseEntity<Map<String, Object>> handleEmotionRecognitionException(EmotionRecognitionException ex) {
        logger.error("情感识别异常", ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", errorHandler.generateFriendlyErrorMessage("EMOTION_RECOGNITION", ex));
        response.put("fallbackData", errorHandler.handleEmotionRecognitionFailure(ex));
        
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    /**
     * 处理语音合成异常
     */
    @ExceptionHandler(VoiceSynthesisException.class)
    public ResponseEntity<Map<String, Object>> handleVoiceSynthesisException(VoiceSynthesisException ex) {
        logger.error("语音合成异常", ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", errorHandler.generateFriendlyErrorMessage("VOICE_SYNTHESIS", ex));
        
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        logger.error("未处理的异常", ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", errorHandler.generateFriendlyErrorMessage("UNKNOWN", ex));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

/**
 * 情感识别异常
 */
class EmotionRecognitionException extends RuntimeException {
    public EmotionRecognitionException(String message) {
        super(message);
    }
    
    public EmotionRecognitionException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
 * 语音合成异常
 */
class VoiceSynthesisException extends RuntimeException {
    public VoiceSynthesisException(String message) {
        super(message);
    }
    
    public VoiceSynthesisException(String message, Throwable cause) {
        super(message, cause);
    }
}
