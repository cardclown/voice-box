package com.example.voicebox.app.device.voice;

import com.example.voicebox.app.device.controller.VoiceController.VoiceSynthesisResponse;
import com.example.voicebox.app.device.domain.UserProfile;
import com.example.voicebox.app.device.domain.VoiceMessage;
import com.example.voicebox.app.device.repository.UserProfileRepository;
import com.example.voicebox.app.device.repository.VoiceMessageRepository;
import com.example.voicebox.app.device.service.voice.VoiceOutputService;
import com.example.voicebox.app.device.service.voice.VoiceServiceProxy;
import com.example.voicebox.app.device.service.voice.VoiceStorageService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * VoiceOutputService 单元测试
 * 
 * 测试任务 5.2: 语音合成
 * - 测试TTS服务调用
 * - 测试音色选择逻辑
 * - 测试语速调整
 */
@RunWith(MockitoJUnitRunner.class)
public class VoiceOutputServiceTest {

    @Mock
    private VoiceServiceProxy voiceServiceProxy;

    @Mock
    private VoiceStorageService storageService;

    @Mock
    private VoiceMessageRepository messageRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private VoiceOutputService voiceOutputService;

    private byte[] mockAudioData;

    @Before
    public void setUp() {
        // 创建模拟音频数据
        mockAudioData = new byte[1024];
    }

    /**
     * 测试TTS服务调用 - 正常流程
     */
    @Test
    public void testSynthesizeVoice_Success() throws Exception {
        String text = "你好，这是测试文本";
        Long userId = 1L;
        String language = "zh-CN";
        String voiceName = "zh_female_qingxin";

        // Mock依赖
        when(voiceServiceProxy.textToSpeech(anyString(), anyString(), anyString()))
                .thenReturn(mockAudioData);
        when(storageService.saveAudioData(any(), anyLong(), anyString()))
                .thenReturn("test-file-id");
        when(storageService.getFilePath(anyString())).thenReturn("/path/to/file");
        when(messageRepository.save(any())).thenReturn(new VoiceMessage());

        // 执行测试
        VoiceSynthesisResponse response = voiceOutputService.synthesizeVoice(
                text, userId, language, voiceName
        );

        // 验证结果
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("test-file-id", response.getFileId());
        assertTrue(response.getDuration() > 0);

        // 验证调用
        verify(voiceServiceProxy).textToSpeech(text, language, voiceName);
        verify(storageService).saveAudioData(mockAudioData, userId, "mp3");
        verify(messageRepository).save(any(VoiceMessage.class));
    }

    /**
     * 测试音色选择逻辑 - 使用用户偏好
     */
    @Test
    public void testSynthesizeVoice_UserPreferredVoice() throws Exception {
        String text = "测试文本";
        Long userId = 1L;
        String language = "zh-CN";

        // 创建用户画像，设置偏好音色
        UserProfile userProfile = new UserProfile();
        userProfile.setUserId(userId);

        when(userProfileRepository.findByUserId(userId))
                .thenReturn(userProfile);
        when(voiceServiceProxy.textToSpeech(anyString(), anyString(), anyString()))
                .thenReturn(mockAudioData);
        when(storageService.saveAudioData(any(), anyLong(), anyString()))
                .thenReturn("test-file-id");
        when(storageService.getFilePath(anyString())).thenReturn("/path/to/file");
        when(messageRepository.save(any())).thenReturn(new VoiceMessage());

        // 执行测试 - 不指定音色
        VoiceSynthesisResponse response = voiceOutputService.synthesizeVoice(
                text, userId, language, null
        );

        // 验证结果
        assertNotNull(response);
        assertTrue(response.isSuccess());

        // 验证调用了用户画像
        verify(userProfileRepository).findByUserId(userId);
    }

    /**
     * 测试音色选择逻辑 - 默认音色
     */
    @Test
    public void testSynthesizeVoice_DefaultVoice() throws Exception {
        String text = "测试文本";
        Long userId = 999L; // 不存在的用户
        String language = "zh-CN";

        when(userProfileRepository.findByUserId(userId))
                .thenReturn(null);
        when(voiceServiceProxy.textToSpeech(anyString(), anyString(), anyString()))
                .thenReturn(mockAudioData);
        when(storageService.saveAudioData(any(), anyLong(), anyString()))
                .thenReturn("test-file-id");
        when(storageService.getFilePath(anyString())).thenReturn("/path/to/file");
        when(messageRepository.save(any())).thenReturn(new VoiceMessage());

        // 执行测试
        VoiceSynthesisResponse response = voiceOutputService.synthesizeVoice(
                text, userId, language, null
        );

        // 验证结果
        assertNotNull(response);
        assertTrue(response.isSuccess());
    }

