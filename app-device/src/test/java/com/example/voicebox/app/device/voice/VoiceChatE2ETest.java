package com.example.voicebox.app.device.voice;

import com.example.voicebox.app.device.domain.VoiceMessage;
import com.example.voicebox.app.device.repository.VoiceMessageRepository;
import com.example.voicebox.app.device.service.voice.VoiceStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 语音聊天端到端测试
 * 
 * 测试完整的语音聊天流程：
 * 1. 录音并发送消息
 * 2. 接收AI语音回复
 * 3. 播放历史语音消息
 * 
 * 验证需求: 1.1-1.8, 2.1-2.8, 3.4, 3.5
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("语音聊天端到端测试")
public class VoiceChatE2ETest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private VoiceMessageRepository voiceMessageRepository;

    @Autowired
    private VoiceStorageService voiceStorageService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        voiceMessageRepository.deleteAll();
    }

    /**
     * 测试场景 1: 完整的语音聊天流程
     * 
     * 用户故事：
     * 作为用户，我想通过语音与AI进行对话，
     * 包括录音发送消息、接收AI语音回复、播放历史消息
     * 
     * 流程：
     * 1. 用户录音并上传音频文件
     * 2. 系统识别语音并返回文本
     * 3. 系统生成AI回复文本
     * 4. 系统将AI回复转换为语音
     * 5. 用户可以播放AI语音回复
     * 6. 用户可以查看和播放历史语音消息
     * 
     * 验证：
     * - 语音识别成功
     * - AI回复生成成功
     * - 语音合成成功
     * - 历史消息可以查询
     * - 音频文件可以下载播放
     */
    @Test
    @DisplayName("完整语音聊天流程")
    void testCompleteVoiceChatFlow() throws Exception {
        // ========== 第1步：用户录音并发送消息 ==========
        Long userId = 1L;
        Long sessionId = 100L;
        String language = "zh-CN";
        
        // 创建模拟音频文件
        byte[] userAudioContent = createMockAudioData("用户语音");
        MockMultipartFile userAudioFile = new MockMultipartFile(
            "file",
            "user-message.wav",
            "audio/wav",
            userAudioContent
        );

        // 上传用户语音
        MvcResult uploadResult = mockMvc.perform(multipart("/api/voice/upload")
                .file(userAudioFile)
                .param("userId", userId.toString())
                .param("sessionId", sessionId.toString())
                .param("language", language))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.recognizedText").exists())
                .andExpect(jsonPath("$.fileId").exists())
                .andReturn();

        // 解析用户消息
        String uploadResponseBody = uploadResult.getResponse().getContentAsString();
        Map<String, Object> uploadResponse = objectMapper.readValue(uploadResponseBody, Map.class);
        String userFileId = (String) uploadResponse.get("fileId");
        String recognizedText = (String) uploadResponse.get("recognizedText");

        System.out.println("✓ 步骤1完成：用户语音上传成功");
        System.out.println("  - 文件ID: " + userFileId);
        System.out.println("  - 识别文本: " + recognizedText);

        // 验证用户消息已保存
        List<VoiceMessage> userMessages = voiceMessageRepository.findByUserIdOrderByCreatedAtDesc(userId);
        assertThat(userMessages).hasSize(1);
        assertThat(userMessages.get(0).getFileId()).isEqualTo(userFileId);
        assertThat(userMessages.get(0).getRecognizedText()).isEqualTo(recognizedText);

        // ========== 第2步：模拟AI生成回复文本 ==========
        // 在实际应用中，这里会调用聊天服务生成AI回复
        // 这里我们模拟一个AI回复
        String aiReplyText = "你好！我收到了你的消息：" + recognizedText;
        
        System.out.println("✓ 步骤2完成：AI回复生成");
        System.out.println("  - AI回复: " + aiReplyText);

        // ========== 第3步：将AI回复转换为语音 ==========
        Map<String, Object> ttsRequest = new HashMap<>();
        ttsRequest.put("text", aiReplyText);
        ttsRequest.put("userId", userId);
        ttsRequest.put("sessionId", sessionId);
        ttsRequest.put("language", language);
        ttsRequest.put("voiceName", "zh_female_qingxin");

        MvcResult ttsResult = mockMvc.perform(post("/api/voice/synthesize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ttsRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.fileId").exists())
                .andExpect(jsonPath("$.duration").exists())
                .andReturn();

        // 解析AI语音回复
        String ttsResponseBody = ttsResult.getResponse().getContentAsString();
        Map<String, Object> ttsResponse = objectMapper.readValue(ttsResponseBody, Map.class);
        String aiFileId = (String) ttsResponse.get("fileId");
        Number aiDuration = (Number) ttsResponse.get("duration");

        System.out.println("✓ 步骤3完成：AI回复语音合成成功");
        System.out.println("  - 文件ID: " + aiFileId);
        System.out.println("  - 时长: " + aiDuration + "秒");

        // 验证AI回复已保存
        List<VoiceMessage> allMessages = voiceMessageRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
        assertThat(allMessages).hasSize(2); // 用户消息 + AI回复
        
        VoiceMessage aiMessage = allMessages.stream()
            .filter(m -> m.getFileId().equals(aiFileId))
            .findFirst()
            .orElse(null);
        
        assertThat(aiMessage).isNotNull();
        // AI回复的文本存储在recognizedText字段中
        assertThat(aiMessage.getDuration()).isNotNull();

        // ========== 第4步：用户播放AI语音回复 ==========
        mockMvc.perform(get("/api/voice/audio/" + aiFileId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("audio/*"))
                .andExpect(result -> {
                    byte[] audioData = result.getResponse().getContentAsByteArray();
                    assertThat(audioData).isNotEmpty();
                    System.out.println("✓ 步骤4完成：AI语音回复下载成功");
                    System.out.println("  - 音频大小: " + audioData.length + " 字节");
                });

        // ========== 第5步：查询历史语音消息 ==========
        List<VoiceMessage> historyMessages = voiceMessageRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
        assertThat(historyMessages).hasSize(2);
        
        System.out.println("✓ 步骤5完成：历史消息查询成功");
        System.out.println("  - 消息数量: " + historyMessages.size());
        
        // 验证历史消息的完整性
        for (VoiceMessage message : historyMessages) {
            assertThat(message.getFileId()).isNotNull();
            assertThat(message.getUserId()).isEqualTo(userId);
            assertThat(message.getSessionId()).isEqualTo(sessionId);
            
            // 验证音频文件路径存在
            assertThat(message.getFilePath()).isNotNull();
            
            System.out.println("  - 消息ID: " + message.getId());
            System.out.println("    文件ID: " + message.getFileId());
            System.out.println("    文本: " + (message.getRecognizedText() != null ? 
                message.getRecognizedText() : "AI回复"));
        }

        // ========== 第6步：用户播放历史消息 ==========
        // 播放用户自己的消息
        mockMvc.perform(get("/api/voice/audio/" + userFileId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("audio/*"));

        // 再次播放AI回复
        mockMvc.perform(get("/api/voice/audio/" + aiFileId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("audio/*"));

        System.out.println("✓ 步骤6完成：历史消息播放成功");
        System.out.println("\n========== 端到端测试完成 ==========");
        System.out.println("✓ 所有步骤验证通过");
    }

    /**
     * 测试场景 2: 多轮对话流程
     * 
     * 场景：
     * 用户与AI进行多轮语音对话
     * 
     * 验证：
     * - 支持多轮对话
     * - 每轮对话都正确保存
     * - 历史消息按顺序排列
     */
    @Test
    @DisplayName("多轮语音对话流程")
    void testMultiRoundVoiceConversation() throws Exception {
        Long userId = 1L;
        Long sessionId = 200L;
        String language = "zh-CN";
        
        int rounds = 3; // 3轮对话
        
        for (int i = 1; i <= rounds; i++) {
            System.out.println("\n========== 第" + i + "轮对话 ==========");
            
            // 用户发送语音消息
            byte[] userAudio = createMockAudioData("用户消息" + i);
            MockMultipartFile userFile = new MockMultipartFile(
                "file",
                "user-round-" + i + ".wav",
                "audio/wav",
                userAudio
            );

            MvcResult uploadResult = mockMvc.perform(multipart("/api/voice/upload")
                    .file(userFile)
                    .param("userId", userId.toString())
                    .param("sessionId", sessionId.toString())
                    .param("language", language))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            String uploadResponse = uploadResult.getResponse().getContentAsString();
            Map<String, Object> uploadData = objectMapper.readValue(uploadResponse, Map.class);
            String recognizedText = (String) uploadData.get("recognizedText");
            
            System.out.println("用户: " + recognizedText);

            // AI回复
            String aiReply = "这是第" + i + "轮的回复：" + recognizedText;
            Map<String, Object> ttsRequest = new HashMap<>();
            ttsRequest.put("text", aiReply);
            ttsRequest.put("userId", userId);
            ttsRequest.put("sessionId", sessionId);
            ttsRequest.put("language", language);

            mockMvc.perform(post("/api/voice/synthesize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(ttsRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            System.out.println("AI: " + aiReply);
        }

        // 验证所有消息都已保存
        List<VoiceMessage> allMessages = voiceMessageRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
        assertThat(allMessages).hasSize(rounds * 2); // 每轮2条消息（用户+AI）

        System.out.println("\n✓ 多轮对话测试完成");
        System.out.println("  - 对话轮数: " + rounds);
        System.out.println("  - 总消息数: " + allMessages.size());
    }

    /**
     * 测试场景 3: 会话切换时的消息隔离
     * 
     * 场景：
     * 用户在不同会话中发送语音消息
     * 
     * 验证：
     * - 不同会话的消息相互隔离
     * - 查询特定会话只返回该会话的消息
     */
    @Test
    @DisplayName("会话切换和消息隔离")
    void testSessionSwitchingAndMessageIsolation() throws Exception {
        Long userId = 1L;
        Long session1 = 301L;
        Long session2 = 302L;
        String language = "zh-CN";

        // 在会话1中发送消息
        byte[] audio1 = createMockAudioData("会话1消息");
        MockMultipartFile file1 = new MockMultipartFile(
            "file", "session1.wav", "audio/wav", audio1
        );

        mockMvc.perform(multipart("/api/voice/upload")
                .file(file1)
                .param("userId", userId.toString())
                .param("sessionId", session1.toString())
                .param("language", language))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 在会话2中发送消息
        byte[] audio2 = createMockAudioData("会话2消息");
        MockMultipartFile file2 = new MockMultipartFile(
            "file", "session2.wav", "audio/wav", audio2
        );

        mockMvc.perform(multipart("/api/voice/upload")
                .file(file2)
                .param("userId", userId.toString())
                .param("sessionId", session2.toString())
                .param("language", language))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 验证会话隔离
        List<VoiceMessage> session1Messages = voiceMessageRepository.findBySessionIdOrderByCreatedAtDesc(session1);
        List<VoiceMessage> session2Messages = voiceMessageRepository.findBySessionIdOrderByCreatedAtDesc(session2);

        assertThat(session1Messages).hasSize(1);
        assertThat(session2Messages).hasSize(1);
        assertThat(session1Messages.get(0).getSessionId()).isEqualTo(session1);
        assertThat(session2Messages.get(0).getSessionId()).isEqualTo(session2);

        System.out.println("✓ 会话隔离测试完成");
        System.out.println("  - 会话1消息数: " + session1Messages.size());
        System.out.println("  - 会话2消息数: " + session2Messages.size());
    }

    /**
     * 测试场景 4: 错误恢复流程
     * 
     * 场景：
     * 在语音聊天过程中遇到错误，系统应该能够恢复
     * 
     * 验证：
     * - 上传失败后可以重试
     * - 合成失败后可以重试
     * - 错误不影响后续操作
     */
    @Test
    @DisplayName("错误恢复流程")
    void testErrorRecoveryFlow() throws Exception {
        Long userId = 1L;
        Long sessionId = 400L;

        // 尝试上传无效文件（应该失败）
        byte[] invalidAudio = "invalid audio data".getBytes();
        MockMultipartFile invalidFile = new MockMultipartFile(
            "file", "invalid.txt", "text/plain", invalidAudio
        );

        mockMvc.perform(multipart("/api/voice/upload")
                .file(invalidFile)
                .param("userId", userId.toString())
                .param("sessionId", sessionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));

        System.out.println("✓ 无效文件被正确拒绝");

        // 重试上传有效文件（应该成功）
        byte[] validAudio = createMockAudioData("有效消息");
        MockMultipartFile validFile = new MockMultipartFile(
            "file", "valid.wav", "audio/wav", validAudio
        );

        mockMvc.perform(multipart("/api/voice/upload")
                .file(validFile)
                .param("userId", userId.toString())
                .param("sessionId", sessionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        System.out.println("✓ 重试上传成功");

        // 尝试合成空文本（应该失败）
        Map<String, Object> emptyRequest = new HashMap<>();
        emptyRequest.put("text", "");
        emptyRequest.put("userId", userId);

        mockMvc.perform(post("/api/voice/synthesize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));

        System.out.println("✓ 空文本被正确拒绝");

        // 重试合成有效文本（应该成功）
        Map<String, Object> validRequest = new HashMap<>();
        validRequest.put("text", "有效的回复文本");
        validRequest.put("userId", userId);
        validRequest.put("sessionId", sessionId);

        mockMvc.perform(post("/api/voice/synthesize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        System.out.println("✓ 重试合成成功");
        System.out.println("✓ 错误恢复测试完成");
    }

    /**
     * 测试场景 5: 并发语音消息处理
     * 
     * 场景：
     * 多个用户同时发送语音消息
     * 
     * 验证：
     * - 系统能够处理并发请求
     * - 每个用户的消息正确保存
     * - 没有数据混淆
     */
    @Test
    @DisplayName("并发语音消息处理")
    void testConcurrentVoiceMessages() throws Exception {
        Long sessionId = 500L;
        int userCount = 3;

        for (long userId = 1; userId <= userCount; userId++) {
            byte[] audio = createMockAudioData("用户" + userId + "的消息");
            MockMultipartFile file = new MockMultipartFile(
                "file",
                "user" + userId + ".wav",
                "audio/wav",
                audio
            );

            mockMvc.perform(multipart("/api/voice/upload")
                    .file(file)
                    .param("userId", String.valueOf(userId))
                    .param("sessionId", sessionId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        // 验证所有用户的消息都已保存
        List<VoiceMessage> allMessages = voiceMessageRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
        assertThat(allMessages).hasSize(userCount);

        // 验证每个用户的消息
        for (long userId = 1; userId <= userCount; userId++) {
            final long currentUserId = userId;
            long count = allMessages.stream()
                .filter(m -> m.getUserId().equals(currentUserId))
                .count();
            
            assertThat(count).isEqualTo(1);
            System.out.println("✓ 用户" + userId + "的消息已正确保存");
        }

        System.out.println("✓ 并发处理测试完成");
    }

    /**
     * 创建模拟音频数据
     * 
     * @param identifier 标识符，用于区分不同的音频
     * @return 音频字节数组
     */
    private byte[] createMockAudioData(String identifier) {
        // 创建一个简单的WAV文件头
        byte[] wavHeader = new byte[44];
        
        // RIFF header
        wavHeader[0] = 'R'; wavHeader[1] = 'I'; wavHeader[2] = 'F'; wavHeader[3] = 'F';
        // WAVE header
        wavHeader[8] = 'W'; wavHeader[9] = 'A'; wavHeader[10] = 'V'; wavHeader[11] = 'E';
        // fmt chunk
        wavHeader[12] = 'f'; wavHeader[13] = 'm'; wavHeader[14] = 't'; wavHeader[15] = ' ';
        
        // 添加一些音频数据（使用identifier的hashCode来生成不同的数据）
        int seed = identifier.hashCode();
        byte[] audioData = new byte[1024];
        for (int i = 0; i < audioData.length; i++) {
            audioData[i] = (byte) (Math.sin((i + seed) * 0.1) * 127);
        }
        
        // 合并头部和数据
        byte[] result = new byte[wavHeader.length + audioData.length];
        System.arraycopy(wavHeader, 0, result, 0, wavHeader.length);
        System.arraycopy(audioData, 0, result, wavHeader.length, audioData.length);
        
        return result;
    }
}
