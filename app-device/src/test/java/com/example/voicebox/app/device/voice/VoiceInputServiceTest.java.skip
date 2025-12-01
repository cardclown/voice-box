package com.example.voicebox.app.device.voice;

import com.example.voicebox.app.device.controller.VoiceController.VoiceUploadResponse;
import com.example.voicebox.app.device.domain.VoiceMessage;
import com.example.voicebox.app.device.repository.VoiceMessageRepository;
import com.example.voicebox.app.device.service.voice.VoiceInputService;
import com.example.voicebox.app.device.service.voice.VoiceServiceProxy;
import com.example.voicebox.app.device.service.voice.VoiceStorageService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * VoiceInputService 单元测试
 * 
 * 测试任务 4.2: 音频文件验证
 * - 测试文件大小限制（10MB）
 * - 测试文件格式验证
 * - 测试音频时长计算
 */
@RunWith(MockitoJUnitRunner.class)
public class VoiceInputServiceTest {

    @Mock
    private VoiceServiceProxy voiceServiceProxy;

    @Mock
    private VoiceStorageService storageService;

    @Mock
    private VoiceMessageRepository messageRepository;

    @InjectMocks
    private VoiceInputService voiceInputService;

    @Before
    public void setUp() {
        // 设置最大文件大小为10MB
        ReflectionTestUtils.setField(voiceInputService, "maxFileSize", 10485760L);
    }

    /**
     * 测试文件大小限制 - 正常大小
     */
    @Test
    public void testValidateAudioFile_NormalSize() throws Exception {
        // 创建5MB的测试文件
        byte[] content = new byte[5 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "audio",
                "test.wav",
                "audio/wav",
                content
        );

        // Mock依赖
        when(storageService.saveAudioFile(any(), anyLong())).thenReturn("test-file-id");
        when(storageService.getFilePath(anyString())).thenReturn("/path/to/file");
        when(voiceServiceProxy.speechToText(any(), anyString())).thenReturn("测试文本");
        when(messageRepository.save(any())).thenReturn(new VoiceMessage());

        // 执行测试
        VoiceUploadResponse response = voiceInputService.processVoiceInput(
                file, 1L, 1L, "zh-CN"
        );

        // 验证结果
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("测试文本", response.getRecognizedText());
    }

    /**
     * 测试文件大小限制 - 超过限制
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValidateAudioFile_ExceedsLimit() throws Exception {
        // 创建15MB的测试文件（超过10MB限制）
        byte[] content = new byte[15 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "audio",
                "test.wav",
                "audio/wav",
                content
        );

        // 执行测试 - 应该抛出异常
        voiceInputService.processVoiceInput(file, 1L, 1L, "zh-CN");
    }

    /**
     * 测试空文件
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValidateAudioFile_EmptyFile() throws Exception {
        // 创建空文件
        MockMultipartFile file = new MockMultipartFile(
                "audio",
                "test.wav",
                "audio/wav",
                new byte[0]
        );

        // 执行测试 - 应该抛出异常
        voiceInputService.processVoiceInput(file, 1L, 1L, "zh-CN");
    }

    /**
     * 测试文件格式验证 - WAV格式
     */
    @Test
    public void testValidateAudioFile_WavFormat() throws Exception {
        byte[] content = new byte[1024];
        MockMultipartFile file = new MockMultipartFile(
                "audio",
                "test.wav",
                "audio/wav",
                content
        );

        when(storageService.saveAudioFile(any(), anyLong())).thenReturn("test-file-id");
        when(storageService.getFilePath(anyString())).thenReturn("/path/to/file");
        when(voiceServiceProxy.speechToText(any(), anyString())).thenReturn("测试");
        when(messageRepository.save(any())).thenReturn(new VoiceMessage());

        VoiceUploadResponse response = voiceInputService.processVoiceInput(
                file, 1L, 1L, "zh-CN"
        );

        assertNotNull(response);
        assertTrue(response.isSuccess());
    }

