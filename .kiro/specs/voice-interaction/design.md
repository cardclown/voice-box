# 语音交互功能设计文档

## 概述

本文档描述了VoiceBox系统语音交互功能的技术设计方案。该功能通过集成语音识别（STT）和语音合成（TTS）服务，为用户提供完整的语音输入和语音输出能力，实现更自然、便捷的AI交互体验。

### 设计目标

1. **易用性**: 提供直观的语音交互界面，降低使用门槛
2. **高质量**: 确保语音识别准确率≥95%，语音合成自然度≥4.0/5.0
3. **高性能**: STT响应时间≤5秒，TTS首字节时间≤2秒
4. **可扩展**: 支持多种语音服务提供商，便于切换和扩展
5. **个性化**: 根据用户画像调整语音音色和语速
6. **可靠性**: 提供完善的错误处理和降级方案

## 架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                          前端层 (Vue 3)                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ 语音输入组件  │  │ 语音播放组件  │  │ 波形可视化   │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────────┐
│                      后端服务层 (Spring Boot)                     │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              VoiceController (REST API)                   │   │
│  └────────┬─────────────────────────────────────────────────┘   │
│           │                                                      │
│  ┌────────┴────────┬──────────────┬──────────────┐             │
│  │ VoiceInputSvc   │ VoiceOutputSvc│ VoiceStorageSvc│           │
│  └─────────────────┴──────────────┴──────────────┘             │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────────┐
│                      语音服务层 (代理)                            │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              VoiceServiceProxy (负载均衡)                  │   │
│  └────────┬─────────────────────────────────────────────────┘   │
│           │                                                      │
│  ┌────────┴────────┬──────────────┬──────────────┐             │
│  │  阿里云语音     │  腾讯云语音   │  Azure 语音   │             │
│  │  (主服务)       │  (备用)       │  (备用)       │             │
│  └─────────────────┴──────────────┴──────────────┘             │
└─────────────────────────────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────────┐
│                      存储层                                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ MySQL        │  │ 文件系统      │  │ Redis 缓存   │          │
│  │ (元数据)     │  │ (音频文件)    │  │ (临时数据)   │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

### 数据流

**语音输入流程**:
```
1. 用户点击录音 → 前端请求麦克风权限
2. 开始录音 → 前端录制音频（WebRTC）
3. 停止录音 → 前端上传音频文件
4. 后端接收 → 保存到文件系统
5. 调用STT → 语音转文字
6. 返回文本 → 前端显示并发送消息
```

**语音输出流程**:
```
1. AI生成文本 → 后端接收回复
2. 调用TTS → 文字转语音
3. 保存音频 → 文件系统
4. 返回URL → 前端获取音频地址
5. 播放音频 → 前端音频播放器
```

## 组件和接口

### 前端组件

#### VoiceInput.vue - 语音输入组件

```vue
<template>
  <div class="voice-input">
    <button 
      @mousedown="startRecording"
      @mouseup="stopRecording"
      @touchstart="startRecording"
      @touchend="stopRecording"
      :disabled="!hasPermission"
      class="voice-button"
    >
      <MicrophoneIcon v-if="!isRecording" />
      <WaveformAnimation v-else :duration="recordingDuration" />
    </button>
    
    <div v-if="isProcessing" class="processing-indicator">
      <Spinner />
      <span>正在识别...</span>
    </div>
    
    <div v-if="recognizedText" class="recognized-text">
      <p>{{ recognizedText }}</p>
      <button @click="editText">编辑</button>
      <button @click="sendMessage">发送</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useVoiceInput } from '@/composables/useVoiceInput'

const {
  isRecording,
  isProcessing,
  hasPermission,
  recordingDuration,
  recognizedText,
  startRecording,
  stopRecording,
  requestPermission
} = useVoiceInput()

onMounted(() => {
  requestPermission()
})
</script>
```

#### VoicePlayer.vue - 语音播放组件

