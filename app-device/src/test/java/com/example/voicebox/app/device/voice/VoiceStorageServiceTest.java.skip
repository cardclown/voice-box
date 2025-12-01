package com.example.voicebox.app.device.voice;

import com.example.voicebox.app.device.service.voice.VoiceStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 语音存储服务测试
 * 
 * 属性 1: 音频文件存储完整性
 * 属性 4: 音频格式验证
 * 属性 6: 语音文件清理
 * 
 * @author VoiceBox Team
 * @since 1.5
 */
@SpringBootTest
public class VoiceStorageServiceTest {
    
    @Autowired
    private VoiceStorageService storageService;
    
    /**
     * 属性 1: 音频文件存储完整性
     * 验证需求: 3.1, 3.2
     */
    @Test
    public void testAudioStorageRoundTrip() throws IOException {
        // 创建测试音频数据
        byte[] originalAudio = "TEST_AUDIO_DATA".getBytes();
        
        // 保存
        String fileId = storageService.saveAudioData(originalAudio, 1L);
        
        // 读取
        byte[] retrievedAudio = storageService.loadAudioData(fileId);
        
        // 验证完全相同
        assertThat(retrievedAudio).isEqualTo(originalAudio);
        
        // 清理
        storageService.deleteAudioFile(fileId);
    }
    
    /**
     * 属性 4: 音频格式验证
     * 验证需求: 4.5
     */
    @Test
    public void testInvalidFileFormat() {
        // 创建非音频文件
        MockMultipartFile invalidFile = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "test content".getBytes()
        );
        
        // 验证抛出异常
        assertThatThrownBy(() -> storageService.saveAudioFile(invalidFile, 1L))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("不支持的文件格式");
    }
    
    /**
     * 测试文件大小限制
     */
    @Test
    public void testFileSizeLimit() {
        // 创建超大文件（11MB）
        byte[] largeData = new byte[11 * 1024 * 1024];
        MockMultipartFile largeFile = new MockMultipartFile(
            "file",
            "large.mp3",
            "audio/mpeg",
            largeData
        );
        
        // 验证抛出异常
        assertThatThrownBy(() -> storageService.saveAudioFile(largeFile, 1L))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("文件大小超过限制");
    }
    
    /**
     * 测试文件存在性检查
     */
    @Test
    public void testFileExists() throws IOException {
        // 保存文件
        byte[] audioData = "TEST".getBytes();
        String fileId = storageService.saveAudioData(audioData, 1L);
        
        // 验证存在
        assertThat(storageService.fileExists(fileId)).isTrue();
        
        // 删除文件
        storageService.deleteAudioFile(fileId);
        
        // 验证不存在
        assertThat(storageService.fileExists(fileId)).isFalse();
    }
}
