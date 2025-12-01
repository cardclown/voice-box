# 情感化语音交互模块 - 设计文档

## 概述

情感化语音交互模块是一个独立的功能模块，集成了语音特征分析、情感识别、自动标签生成和情感化语音合成功能。该模块通过分析用户的语音输入来识别性格、性别、语气和情绪特征，并根据这些特征生成个性化的带情感的语音响应。

---

## 架构设计

### 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    前端 - 语音交互界面                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ 语音输入组件  │  │ 情绪可视化   │  │ 标签展示     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    API层 - REST接口                          │
│  /api/emotional-voice/analyze                               │
│  /api/emotional-voice/synthesize                            │
│  /api/emotional-voice/profile                               │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    服务层                                     │
│  ┌──────────────────┐  ┌──────────────────┐                │
│  │ 语音特征分析服务  │  │ 情感识别服务     │                │
│  └──────────────────┘  └──────────────────┘                │
│  ┌──────────────────┐  ┌──────────────────┐                │
│  │ 标签生成服务      │  │ 情感语音合成服务 │                │
│  └──────────────────┘  └──────────────────┘                │
│  ┌──────────────────┐  ┌──────────────────┐                │
│  │ 用户画像服务      │  │ 统计分析服务     │                │
│  └──────────────────┘  └──────────────────┘                │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    数据层                                     │
│  ┌──────────────────┐  ┌──────────────────┐                │
│  │ 语音特征数据      │  │ 用户标签数据     │                │
│  └──────────────────┘  └──────────────────┘                │
│  ┌──────────────────┐  ┌──────────────────┐                │
│  │ 情绪历史数据      │  │ 音频文件存储     │                │
│  └──────────────────┘  └──────────────────┘                │
└─────────────────────────────────────────────────────────────┘
```

---

## 组件和接口

### 1. 语音特征分析服务 (VoiceFeatureAnalyzer)

**职责**: 提取和分析语音的音频特征

**接口**:
```java
public interface VoiceFeatureAnalyzer {
    VoiceFeatures extractFeatures(byte[] audioData);
    GenderPrediction predictGender(VoiceFeatures features);
    PersonalityTraits analyzePersonality(VoiceFeatures features, String text);
}
```

**关键方法**:
- `extractFeatures()`: 提取音高、音量、语速、音色等特征
- `predictGender()`: 基于音高范围预测性别
- `analyzePersonality()`: 结合语音和文本分析性格

---

### 2. 情感识别服务 (EmotionRecognitionService)

**职责**: 识别语音中的情绪状态

**接口**:
```java
public interface EmotionRecognitionService {
    EmotionResult recognizeEmotion(VoiceFeatures features, String text);
    ToneStyle analyzeTone(VoiceFeatures features, String text);
    float calculateEmotionIntensity(EmotionResult emotion);
}
```

**情绪类型**:
- HAPPY (开心)
- SAD (悲伤)
- ANGRY (愤怒)
- CALM (平静)
- ANXIOUS (焦虑)
- EXCITED (兴奋)
- NEUTRAL (中性)

---

### 2.5. 智能触发服务 (SmartTriggerService)

**职责**: 根据语音方向和语气自动判断用户是否在和系统对话

**接口**:
```java
public interface SmartTriggerService {
    TriggerDecision shouldTrigger(VoiceFeatures features, String text);
    DirectionConfidence detectDirection(byte[] audioData);
    IntentConfidence detectIntent(String text, VoiceFeatures features);
}
```

**触发判断逻辑**:

1. **语音方向检测**:
   - 使用音量强度判断距离（近距离音量大）
   - 使用音频清晰度判断方向（正对设备清晰度高）
   - 返回方向置信度（0-1）

2. **语气意图识别**:
   - 检测是否包含唤醒词（"小助手"、"你好"等）
   - 检测是否为疑问句（语调上扬）
   - 检测是否为命令句（语气坚定）
   - 检测停顿模式（等待回复的停顿）
   - 返回意图置信度（0-1）

3. **综合判断**:
   - 方向置信度 > 0.7 且 意图置信度 > 0.6 → 自动触发
   - 方向置信度 > 0.8 → 降低意图阈值到0.5
   - 包含唤醒词 → 直接触发

**触发决策**:
```java
public class TriggerDecision {
    private boolean shouldTrigger;        // 是否触发
    private float confidence;             // 综合置信度
    private String reason;                // 触发原因
    private DirectionConfidence direction; // 方向检测结果
    private IntentConfidence intent;      // 意图检测结果
}
```

**配置参数**:
- `directionThreshold`: 方向置信度阈值（默认0.7）
- `intentThreshold`: 意图置信度阈值（默认0.6）
- `wakeWords`: 唤醒词列表
- `autoTriggerEnabled`: 是否启用自动触发

---

### 3. 标签生成服务 (EmotionalTagGenerator)

**职责**: 根据分析结果自动生成用户标签

**接口**:
```java
public interface EmotionalTagGenerator {
    List<UserTag> generateTags(VoiceAnalysisResult analysis);
    void updateUserProfile(Long userId, List<UserTag> tags);
    List<UserTag> getUserTags(Long userId, TagCategory category);
}
```

**标签类别**:
- GENDER (性别): 男性、女性、中性
- PERSONALITY (性格): 外向型、内向型、理性型、感性型
- EMOTION (情绪): 乐观、悲观、平和、急躁
- TONE (语气): 正式、随意、幽默、严肃

---

### 4. 情感语音合成服务 (EmotionalTTSService)

**职责**: 生成带有情感的语音输出

**接口**:
```java
public interface EmotionalTTSService {
    byte[] synthesizeWithEmotion(String text, EmotionConfig config);
    VoiceConfig selectVoiceByProfile(UserProfile profile);
    EmotionConfig buildEmotionConfig(EmotionType emotion, float intensity);
}
```

**情感配置**:
```java
public class EmotionConfig {
    private EmotionType emotionType;      // 情绪类型
    private float intensity;               // 情感强度 (0-1)
    private float speedRate;               // 语速倍率 (0.5-2.0)
    private float pitchRate;               // 音调倍率 (0.5-2.0)
    private String voiceId;                // 音色ID
}
```

---

### 5. 用户画像服务 (EmotionalProfileService)

**职责**: 管理用户的情感画像数据

**接口**:
```java
public interface EmotionalProfileService {
    EmotionalProfile getProfile(Long userId);
    void updateProfile(Long userId, VoiceAnalysisResult analysis);
    EmotionStatistics getEmotionStatistics(Long userId, LocalDate startDate, LocalDate endDate);
}
```

---

## 数据模型

### 1. 语音特征 (VoiceFeatures)

```java
public class VoiceFeatures {
    private Float averagePitch;        // 平均音高 (Hz)
    private Float pitchVariance;       // 音高方差
    private Float averageVolume;       // 平均音量 (dB)
    private Float volumeVariance;      // 音量方差
    private Float speechRate;          // 语速 (字/分钟)
    private Float pauseDuration;       // 停顿时长 (秒)
    private Float energyLevel;         // 能量水平
    private Map<String, Float> spectralFeatures;  // 频谱特征
}
```

### 2. 情绪分析结果 (EmotionResult)

```java
public class EmotionResult {
    private EmotionType primaryEmotion;    // 主要情绪
    private Float confidence;              // 置信度 (0-1)
    private Float intensity;               // 强度 (0-1)
    private Map<EmotionType, Float> emotionScores;  // 各情绪得分
    private ToneStyle toneStyle;           // 语气风格
}
```

### 3. 用户标签 (EmotionalTag)

```java
@Entity
@Table(name = "emotional_tags")
public class EmotionalTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId;
    private TagCategory category;      // 标签类别
    private String tagName;            // 标签名称
    private Float confidence;          // 置信度
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;          // 是否激活
}
```

### 4. 情感画像 (EmotionalProfile)

```java
@Entity
@Table(name = "emotional_profiles")
public class EmotionalProfile {
    @Id
    private Long userId;
    