```vue
<template>
  <div class="voice-player">
    <button @click="togglePlay" class="play-button">
      <PlayIcon v-if="!isPlaying" />
      <PauseIcon v-else />
    </button>
    
    <div class="progress-bar">
      <div class="progress" :style="{ width: progress + '%' }"></div>
    </div>
    
    <span class="duration">{{ formatTime(currentTime) }} / {{ formatTime(totalDuration) }}</span>
    
    <audio 
      ref="audioRef"
      :src="audioUrl"
      @timeupdate="updateProgress"
      @ended="onEnded"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useVoicePlayer } from '@/composables/useVoicePlayer'

const props = defineProps({
  audioUrl: String,
  autoPlay: Boolean
})

const {
  isPlaying,
  currentTime,
  totalDuration,
  progress,
  togglePlay,
  updateProgress,
  onEnded
} = useVoicePlayer(props.audioUrl, props.autoPlay)
</script>
```

### 后端服务

#### VoiceController.java - REST API控制器

```java
@RestController
@RequestMapping("/api/voice")
@CrossOrigin(origins = "*")
public class VoiceController {
    
    @Autowired
    private VoiceInputService voiceInputService;
    
    @Autowired
    private VoiceOutputService voiceOutputService;
    
    /**
     * 上传语音文件并转换为文本
     */
    @PostMapping("/upload")
    public ResponseEntity<VoiceUploadResponse> uploadVoice(
        @RequestParam("file") MultipartFile file,
        @RequestParam("userId") Long userId,
        @RequestParam("sessionId") Long sessionId,
        @RequestParam(value = "language", defaultValue = "zh-CN") String language
    ) {
        try {
            VoiceUploadResponse response = voiceInputService.processVoiceInput(
                file, userId, sessionId, language
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(VoiceUploadResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 将文本转换为语音
     */
    @PostMapping("/synthesize")
    public ResponseEntity<VoiceSynthesisResponse> synthesizeVoice(
        @RequestBody VoiceSynthesisRequest request
    ) {
        try {
            VoiceSynthesisResponse response = voiceOutputService.synthesizeVoice(
                request.getText(),
                request.getUserId(),
                request.getLanguage(),
                request.getVoiceProfile()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(VoiceSynthesisResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 获取语音文件
     */
    @GetMapping("/audio/{fileId}")
    public ResponseEntity<Resource> getAudioFile(@PathVariable String fileId) {
        try {
            Resource resource = voiceStorageService.loadAudioFile(fileId);
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
```

#### VoiceInputService.java - 语音输入服务

```java
@Service
public class VoiceInputService {
    
    @Autowired
    private VoiceServiceProxy voiceServiceProxy;
    
    @Autowired
    private VoiceStorageService storageService;
    
    @Autowired
    private VoiceMessageRepository messageRepository;
    
    public VoiceUploadResponse processVoiceInput(
        MultipartFile file,
        Long userId,
        Long sessionId,
        String language
    ) throws Exception {
        
        // 1. 验证文件
        validateAudioFile(file);
        
        // 2. 保存原始音频文件
        String fileId = storageService.saveAudioFile(file, userId);
        
        // 3. 调用STT服务
        String recognizedText = voiceServiceProxy.speechToText(
            file.getInputStream(),
            language
        );
        
        // 4. 保存语音消息记录
        VoiceMessage voiceMessage = new VoiceMessage();
        voiceMessage.setUserId(userId);
        voiceMessage.setSessionId(sessionId);
        voiceMessage.setFileId(fileId);
        voiceMessage.setRecognizedText(recognizedText);
        voiceMessage.setDuration(getAudioDuration(file));
        voiceMessage.setLanguage(language);
        messageRepository.save(voiceMessage);
        
        // 5. 返回结果
        return VoiceUploadResponse.builder()
            .fileId(fileId)
            .recognizedText(recognizedText)
            .confidence(0.95)
            .duration(voiceMessage.getDuration())
            .build();
    }
    
    private void validateAudioFile(MultipartFile file) throws Exception {
        // 检查文件大小（最大10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("文件大小超过限制");
        }
        
        // 检查文件格式
        String contentType = file.getContentType();
        if (!contentType.startsWith("audio/")) {
            throw new IllegalArgumentException("不支持的文件格式");
        }
    }
}
```