    /**
     * 测试不同语言的语音合成
     */
    @Test
    public void testSynthesizeVoice_DifferentLanguages() throws Exception {
        String text = "Hello, this is a test";
        Long userId = 1L;
        String language = "en-US";
        String voiceName = "en_female_sara";

        when(voiceServiceProxy.textToSpeech(anyString(), anyString(), anyString()))
                .thenReturn(mockAudioData);
        when(storageService.saveAudioData(any(), anyLong(), anyString()))
                .thenReturn("test-file-id");
        when(storageService.getFilePath(anyString())).thenReturn("/path/to/file");
        when(messageRepository.save(any())).thenReturn(new VoiceMessage());

        VoiceSynthesisResponse response = voiceOutputService.synthesizeVoice(
                text, userId, language, voiceName
        );

        assertNotNull(response);
        assertTrue(response.isSuccess());
        verify(voiceServiceProxy).textToSpeech(text, language, voiceName);
    }

    /**
     * 测试长文本的语音合成
     */
    @Test
    public void testSynthesizeVoice_LongText() throws Exception {
        // 创建长文本（超过100字）
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longText.append("这是一段很长的测试文本。");
        }

        Long userId = 1L;
        String language = "zh-CN";
        String voiceName = "zh_female_qingxin";

        when(voiceServiceProxy.textToSpeech(anyString(), anyString(), anyString()))
                .thenReturn(mockAudioData);
        when(storageService.saveAudioData(any(), anyLong(), anyString()))
                .thenReturn("test-file-id");
        when(storageService.getFilePath(anyString())).thenReturn("/path/to/file");
        when(messageRepository.save(any())).thenReturn(new VoiceMessage());

        VoiceSynthesisResponse response = voiceOutputService.synthesizeVoice(
                longText.toString(), userId, language, voiceName
        );

        assertNotNull(response);
        assertTrue(response.isSuccess());
        // 长文本应该有更长的时长
        assertTrue(response.getDuration() > 10);
    }

    /**
     * 测试TTS服务失败的情况
     */
    @Test(expected = Exception.class)
    public void testSynthesizeVoice_TTSFailure() throws Exception {
        String text = "测试文本";
        Long userId = 1L;
        String language = "zh-CN";
        String voiceName = "zh_female_qingxin";

        // Mock TTS服务失败
        when(voiceServiceProxy.textToSpeech(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("TTS服务不可用"));

        // 执行测试 - 应该抛出异常
        voiceOutputService.synthesizeVoice(text, userId, language, voiceName);
    }

    /**
     * 测试空文本
     */
    @Test
    public void testSynthesizeVoice_EmptyText() throws Exception {
        String text = "";
        Long userId = 1L;
        String language = "zh-CN";
        String voiceName = "zh_female_qingxin";

        when(voiceServiceProxy.textToSpeech(anyString(), anyString(), anyString()))
                .thenReturn(new byte[0]);
        when(storageService.saveAudioData(any(), anyLong(), anyString()))
                .thenReturn("test-file-id");
        when(storageService.getFilePath(anyString())).thenReturn("/path/to/file");
        when(messageRepository.save(any())).thenReturn(new VoiceMessage());

        VoiceSynthesisResponse response = voiceOutputService.synthesizeVoice(
                text, userId, language, voiceName
        );

        assertNotNull(response);
        // 空文本时长应该至少为1秒（根据estimateDuration的实现）
        assertTrue(response.getDuration() >= 1);
    }

    /**
     * 测试完整的语音合成流程
     */
    @Test
    public void testSynthesizeVoice_CompleteFlow() throws Exception {
        String text = "完整流程测试文本";
        Long userId = 1L;
        String language = "zh-CN";
        String voiceName = "zh_female_qingxin";
        String expectedFileId = "complete-test-file-id";

        when(voiceServiceProxy.textToSpeech(text, language, voiceName))
                .thenReturn(mockAudioData);
        when(storageService.saveAudioData(mockAudioData, userId, "mp3"))
                .thenReturn(expectedFileId);
        when(storageService.getFilePath(expectedFileId))
                .thenReturn("/path/to/" + expectedFileId);
        when(messageRepository.save(any())).thenAnswer(invocation -> {
            VoiceMessage msg = invocation.getArgument(0);
            msg.setId(1L);
            return msg;
        });

        VoiceSynthesisResponse response = voiceOutputService.synthesizeVoice(
                text, userId, language, voiceName
        );

        // 验证结果
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(expectedFileId, response.getFileId());
        assertTrue(response.getDuration() > 0);

        // 验证所有步骤都被调用
        verify(voiceServiceProxy).textToSpeech(text, language, voiceName);
        verify(storageService).saveAudioData(mockAudioData, userId, "mp3");
        verify(storageService).getFilePath(expectedFileId);
        verify(messageRepository).save(argThat(msg -> 
            msg.getUserId().equals(userId) &&
            msg.getFileId().equals(expectedFileId) &&
            msg.getRecognizedText().equals(text) &&
            !msg.getIsInput()
        ));
    }
}