    private String predictedGender;        // 预测性别
    private Float genderConfidence;        // 性别置信度
    
    private String personalityType;        // 性格类型
    private Float personalityConfidence;   // 性格置信度
    
    private String dominantEmotion;        // 主导情绪
    private String preferredTone;          // 偏好语气
    
    private Float averageSpeechRate;       // 平均语速
    private Float averagePitch;            // 平均音高
    
    private Integer totalInteractions;     // 总交互次数
    private LocalDateTime lastUpdated;
}
```

### 5. 情绪历史记录 (EmotionHistory)

```java
@Entity
@Table(name = "emotion_history")
public class EmotionHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId;
    private Long sessionId;
    private String audioFileId;
    
    private EmotionType detectedEmotion;
    private Float emotionIntensity;
    private ToneStyle toneStyle;
    
    private String transcribedText;
    private LocalDateTime timestamp;
}
```

---

## 正确性属性

*属性是系统应该满足的特征或行为，在所有有效执行中都应该成立。*

### 属性 1: 特征提取一致性

**对于任意**音频文件，如果多次提取特征，应该得到相同的特征向量（误差在可接受范围内）

**验证需求**: 1.1, 1.2

### 属性 2: 性别识别稳定性

**对于任意**用户，当有足够样本时（≥5次），性别识别结果应该保持稳定

**验证需求**: 2.4

### 属性 3: 情绪识别合理性

**对于任意**语音输入，识别的情绪应该与语音特征相符（如高音高+快语速不应识别为"悲伤"）

**验证需求**: 4.1, 4.2

### 属性 4: 标签置信度单调性

**对于任意**标签，随着样本数量增加，置信度应该趋于稳定或提高

**验证需求**: 6.2, 6.3

### 属性 5: 情感语音一致性

**对于任意**情感配置，生成的语音应该与配置的情感类型匹配

**验证需求**: 8.1, 8.2, 8.3, 8.4

### 属性 6: 音色选择适配性

**对于任意**用户画像，选择的音色应该与用户的性别和性格特征相匹配

**验证需求**: 9.1, 9.2, 9.3, 9.4

### 属性 7: 语速调整合理性

**对于任意**用户语速，系统调整的回复语速应该在合理范围内（0.5-2.0倍）

**验证需求**: 10.1, 10.2, 10.5

### 属性 8: 情感强度限制

**对于任意**情感强度输入，系统应该将其限制在0-1范围内

**验证需求**: 11.3, 11.5

### 属性 9: 标签更新幂等性

**对于任意**相同的分析结果，多次更新标签应该产生相同的结果

**验证需求**: 6.4

### 属性 10: 数据加密完整性

**对于任意**敏感数据，加密后解密应该得到原始数据

**验证需求**: 17.1, 17.2

---

## 错误处理

### 错误类型

1. **音频质量不足**: 返回低置信度，提示用户重新录音
2. **特征提取失败**: 使用默认特征值，记录错误日志
3. **情感识别失败**: 返回NEUTRAL情绪，置信度为0
4. **语音合成失败**: 降级到纯文本响应
5. **数据库错误**: 使用缓存数据，异步重试

### 降级策略

```
情感识别失败 → 使用默认中性情绪
    ↓