#### VoiceOutputService.java - 语音输出服务

```java
@Service
public class VoiceOutputService {
    
    @Autowired
    private VoiceServiceProxy voiceServiceProxy;
    
    @Autowired
    private VoiceStorageService storageService;
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    public VoiceSynthesisResponse synthesizeVoice(
        String text,
        Long userId,
        String language,
        VoiceProfile voiceProfile
    ) throws Exception {
        
        // 1. 获取用户偏好的语音配置
        if (voiceProfile == null) {
            voiceProfile = getUserVoiceProfile(userId);
        }
        
        // 2. 调用TTS服务
        byte[] audioData = voiceServiceProxy.textToSpeech(
            text,
            language,
            voiceProfile
        );
        
        // 3. 保存音频文件
        String fileId = storageService.saveAudioData(audioData, userId);
        
        // 4. 返回结果
        return VoiceSynthesisResponse.builder()
            .fileId(fileId)
            .audioUrl("/api/voice/audio/" + fileId)
            .duration(estimateDuration(text))
            .build();
    }
    
    private VoiceProfile getUserVoiceProfile(Long userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId);
        
        if (profile == null) {
            return VoiceProfile.getDefault();
        }
        
        // 根据用户性格特征选择语音音色
        VoiceProfile voiceProfile = new VoiceProfile();
        
        // 外向性高 -> 活力音色
        if (profile.getExtraversion().compareTo(BigDecimal.valueOf(0.6)) > 0) {
            voiceProfile.setVoiceName("xiaoyun");  // 活力女声
            voiceProfile.setSpeechRate(1.1);
        } else {
            voiceProfile.setVoiceName("xiaogang");  // 温和男声
            voiceProfile.setSpeechRate(1.0);
        }
        
        return voiceProfile;
    }
}
```

## 数据模型

### 数据库表设计

#### voice_messages - 语音消息表

```sql
CREATE TABLE voice_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    message_id BIGINT,                      -- 关联的聊天消息ID
    file_id VARCHAR(255) NOT NULL,          -- 音频文件ID
    file_path VARCHAR(500) NOT NULL,        -- 文件存储路径
    file_size BIGINT NOT NULL,              -- 文件大小（字节）
    duration INT NOT NULL,                  -- 音频时长（秒）
    format VARCHAR(20) NOT NULL,            -- 音频格式（mp3/wav/ogg）
    sample_rate INT DEFAULT 16000,          -- 采样率
    recognized_text TEXT,                   -- 识别的文本
    confidence DECIMAL(5,4),                -- 识别置信度
    language VARCHAR(10) DEFAULT 'zh-CN',   -- 语言
    is_input BOOLEAN DEFAULT TRUE,          -- true=用户输入, false=AI输出
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id),
    INDEX idx_message_id (message_id),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (session_id) REFERENCES chat_session(id) ON DELETE CASCADE
);
```

#### voice_service_logs - 语音服务调用日志

```sql
CREATE TABLE voice_service_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    service_type VARCHAR(20) NOT NULL,      -- STT/TTS
    provider VARCHAR(50) NOT NULL,          -- 服务提供商
    request_id VARCHAR(255),                -- 请求ID
    input_size BIGINT,                      -- 输入大小
    output_size BIGINT,                     -- 输出大小
    duration_ms INT,                        -- 处理时长（毫秒）
    status VARCHAR(20) NOT NULL,            -- success/failed
    error_message TEXT,                     -- 错误信息
    cost DECIMAL(10,6),                     -- 成本（元）
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_user_id (user_id),
    INDEX idx_service_type (service_type),
    INDEX idx_provider (provider),
    INDEX idx_created_at (created_at)
);
```

