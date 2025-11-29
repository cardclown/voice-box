package com.example.voicebox.app.device.service.voice;

import com.example.voicebox.app.device.controller.VoiceController.VoiceSynthesisResponse;
import com.example.voicebox.app.device.domain.UserProfile;
import com.example.voicebox.app.device.domain.VoiceMessage;
import com.example.voicebox.app.device.repository.UserProfileRepository;
import com.example.voicebox.app.device.repository.VoiceMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 语音输出服务
 */
@Slf4j
@Service
public class VoiceOutputService {

    @Autowired
    private VoiceServiceProxy voiceServiceProxy;

    @Autowired
    private VoiceStorageService storageService;

    @Autowired
    private VoiceMessageRepository messageRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    /**
     * 合成语音
     */
    public VoiceSynthesisResponse synthesizeVoice(
            String text,
            Long userId,
            String language,
            String voiceName) throws Exception {

        log.info("开始语音合成 - userId: {}, text: {}, language: {}, voice: {}", 
                userId, text, language, voiceName);

        // 1. 获取用户偏好的语音配置
        if (voiceName == null || voiceName.isEmpty()) {
            voiceName = getUserPreferredVoice(userId);
        }

        // 2. 调用TTS服务
        byte[] audioData = voiceServiceProxy.textToSpeech(text, language, voiceName);
        log.info("语音合成成功 - size: {} bytes", audioData.length);

        // 3. 保存音频文件
        String fileId = storageService.saveAudioData(audioData, userId, "mp3");
        log.info("音频文件已保存 - fileId: {}", fileId);

        // 4. 估算时长
        int duration = estimateDuration(text);

        // 5. 保存语音消息记录
        VoiceMessage voiceMessage = new VoiceMessage();
        voiceMessage.setUserId(userId);
        voiceMessage.setFileId(fileId);
        voiceMessage.setFilePath(storageService.getFilePath(fileId));
        voiceMessage.setFileSize((long) audioData.length);
        voiceMessage.setDuration(duration);
        voiceMessage.setFormat("mp3");
        voiceMessage.setSampleRate(16000);
        voiceMessage.setRecognizedText(text);
        voiceMessage.setLanguage(language);
        voiceMessage.setIsInput(false); // AI输出
        voiceMessage.setCreatedAt(LocalDateTime.now());

        messageRepository.save(voiceMessage);
        log.info("语音消息记录已保存 - id: {}", voiceMessage.getId());

        // 6. 返回结果
        VoiceSynthesisResponse response = new VoiceSynthesisResponse();
        response.setSuccess(true);
        response.setFileId(fileId);
        response.setAudioUrl("/api/voice/audio/" + fileId);
        response.setDuration(duration);

        return response;
    }

    /**
     * 获取用户偏好的语音音色
     * 根据用户画像选择合适的音色
     */
    private String getUserPreferredVoice(Long userId) {
        try {
            UserProfile profile = userProfileRepository.findByUserId(userId);

            if (profile == null) {
                return getDefaultVoice();
            }

            // 根据用户性格特征选择语音音色
            BigDecimal extraversion = profile.getExtraversion();

            if (extraversion != null && extraversion.compareTo(BigDecimal.valueOf(0.6)) > 0) {
                // 外向性高 -> 活力音色
                log.info("用户外向性高，选择活力音色 - userId: {}, extraversion: {}", 
                        userId, extraversion);
                return "zh_female_huoli"; // 活力女声
            } else {
                // 外向性低 -> 温和音色
                log.info("用户外向性低，选择温和音色 - userId: {}, extraversion: {}", 
                        userId, extraversion);
                return "zh_female_qingxin"; // 清新女声
            }

        } catch (Exception e) {
            log.warn("获取用户语音偏好失败，使用默认音色 - userId: {}", userId, e);
            return getDefaultVoice();
        }
    }

    /**
     * 获取默认语音音色
     */
    private String getDefaultVoice() {
        return "zh_female_qingxin"; // 清新女声
    }

    /**
     * 估算语音时长（秒）
     * 基于文本长度和平均语速
     */
    private int estimateDuration(String text) {
        // 平均语速：每分钟约200个汉字
        int charsPerMinute = 200;
        int textLength = text.length();
        int durationSeconds = (int) Math.ceil((double) textLength / charsPerMinute * 60);
        
        log.debug("估算语音时长 - textLength: {}, duration: {} seconds", textLength, durationSeconds);
        return Math.max(1, durationSeconds); // 至少1秒
    }

    /**
     * 流式语音合成
     * 用于实时播放场景
     */
    public void streamSynthesizeVoice(
            String text,
            Long userId,
            String language,
            String voiceName,
            DoubaoVoiceService.AudioDataCallback callback) {

        log.info("开始流式语音合成 - userId: {}, text: {}", userId, text);

        // 获取用户偏好的语音配置
        if (voiceName == null || voiceName.isEmpty()) {
            voiceName = getUserPreferredVoice(userId);
        }

        // 调用流式TTS服务
        voiceServiceProxy.streamTextToSpeech(text, language, voiceName, callback);
    }
}