    /**
     * 测试文件格式验证 - MP3格式
     */
    @Test
    public void testValidateAudioFile_Mp3Format() throws Exception {
        byte[] content = new byte[1024];
        MockMultipartFile file = new MockMultipartFile(
                "audio",
                "test.mp3",
                "audio/mpeg",
                content
        );

        when(storageService.saveAudioFile(any(), anyLong())).thenReturn("test-file-id");
        when(storageService.getFilePath(anyString())).thenReturn("/path/to/file");
        when(voiceServiceProxy.speechToText(any(), anyString())).thenReturn("测试");
        when(messageRepository.save(any())).thenReturn(new VoiceMessage());

        VoiceUploadResponse response = voiceInputService.processVoiceInput(
                file, 1L, 1L, "zh-CN"
        );

        assertNotNull(response);
        assertTrue(response.isSuccess());
    }

    /**
     * 测试语音识别失败的情况
     */
    @Test
    public void testProcessVoiceInput_RecognitionFailure() throws Exception {
        byte[] content = new byte[1024];
        MockMultipartFile file = new MockMultipartFile(
                "audio",
                "test.wav",
                "audio/wav",
                content
        );

        when(storageService.saveAudioFile(any(), anyLong())).thenReturn("test-file-id");
        when(storageService.getFilePath(anyString())).thenReturn("/path/to/file");
        when(voiceServiceProxy.speechToText(any(), anyString()))
                .thenThrow(new RuntimeException("识别失败"));
        when(messageRepository.save(any())).thenReturn(new VoiceMessage());

        VoiceUploadResponse response = voiceInputService.processVoiceInput(
                file, 1L, 1L, "zh-CN"
        );

        // 即使识别失败，也应该返回成功（文件已保存）
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("[识别失败]", response.getRecognizedText());
    }

    /**
     * 测试音频时长计算
     */
    @Test
    public void testProcessVoiceInput_DurationCalculation() throws Exception {
        byte[] content = new byte[1024 * 100]; // 100KB
        MockMultipartFile file = new MockMultipartFile(
                "audio",
                "test.wav",
                "audio/wav",
                content
        );

        when(storageService.saveAudioFile(any(), anyLong())).thenReturn("test-file-id");
        when(storageService.getFilePath(anyString())).thenReturn("/path/to/file");
        when(voiceServiceProxy.speechToText(any(), anyString())).thenReturn("测试");
        when(messageRepository.save(any())).thenReturn(new VoiceMessage());

        VoiceUploadResponse response = voiceInputService.processVoiceInput(
                file, 1L, 1L, "zh-CN"
        );

        assertNotNull(response);
        assertTrue(response.getDuration() > 0);
    }

    /**
     * 测试完整的语音输入流程
     */
    @Test
    public void testProcessVoiceInput_CompleteFlow() throws Exception {
        byte[] content = new byte[1024];
        MockMultipartFile file = new MockMultipartFile(
                "audio",
                "test.wav",
                "audio/wav",
                content
        );

        String expectedFileId = "test-file-id-123";
        String expectedText = "这是识别的文本";

        when(storageService.saveAudioFile(any(), anyLong())).thenReturn(expectedFileId);
        when(storageService.getFilePath(anyString())).thenReturn("/path/to/file");
        when(voiceServiceProxy.speechToText(any(), anyString())).thenReturn(expectedText);
        when(messageRepository.save(any())).thenAnswer(invocation -> {
            VoiceMessage msg = invocation.getArgument(0);
            msg.setId(1L);
            return msg;
        });

        VoiceUploadResponse response = voiceInputService.processVoiceInput(
                file, 1L, 1L, "zh-CN"
        );

        // 验证结果
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(expectedFileId, response.getFileId());
        assertEquals(expectedText, response.getRecognizedText());
        assertNotNull(response.getConfidence());
        assertTrue(response.getDuration() >= 0);

        // 验证调用
        verify(storageService).saveAudioFile(any(), eq(1L));
        verify(voiceServiceProxy).speechToText(any(), eq("zh-CN"));
        verify(messageRepository).save(any(VoiceMessage.class));
    }
}
