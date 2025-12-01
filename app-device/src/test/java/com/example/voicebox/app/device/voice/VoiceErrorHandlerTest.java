package com.example.voicebox.app.device.voice;

import com.example.voicebox.app.device.service.voice.VoiceErrorHandler;
import com.example.voicebox.app.device.service.voice.VoiceErrorHandler.ErrorType;
import com.example.voicebox.app.device.service.voice.VoiceErrorHandler.VoiceException;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * 错误处理服务单元测试
 * Validates: Requirements 13.1, 13.2, 13.3, 13.6
 */
public class VoiceErrorHandlerTest {
    
    private VoiceErrorHandler errorHandler;
    
    @Before
    public void setUp() {
        errorHandler = new VoiceErrorHandler();
    }
    
    @Test
    public void testHandlePermissionError() {
        // 测试权限错误处理
        Exception permissionError = new Exception("Permission denied");
        Map<String, Object> response = errorHandler.handleError(permissionError, "测试权限");
        
        assertFalse((Boolean) response.get("success"));
        assertEquals("PERMISSION_DENIED", response.get("errorCode"));
        assertEquals(403, response.get("httpStatus"));
        assertFalse((Boolean) response.get("retryable"));
        assertNotNull(response.get("userMessage"));
        assertNotNull(response.get("suggestedAction"));
    }
    
    @Test
    public void testHandleFileTooLargeError() {
        // 测试文件过大错误
        VoiceException error = VoiceErrorHandler.fileTooLarge(15 * 1024 * 1024, 10 * 1024 * 1024);
        Map<String, Object> response = errorHandler.handleError(error, "文件上传");
        
        assertFalse((Boolean) response.get("success"));
        assertEquals("FILE_TOO_LARGE", response.get("errorCode"));
        assertEquals(413, response.get("httpStatus"));
        assertFalse((Boolean) response.get("retryable"));
    }
    
    @Test
    public void testHandleUnsupportedFormatError() {
        // 测试格式不支持错误
        VoiceException error = VoiceErrorHandler.unsupportedFormat("avi");
        Map<String, Object> response = errorHandler.handleError(error, "格式验证");
        
        assertFalse((Boolean) response.get("success"));
        assertEquals("UNSUPPORTED_FORMAT", response.get("errorCode"));
        assertEquals(415, response.get("httpStatus"));
    }
    
    @Test
    public void testHandleServiceError() {
        // 测试服务不可用错误
        VoiceException error = VoiceErrorHandler.serviceError("STT", "服务超时");
        Map<String, Object> response = errorHandler.handleError(error, "STT调用");
        
        assertFalse((Boolean) response.get("success"));
        assertEquals("STT_SERVICE_ERROR", response.get("errorCode"));
        assertEquals(502, response.get("httpStatus"));
        assertTrue((Boolean) response.get("retryable"));
    }
    
    @Test
    public void testHandleNetworkTimeout() {
        // 测试网络超时错误
        Exception timeoutError = new Exception("Connection timeout");
        Map<String, Object> response = errorHandler.handleError(timeoutError, "网络请求");
        
        assertFalse((Boolean) response.get("success"));
        assertEquals("TIMEOUT", response.get("errorCode"));
        assertEquals(504, response.get("httpStatus"));
        assertTrue((Boolean) response.get("retryable"));
    }
    
    @Test
    public void testExecuteWithRetrySuccess() {
        // 测试重试成功
        AtomicInteger attempts = new AtomicInteger(0);
        
        String result = errorHandler.executeWithRetry(() -> {
            attempts.incrementAndGet();
            if (attempts.get() < 2) {
                throw new RuntimeException("临时失败");
            }
            return "成功";
        }, "测试操作");
        
        assertEquals("成功", result);
        assertEquals(2, attempts.get());
    }
    
    @Test
    public void testExecuteWithRetryMaxAttempts() {
        // 测试达到最大重试次数
        AtomicInteger attempts = new AtomicInteger(0);
        
        try {
            errorHandler.executeWithRetry(() -> {
                attempts.incrementAndGet();
                throw new RuntimeException("持续失败");
            }, "测试操作", 3);
            
            fail("应该抛出异常");
        } catch (VoiceException e) {
            assertEquals(3, attempts.get());
            assertNotNull(e.getErrorType());
        }
    }
    
