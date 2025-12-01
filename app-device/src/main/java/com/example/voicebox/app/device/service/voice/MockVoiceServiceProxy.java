package com.example.voicebox.app.device.service.voice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 模拟语音服务代理实现
 * 用于开发和测试，不调用真实的语音服务
 * 
 * @author VoiceBox Team
 * @since 1.5
 */
@Service
public class MockVoiceServiceProxy {
    
    private static final Logger logger = LoggerFactory.getLogger(MockVoiceServiceProxy.class);
    
    public String speechToText(InputStream audioStream, String language) throws Exception {
        logger.info("模拟STT服务 - 语言: {}", language);
        
        // 模拟处理延迟
        Thread.sleep(500);
        
        // 返回模拟的识别结果
        return "这是模拟的语音识别结果，实际应该调用真实的STT服务";
    }
    
    public byte[] textToSpeech(String text, String language, VoiceProfile voiceProfile) throws Exception {
        logger.info("模拟TTS服务 - 文本长度: {}, 语言: {}, 音色: {}", 
            text.length(), language, voiceProfile.getVoiceName());
        
        // 模拟处理延迟
        Thread.sleep(300);
        
        // 返回模拟的音频数据（实际应该是音频字节）
        String mockAudio = "MOCK_AUDIO_DATA_" + text.substring(0, Math.min(10, text.length()));
        return mockAudio.getBytes(StandardCharsets.UTF_8);
    }
    
    public boolean checkHealth() {
        logger.debug("检查语音服务健康状态");
        return true;
    }
}