### Java实体类

#### VoiceMessage.java

```java
@Entity
@Table(name = "voice_messages")
public class VoiceMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId;
    private Long sessionId;
    private Long messageId;
    
    private String fileId;
    private String filePath;
    private Long fileSize;
    private Integer duration;
    private String format;
    private Integer sampleRate;
    
    @Column(columnDefinition = "TEXT")
    private String recognizedText;
    
    private BigDecimal confidence;
    private String language;
    private Boolean isInput;
    
    private LocalDateTime createdAt;
    
    // Getters and Setters
}
```

#### VoiceProfile.java - 语音配置

```java
public class VoiceProfile {
    private String voiceName;       // 音色名称
    private Double speechRate;      // 语速 (0.5-2.0)
    private Double pitch;           // 音调 (0.5-2.0)
    private Double volume;          // 音量 (0.0-1.0)
    private String emotion;         // 情感 (neutral/happy/sad)
    
    public static VoiceProfile getDefault() {
        VoiceProfile profile = new VoiceProfile();
        profile.setVoiceName("xiaoyun");
        profile.setSpeechRate(1.0);
        profile.setPitch(1.0);
        profile.setVolume(0.8);
        profile.setEmotion("neutral");
        return profile;
    }
}
```


## 正确性属性

*属性是一个特征或行为，应该在系统的所有有效执行中保持为真——本质上是关于系统应该做什么的正式陈述。属性作为人类可读规范和机器可验证正确性保证之间的桥梁。*

### 属性 1: 音频文件存储完整性

*对于任何*上传的音频文件，保存到文件系统后再读取，应该得到完全相同的音频数据（字节级别相同）
**验证需求: 3.1, 3.2**

### 属性 2: 语音识别文本非空性

*对于任何*包含有效语音内容的音频文件，STT服务返回的识别文本不应为空字符串
**验证需求: 1.4, 5.1**

### 属性 3: 录音时长限制

*对于任何*录音操作，当录音时长达到最大限制（5分钟）时，系统应该自动停止录音
**验证需求: 1.6**

### 属性 4: 音频格式验证