    @Test
    public void testExecuteWithRetryNonRetryableError() {
        // 测试不可重试的错误
        AtomicInteger attempts = new AtomicInteger(0);
        
        try {
            errorHandler.executeWithRetry(() -> {
                attempts.incrementAndGet();
                throw VoiceErrorHandler.permissionDenied("权限被拒绝");
            }, "测试操作");
            
            fail("应该抛出异常");
        } catch (VoiceException e) {
            assertEquals(1, attempts.get()); // 不应该重试
            assertEquals(ErrorType.PERMISSION_DENIED, e.getErrorType());
        }
    }
    
    @Test
    public void testVoiceExceptionContext() {
        // 测试异常上下文
        VoiceException exception = new VoiceException(ErrorType.STORAGE_ERROR, "存储失败")
            .addContext("userId", 123L)
            .addContext("fileSize", 1024);
        
        assertEquals(ErrorType.STORAGE_ERROR, exception.getErrorType());
        assertEquals(123L, exception.getContext().get("userId"));
        assertEquals(1024, exception.getContext().get("fileSize"));
    }
    
    @Test
    public void testErrorTypeProperties() {
        // 测试错误类型属性
        ErrorType permissionError = ErrorType.PERMISSION_DENIED;
        assertFalse(permissionError.isRetryable());
        assertEquals(403, permissionError.getHttpStatus());
        assertEquals("PERMISSION_DENIED", permissionError.getCode());
        
        ErrorType serviceError = ErrorType.STT_SERVICE_ERROR;
        assertTrue(serviceError.isRetryable());
        assertEquals(502, serviceError.getHttpStatus());
    }
    
    @Test
    public void testHandleUnknownError() {
        // 测试未知错误
        Exception unknownError = new Exception("未知错误");
        Map<String, Object> response = errorHandler.handleError(unknownError, "未知操作");
        
        assertFalse((Boolean) response.get("success"));
        assertEquals("INTERNAL_ERROR", response.get("errorCode"));
        assertEquals(500, response.get("httpStatus"));
    }
    
    @Test
    public void testErrorResponseStructure() {
        // 测试错误响应结构
        Exception error = new Exception("测试错误");
        Map<String, Object> response = errorHandler.handleError(error, "测试");
        
        // 验证必需字段
        assertTrue(response.containsKey("success"));
        assertTrue(response.containsKey("errorCode"));
        assertTrue(response.containsKey("errorMessage"));
        assertTrue(response.containsKey("httpStatus"));
        assertTrue(response.containsKey("retryable"));
        assertTrue(response.containsKey("timestamp"));
        assertTrue(response.containsKey("userMessage"));
        assertTrue(response.containsKey("suggestedAction"));
        
        assertFalse((Boolean) response.get("success"));
    }
    
    @Test
    public void testStaticErrorCreators() {
        // 测试静态错误创建方法
        VoiceException permissionError = VoiceErrorHandler.permissionDenied("测试");
        assertEquals(ErrorType.PERMISSION_DENIED, permissionError.getErrorType());
        
        VoiceException fileSizeError = VoiceErrorHandler.fileTooLarge(1000, 500);
        assertEquals(ErrorType.FILE_TOO_LARGE, fileSizeError.getErrorType());
        assertEquals(1000L, fileSizeError.getContext().get("fileSize"));
        
        VoiceException formatError = VoiceErrorHandler.unsupportedFormat("avi");
        assertEquals(ErrorType.UNSUPPORTED_FORMAT, formatError.getErrorType());
        assertEquals("avi", formatError.getContext().get("format"));
        
        VoiceException sttError = VoiceErrorHandler.serviceError("STT", "失败");
        assertEquals(ErrorType.STT_SERVICE_ERROR, sttError.getErrorType());
        
        VoiceException ttsError = VoiceErrorHandler.serviceError("TTS", "失败");
        assertEquals(ErrorType.TTS_SERVICE_ERROR, ttsError.getErrorType());
    }
    
    @Test
    public void testRetryBackoff() {
        // 测试重试退避时间
        AtomicInteger attempts = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();
        
        try {
            errorHandler.executeWithRetry(() -> {
                attempts.incrementAndGet();
                throw new RuntimeException("Storage error"); // 可重试错误
            }, "测试退避", 3);
        } catch (VoiceException e) {
            // 预期失败
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        // 验证重试次数
        assertEquals(3, attempts.get());
        
        // 验证总时间（1s + 2s = 3s，允许一些误差）
        assertTrue("退避时间应该至少3秒", duration >= 3000);
        assertTrue("退避时间不应该超过5秒", duration < 5000);
    }
}