语音合成失败 → 返回纯文本
    ↓
标签生成失败 → 跳过标签更新，继续处理
    ↓
数据库不可用 → 使用内存缓存
```

---

## 测试策略

### 单元测试
- 测试特征提取算法的准确性
- 测试情绪识别逻辑
- 测试标签生成规则
- 测试情感配置构建

### 属性测试
- 使用fast-check生成随机音频特征
- 验证所有正确性属性
- 测试边界情况和异常输入

### 集成测试
- 测试完整的语音分析流程
- 测试情感语音合成流程
- 测试用户画像更新流程

### 性能测试
- 测试特征提取响应时间（<3秒）
- 测试情感识别响应时间（<2秒）
- 测试语音合成响应时间（<3秒）
- 测试并发处理能力（≥10用户）

---

## 部署架构

### 服务部署

```
┌─────────────────┐
│   Nginx         │  (反向代理)
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
┌───▼───┐ ┌──▼────┐
│ Web   │ │ API   │
│ Server│ │ Server│
└───────┘ └───┬───┘
              │
         ┌────┴────┐
         │         │
    ┌────▼───┐ ┌──▼────┐
    │ MySQL  │ │ Redis │
    └────────┘ └───────┘
```

### 扩展性考虑

- 情感识别服务可独立扩展
- 语音合成服务可独立扩展
- 使用Redis缓存热点数据
- 使用消息队列处理异步任务

---

## 安全考虑

1. **数据加密**: 所有语音文件和敏感标签数据加密存储
2. **访问控制**: 用户只能访问自己的数据
3. **数据脱敏**: 导出数据时匿名化处理
4. **审计日志**: 记录所有数据访问和修改操作
5. **数据保留**: 支持用户删除所有个人数据

---

## 性能优化

1. **特征缓存**: 缓存已提取的特征向量
2. **异步处理**: 标签更新和统计计算异步执行
3. **批量处理**: 批量更新数据库减少IO
4. **CDN加速**: 音频文件通过CDN分发
5. **连接池**: 使用数据库连接池提高性能

---

## 监控和日志

### 关键指标

- 特征提取成功率
- 情感识别准确率
- 语音合成成功率
- API响应时间
- 系统错误率

### 日志级别

- INFO: 正常操作日志
- WARN: 低置信度结果、降级操作
- ERROR: 处理失败、异常情况
- DEBUG: 详细的特征数据、中间结果

---

## 未来扩展

1. **深度学习模型**: 集成更先进的情感识别模型
2. **实时流式处理**: 支持实时语音流分析
3. **多模态融合**: 结合面部表情、文本情感分析
4. **个性化训练**: 为每个用户训练专属模型
5. **情感对话策略**: 根据情感状态调整对话策略