*对于任何*上传的文件，如果文件的MIME类型不是audio/*，系统应该拒绝该文件并返回错误
**验证需求: 4.5**

### 属性 5: 播放进度一致性

*对于任何*正在播放的音频，显示的播放进度百分比应该等于 (当前播放时间 / 总时长) × 100
**验证需求: 2.4**

### 属性 6: 语音文件清理

*对于任何*创建时间超过90天的语音文件，系统清理操作应该将其从文件系统中删除
**验证需求: 3.7**

### 属性 7: 权限拒绝后功能禁用

*对于任何*用户，如果麦克风权限被拒绝，语音输入按钮应该处于禁用状态
**验证需求: 1.7, 9.2**

### 属性 8: 服务降级切换

*对于任何*语音服务调用，如果主服务失败，系统应该自动切换到备用服务并重试
**验证需求: 10.3, 10.6**

### 属性 9: 重试指数退避

*对于任何*失败的服务调用，重试间隔应该按指数增长（1秒、2秒、4秒）
**验证需求: 1.8, 10.5**

### 属性 10: 音频压缩质量保持

*对于任何*音频文件，压缩后的比特率不应低于64kbps，以保证语音清晰度
**验证需求: 4.6**

### 属性 11: 语言模型匹配

*对于任何*语音识别请求，使用的STT语言模型应该与请求中指定的语言参数一致
**验证需求: 6.2**

### 属性 12: 个性化音色选择

*对于任何*用户，如果其外向性得分>0.6，TTS应该选择活力音色；否则选择温和音色
**验证需求: 12.1, 12.5, 12.6**

### 属性 13: 文本编辑后发送

*对于任何*识别的文本，用户编辑后发送的消息内容应该是编辑后的文本，而不是原始识别文本
**验证需求: 5.3**

### 属性 14: 播放状态切换

*对于任何*音频播放器，点击播放按钮应该切换播放状态（播放→暂停，暂停→播放）
**验证需求: 2.3, 2.5, 2.6**

### 属性 15: 流式音频连续性

*对于任何*流式播放的音频片段序列，合并后的完整音频应该与原始文本的TTS结果一致
**验证需求: 7.6**

## 错误处理

### 错误分类

#### 1. 客户端错误

| 错误类型 | HTTP状态码 | 错误代码 | 处理策略 |
|---------|-----------|---------|---------|
| 权限被拒绝 | 403 | PERMISSION_DENIED | 提示用户授权 |
| 文件过大 | 413 | FILE_TOO_LARGE | 提示压缩或分段 |
| 格式不支持 | 415 | UNSUPPORTED_FORMAT | 自动转换或提示 |
| 录音时长超限 | 400 | DURATION_EXCEEDED | 自动停止录音 |

#### 2. 服务端错误

| 错误类型 | HTTP状态码 | 错误代码 | 处理策略 |
|---------|-----------|---------|---------|
| STT服务失败 | 502 | STT_SERVICE_ERROR | 重试3次，切换服务商 |
| TTS服务失败 | 502 | TTS_SERVICE_ERROR | 重试3次，仅显示文本 |
| 存储失败 | 500 | STORAGE_ERROR | 重试，记录日志 |
| 网络超时 | 504 | TIMEOUT | 重试，提示用户 |

### 错误处理流程

```java
@Service
public class VoiceErrorHandler {
    
    private static final int MAX_RETRIES = 3;
    
    public <T> T executeWithRetry(Supplier<T> operation, String operationName) {
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < MAX_RETRIES) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                attempt++;
                
                if (attempt < MAX_RETRIES) {
                    // 指数退避
                    long delay = (long) Math.pow(2, attempt) * 1000;
                    Thread.sleep(delay);
                    
                    log.warn("操作失败，第{}次重试: {}", attempt, operationName);
                }
            }
        }
        
        // 所有重试都失败
        log.error("操作最终失败: {}", operationName, lastException);
        throw new VoiceServiceException("操作失败: " + operationName, lastException);
    }
    
    public void handleVoiceError(Exception e, String context) {
        if (e instanceof PermissionDeniedException) {
            // 权限错误 - 不重试
            throw new VoiceException(ErrorCode.PERMISSION_DENIED, "请授予麦克风权限");
        } else if (e instanceof FileTooLargeException) {
            // 文件过大 - 不重试
            throw new VoiceException(ErrorCode.FILE_TOO_LARGE, "文件大小超过10MB");
        } else if (e instanceof ServiceUnavailableException) {
            // 服务不可用 - 切换服务商
            switchToBackupService();
            throw new VoiceException(ErrorCode.SERVICE_UNAVAILABLE, "服务暂时不可用，请稍后重试");
        } else {
            // 其他错误 - 记录日志
            log.error("语音处理错误: {}", context, e);
            throw new VoiceException(ErrorCode.INTERNAL_ERROR, "处理失败，请重试");
        }
    }
}
```

### 降级策略

```java
@Service
public class VoiceDegradationService {
    
    /**
     * STT服务降级
     */
    public String degradedSpeechToText(InputStream audioStream) {
        // 1. 尝试主服务（阿里云）
        try {
            return aliCloudSTT.recognize(audioStream);
        } catch (Exception e) {
            log.warn("阿里云STT失败，切换到腾讯云", e);
        }
        
        // 2. 尝试备用服务（腾讯云）
        try {
            return tencentSTT.recognize(audioStream);
        } catch (Exception e) {
            log.warn("腾讯云STT失败，切换到Azure", e);
        }
        
        // 3. 尝试第三备用（Azure）
        try {
            return azureSTT.recognize(audioStream);
        } catch (Exception e) {
            log.error("所有STT服务都失败", e);
        }
        
        // 4. 完全降级 - 返回提示
        return "[语音识别暂时不可用，请使用文字输入]";
    }
    
    /**
     * TTS服务降级
     */
    public byte[] degradedTextToSpeech(String text, VoiceProfile profile) {
        // 1. 尝试主服务
        try {
            return aliCloudTTS.synthesize(text, profile);
        } catch (Exception e) {
            log.warn("阿里云TTS失败，切换到腾讯云", e);
        }
        
        // 2. 尝试备用服务
        try {
            return tencentTTS.synthesize(text, profile);
        } catch (Exception e) {
            log.warn("腾讯云TTS失败", e);
        }
        
        // 3. 完全降级 - 不提供语音，仅显示文本
        log.error("所有TTS服务都失败，仅显示文本");
        return null;  // 前端检测到null时只显示文本
    }
}
```

## 测试策略

### 单元测试

#### 音频文件处理测试

```java
@Test
public void testAudioFileValidation() {
    // 测试文件大小验证
    MockMultipartFile largeFile = createMockAudioFile(11 * 1024 * 1024);
    assertThrows(IllegalArgumentException.class, () -> {
        voiceInputService.validateAudioFile(largeFile);
    });
    
    // 测试文件格式验证
    MockMultipartFile invalidFile = createMockFile("text/plain");
    assertThrows(IllegalArgumentException.class, () -> {
        voiceInputService.validateAudioFile(invalidFile);
    });
}

