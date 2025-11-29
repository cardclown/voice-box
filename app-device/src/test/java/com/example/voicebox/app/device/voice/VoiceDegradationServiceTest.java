package com.example.voicebox.app.device.voice;

import com.example.voicebox.app.device.service.voice.VoiceDegradationService;
import com.example.voicebox.app.device.service.voice.VoiceProfile;
import com.example.voicebox.app.device.service.voice.VoiceServiceProxy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 语音服务降级测试
 * 
 * 属性 8: 服务降级切换
 * 属性 9: 重试指数退避
 * 
 * @author VoiceBox Team
 * @since 1.5
 */
@SpringBootTest
public class VoiceDegradationServiceTest {
    
    @Autowired
    private VoiceDegradationService degradationService;
    
    @MockBean
    private VoiceServiceProxy voiceServiceProxy;
    
    /**
     * 属性 8: 服务降级切换
     * 验证需求: 10.3, 10.6
     * 
     * 测试当主服务失败时，系统能够返回降级提示
     */
    @Test
    public void testServiceFailoverForSTT() throws Exception {
        // 模拟主服务失败
        when(voiceServiceProxy.speechToText(any(InputStream.class), anyString()))
            .thenThrow(new RuntimeException("服务不可用"));
        
        // 调用降级服务
        InputStream audioStream = new ByteArrayInputStream("test".getBytes());
        String result = degradationService.degradedSpeechToText(audioStream, "zh-CN");
        
        // 验证返回降级提示
        assertThat(result).contains("语音识别暂时不可用");
        
        // 验证尝试调用了主服务
        verify(voiceServiceProxy, times(1)).speechToText(any(), anyString());
    }
    
    /**
     * 属性 8: 服务降级切换 - TTS
     * 验证需求: 10.3, 10.6
     */
    @Test
    public void testServiceFailoverForTTS() throws Exception {
        // 模拟主服务失败
        when(voiceServiceProxy.textToSpeech(anyString(), anyString(), any(VoiceProfile.class)))
            .thenThrow(new RuntimeException("服务不可用"));
        
        // 调用降级服务
        byte[] result = degradationService.degradedTextToSpeech("测试", "zh-CN", VoiceProfile.getDefault());
        
        // 验证返回null（降级为仅显示文本）
        assertThat(result).isNull();
        
        // 验证尝试调用了主服务
        verify(voiceServiceProxy, times(1)).textToSpeech(anyString(), anyString(), any());
    }
    
    /**
     * 属性 9: 重试指数退避
     * 验证需求: 1.8, 10.5
     * 
     * 测试重试机制使用指数退避策略
     */
    @Test
    public void testExponentialBackoffRetry() throws Exception {
        // 模拟前2次失败，第3次成功
        when(voiceServiceProxy.speechToText(any(InputStream.class), anyString()))
            .thenThrow(new RuntimeException("失败1"))
            .thenThrow(new RuntimeException("失败2"))
            .thenReturn("成功");
        
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        
        // 调用带重试的服务
        InputStream audioStream = new ByteArrayInputStream("test".getBytes());
        String result = degradationService.speechToTextWithRetry(audioStream, "zh-CN", 3);
        
        // 记录结束时间
        long duration = System.currentTimeMillis() - startTime;
        
        // 验证最终成功
        assertThat(result).isEqualTo("成功");
        
        // 验证调用了3次
        verify(voiceServiceProxy, times(3)).speechToText(any(), anyString());
        
        // 验证使用了指数退避（1秒 + 2秒 = 3秒最小延迟）
        assertThat(duration).isGreaterThanOrEqualTo(3000);
    }
    
    /**
     * 测试重试全部失败的情况
     */
    @Test
    public void testRetryAllFailed() throws Exception {
        // 模拟所有重试都失败
        when(voiceServiceProxy.speechToText(any(InputStream.class), anyString()))
            .thenThrow(new RuntimeException("服务失败"));
        
        // 调用带重试的服务
        InputStream audioStream = new ByteArrayInputStream("test".getBytes());
        String result = degradationService.speechToTextWithRetry(audioStream, "zh-CN", 3);
        
        // 验证返回失败提示
        assertThat(result).contains("语音识别失败");
        
        // 验证重试了3次
        verify(voiceServiceProxy, times(3)).speechToText(any(), anyString());
    }
    
    /**
     * 测试主服务成功的情况（无需降级）
     */
    @Test
    public void testNoFailoverWhenServiceWorks() throws Exception {
        // 模拟主服务成功
        when(voiceServiceProxy.speechToText(any(InputStream.class), anyString()))
            .thenReturn("识别成功");
        
        // 调用降级服务
        InputStream audioStream = new ByteArrayInputStream("test".getBytes());
        String result = degradationService.degradedSpeechToText(audioStream, "zh-CN");
        
        // 验证返回正常结果
        assertThat(result).isEqualTo("识别成功");
        
        // 验证只调用了一次主服务
        verify(voiceServiceProxy, times(1)).speechToText(any(), anyString());
    }
}