@Test
public void testAudioDurationCalculation() {
    byte[] audioData = loadTestAudioFile("test-10s.mp3");
    int duration = audioUtils.calculateDuration(audioData);
    assertEquals(10, duration, 1);  // 允许1秒误差
}
```

#### 语音服务代理测试

```java
@Test
public void testServiceFailover() {
    // 模拟主服务失败
    when(aliCloudSTT.recognize(any())).thenThrow(new ServiceException());
    when(tencentSTT.recognize(any())).thenReturn("测试文本");
    
    String result = voiceServiceProxy.speechToText(audioStream, "zh-CN");
    
    assertEquals("测试文本", result);
    verify(aliCloudSTT, times(1)).recognize(any());
    verify(tencentSTT, times(1)).recognize(any());
}

@Test
public void testRetryWithExponentialBackoff() {
    AtomicInteger attempts = new AtomicInteger(0);
    
    when(sttService.recognize(any())).thenAnswer(invocation -> {
        int attempt = attempts.incrementAndGet();
        if (attempt < 3) {
            throw new ServiceException("临时失败");
        }
        return "成功";
    });
    
    long startTime = System.currentTimeMillis();
    String result = voiceErrorHandler.executeWithRetry(
        () -> sttService.recognize(audioStream),
        "STT"
    );
    long duration = System.currentTimeMillis() - startTime;
    
    assertEquals("成功", result);
    assertEquals(3, attempts.get());
    assertTrue(duration >= 3000);  // 1s + 2s = 3s 最小延迟
}
```

### 属性测试

使用JUnit-Quickcheck进行属性测试：

```java
@RunWith(JUnitQuickcheck.class)
public class VoicePropertyTest {
    
    /**
     * 属性1: 音频文件存储完整性
     */
    @Property
    public void audioStorageRoundTrip(@From(AudioGenerator.class) byte[] originalAudio) {
        // 保存音频
        String fileId = storageService.saveAudioData(originalAudio, 1L);
        
        // 读取音频
        byte[] retrievedAudio = storageService.loadAudioData(fileId);
        
        // 验证完全相同
        assertArrayEquals(originalAudio, retrievedAudio);
    }
    
    /**
     * 属性3: 录音时长限制
     */
    @Property
    public void recordingDurationLimit(@InRange(min = "300", max = "400") int seconds) {
        // 模拟录音
        RecordingSession session = new RecordingSession();
        session.start();
        
        // 等待到达限制
        session.simulateRecording(seconds);
        
        // 验证自动停止
        if (seconds >= 300) {
            assertFalse(session.isRecording());
            assertEquals(300, session.getDuration());
        }
    }
    
    /**
     * 属性5: 播放进度一致性
     */
    @Property
    public void playbackProgressConsistency(
        @InRange(min = "0", max = "100") int currentTime,
        @InRange(min = "100", max = "200") int totalDuration
    ) {
        VoicePlayer player = new VoicePlayer();
        player.setTotalDuration(totalDuration);
        player.setCurrentTime(currentTime);
        
        double expectedProgress = (double) currentTime / totalDuration * 100;
        double actualProgress = player.getProgress();
        
        assertEquals(expectedProgress, actualProgress, 0.01);
    }
    
    /**
     * 属性12: 个性化音色选择
     */
    @Property
    public void personalizedVoiceSelection(
        @InRange(min = "0.0", max = "1.0") double extraversion
    ) {
        UserProfile profile = new UserProfile();
        profile.setExtraversion(BigDecimal.valueOf(extraversion));
        
        VoiceProfile voiceProfile = voiceOutputService.getUserVoiceProfile(profile);
        
        if (extraversion > 0.6) {
            assertEquals("xiaoyun", voiceProfile.getVoiceName());  // 活力音色
        } else {
            assertEquals("xiaogang", voiceProfile.getVoiceName());  // 温和音色
        }
    }
}
```

### 集成测试

```java
@SpringBootTest
@AutoConfigureMockMvc
public class VoiceIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void testCompleteVoiceInputFlow() throws Exception {
        // 1. 上传音频文件
        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "test.mp3",
            "audio/mpeg",
            loadTestAudio()
        );
        
        MvcResult result = mockMvc.perform(
            multipart("/api/voice/upload")
                .file(audioFile)
                .param("userId", "1")
                .param("sessionId", "1")
                .param("language", "zh-CN")
        )
        .andExpect(status().isOk())
        .andReturn();
        
        // 2. 验证返回结果
        VoiceUploadResponse response = parseResponse(result);
        assertNotNull(response.getFileId());
        assertNotNull(response.getRecognizedText());
        assertTrue(response.getConfidence() >= 0.8);
        
        // 3. 验证文件已保存
        assertTrue(storageService.fileExists(response.getFileId()));
        
        // 4. 验证数据库记录
        VoiceMessage message = messageRepository.findByFileId(response.getFileId());
        assertNotNull(message);
        assertEquals(response.getRecognizedText(), message.getRecognizedText());
    }
    
    @Test
    public void testCompleteVoiceOutputFlow() throws Exception {
        // 1. 请求语音合成
        VoiceSynthesisRequest request = new VoiceSynthesisRequest();
        request.setText("你好，这是一个测试");
        request.setUserId(1L);
        request.setLanguage("zh-CN");
        
        MvcResult result = mockMvc.perform(
            post("/api/voice/synthesize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
        .andExpect(status().isOk())
        .andReturn();
        
        // 2. 验证返回结果
        VoiceSynthesisResponse response = parseResponse(result);
        assertNotNull(response.getFileId());
        assertNotNull(response.getAudioUrl());
        
        // 3. 下载音频文件
        mockMvc.perform(get(response.getAudioUrl()))
            .andExpect(status().isOk())
            .andExpect(content().contentType("audio/mpeg"));
    }
}
```

### 性能测试

```java
@Test
public void testSTTPerformance() {
    // 1分钟音频应在5秒内完成识别
    byte[] audioData = loadTestAudio("test-60s.mp3");
    
    long startTime = System.currentTimeMillis();
    String result = voiceServiceProxy.speechToText(
        new ByteArrayInputStream(audioData),
        "zh-CN"
    );
    long duration = System.currentTimeMillis() - startTime;
    
    assertNotNull(result);
    assertTrue(duration < 5000, "STT处理时间超过5秒: " + duration + "ms");
}

@Test
public void testTTSPerformance() {
    // 100字文本应在2秒内返回第一段音频
    String text = generateText(100);
    
    long startTime = System.currentTimeMillis();
    byte[] audioData = voiceServiceProxy.textToSpeech(
        text,
        "zh-CN",
        VoiceProfile.getDefault()
    );
    long duration = System.currentTimeMillis() - startTime;
    
    assertNotNull(audioData);
    assertTrue(duration < 2000, "TTS处理时间超过2秒: " + duration + "ms");
}
```

### 测试覆盖率目标

- 单元测试覆盖率: ≥80%
- 集成测试覆盖率: ≥60%
- 属性测试: 覆盖所有关键属性
- 性能测试: 覆盖所有性能指标

## 部署和运维

### 服务配置

```yaml
# application.yml
voice:
  storage:
    base-path: /data/voicebox/audio
    max-file-size: 10485760  # 10MB
    retention-days: 90
    
  stt:
    primary-provider: aliyun
    backup-providers:
      - tencent
      - azure
    timeout: 10000  # 10秒
    max-retries: 3
    
  tts:
    primary-provider: aliyun
    backup-providers:
      - tencent
    timeout: 5000  # 5秒
    cache-enabled: true
    cache-ttl: 3600  # 1小时
    
  quality:
    min-sample-rate: 16000
    min-bitrate: 64000
    target-lufs: -14.0
```

### 监控指标

```java
@Component
public class VoiceMetrics {
    
    private final MeterRegistry registry;
    
    // STT指标
    public void recordSTTRequest(String provider, long durationMs, boolean success) {
        registry.counter("voice.stt.requests",
            "provider", provider,
            "status", success ? "success" : "failed"
        ).increment();
        
        registry.timer("voice.stt.duration",
            "provider", provider
        ).record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    // TTS指标
    public void recordTTSRequest(String provider, int textLength, long durationMs) {
        registry.counter("voice.tts.requests",
            "provider", provider
        ).increment();
        
        registry.summary("voice.tts.text_length").record(textLength);
        registry.timer("voice.tts.duration").record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    // 存储指标
    public void recordStorageOperation(String operation, long fileSize, boolean success) {
        registry.counter("voice.storage.operations",
            "operation", operation,
            "status", success ? "success" : "failed"
        ).increment();
        
        registry.summary("voice.storage.file_size").record(fileSize);
    }
    
    // 用户使用指标
    public void recordUserVoiceUsage(Long userId, boolean isInput) {
        registry.counter("voice.user.usage",
            "user_id", userId.toString(),
            "type", isInput ? "input" : "output"
        ).increment();
    }
}
```

### 日志规范

```java
// 成功日志
log.info("语音识别成功 - userId: {}, fileId: {}, text: {}, confidence: {}, duration: {}ms",
    userId, fileId, text, confidence, duration);

// 警告日志
log.warn("STT服务响应慢 - provider: {}, duration: {}ms, threshold: {}ms",
    provider, duration, threshold);

// 错误日志
log.error("语音处理失败 - userId: {}, fileId: {}, error: {}",
    userId, fileId, e.getMessage(), e);
```

### 告警规则

| 指标 | 阈值 | 告警级别 | 处理方式 |
|------|------|---------|---------|
| STT错误率 | >5% | 警告 | 检查服务状态 |
| STT错误率 | >10% | 严重 | 切换服务商 |
| TTS响应时间 | >5s | 警告 | 优化配置 |
| 存储空间 | >80% | 警告 | 清理旧文件 |
| 存储空间 | >90% | 严重 | 立即扩容 |
| 服务可用性 | <99% | 严重 | 紧急处理 |
