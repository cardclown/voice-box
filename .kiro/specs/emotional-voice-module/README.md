# 情感化语音交互模块

## 概述

情感化语音交互模块是一个独立的功能模块，通过分析用户的语音输入来识别性格、性别、语气和情绪特征，自动生成用户标签，并根据用户特征返回带有情感的个性化语音响应。

## 核心功能

### 🎤 语音输入分析
- **语音特征提取**: 音高、音量、语速、音色
- **性别识别**: 男性/女性/中性，带置信度
- **性格分析**: 外向/内向、理性/感性
- **情绪识别**: 开心、悲伤、愤怒、平静、焦虑、兴奋
- **语气风格**: 正式、随意、幽默、严肃

### 🏷️ 自动标签生成
- 根据分析结果自动生成用户标签
- 标签包含类型、值、置信度和时间戳
- 支持标签更新和冲突处理
- 标签过期自动降权

### 👤 用户画像管理
- 维护完整的用户情感画像
- 累积多次交互数据提高准确性
- 支持画像查询和历史追踪
- 标签分类管理（性别、性格、情绪、语气）

### 🎭 情感化语音输出
- **情感语音合成**: 根据用户情绪调整回复情感
- **音色个性化**: 根据性别和性格选择音色
- **语速音调调整**: 匹配用户说话风格
- **情感强度控制**: 恰当的情感表达

### 🖥️ 独立模块界面
- 专注的语音交互体验
- 实时情绪反馈和可视化
- 标签展示和管理
- 对话历史记录
- 情感统计分析

## 技术架构

```
前端界面 (Vue 3)
    ↓
REST API
    ↓
服务层
├── 语音特征分析
├── 情绪识别
├── 标签生成
├── 用户画像管理
└── 情感语音合成
    ↓
数据层 (MySQL)
```

## 文档结构

- `requirements.md` - 详细需求文档（20个需求）
- `design.md` - 技术设计文档
- `tasks.md` - 实施计划（34个任务）
- `README.md` - 本文档

## 快速开始

### 1. 查看需求
```bash
cat .kiro/specs/emotional-voice-module/requirements.md
```

### 2. 查看设计
```bash
cat .kiro/specs/emotional-voice-module/design.md
```

### 3. 开始实施
打开`tasks.md`文件，从任务1开始执行。

## 实施计划

### 阶段划分

1. **阶段1-2**: 数据库和语音特征分析（4-6天）
2. **阶段3-4**: 情绪识别和标签生成（5-7天）
3. **阶段5**: 情感化语音合成（4-5天）
4. **阶段6**: REST API（2-3天）
5. **阶段7-8**: 前端界面和服务（7-9天）
6. **阶段9-11**: 优化和增强（6-9天）
7. **阶段12**: 集成和测试（2-3天）

**总计**: 约30-42天

### 里程碑

- **MVP**: 完成阶段1-6（基础功能和API）
- **完整版**: 完成阶段1-8（包含前端界面）
- **优化版**: 完成所有阶段（包含安全和性能优化）

## 核心服务

### 后端服务

1. **VoiceFeatureAnalyzer** - 语音特征提取
2. **EmotionRecognitionService** - 情绪识别
3. **EmotionalTagGenerator** - 标签生成
4. **EmotionalProfileService** - 用户画像管理
5. **EmotionalTTSService** - 情感语音合成

### 前端组件

1. **EmotionalVoice.vue** - 主页面
2. **EmotionalVoiceInput.vue** - 语音输入
3. **EmotionFeedback.vue** - 实时情绪反馈
4. **TagVisualization.vue** - 标签可视化
5. **EmotionHistory.vue** - 对话历史
6. **EmotionStatistics.vue** - 情感统计

## API接口

### 语音分析
```
POST /api/emotional-voice/analyze
Content-Type: multipart/form-data

参数:
- audio: 音频文件
- userId: 用户ID
- sessionId: 会话ID

返回:
{
  "voiceFeatures": {...},
  "gender": "male",
  "genderConfidence": 0.85,
  "personality": "extrovert",
  "emotion": "happy",
  "emotionIntensity": 0.7,
  "toneStyle": "casual",
  "tags": [...]
}
```

### 情感语音合成
```
POST /api/emotional-voice/synthesize
Content-Type: application/json

参数:
{
  "text": "你好",
  "userId": 1,
  "sessionId": 1,
  "emotionType": "happy",
  "intensity": 0.7
}

返回:
{
  "success": true,
  "fileId": "...",
  "audioUrl": "/api/voice/audio/...",
  "emotionConfig": {...}
}
```

### 用户画像查询
```
GET /api/emotional-voice/profile/{userId}

返回:
{
  "userId": 1,
  "gender": "male",
  "personalityType": "extrovert",
  "dominantEmotion": "happy",
  "tags": [...],
  "statistics": {...}
}
```

## 数据模型

### 情感标签 (EmotionalTag)
- 标签类别（性别、性格、情绪、语气）
- 标签名称和值
- 置信度
- 创建和更新时间

### 情感画像 (EmotionalProfile)
- 用户ID
- 预测性别和置信度
- 性格类型和置信度
- 主导情绪
- 偏好语气
- 平均语速和音高
- 总交互次数

### 情绪历史 (EmotionHistory)
- 用户ID和会话ID
- 音频文件ID
- 检测到的情绪和强度
- 语气风格
- 识别的文本
- 时间戳

## 正确性属性

1. **特征提取一致性** - 相同音频多次提取应得到相同特征
2. **性别识别稳定性** - 足够样本后性别识别应保持稳定
3. **情绪识别合理性** - 识别的情绪应与语音特征相符
4. **标签置信度单调性** - 样本增加时置信度应趋于稳定
5. **情感语音一致性** - 生成的语音应与配置的情感匹配
6. **音色选择适配性** - 音色应与用户特征匹配
7. **语速调整合理性** - 调整应在合理范围内
8. **情感强度限制** - 强度应限制在0-1范围
9. **标签更新幂等性** - 相同输入多次更新应产生相同结果
10. **数据加密完整性** - 加密后解密应得到原始数据

## 安全和隐私

- 语音文件加密存储
- 敏感标签数据加密
- 用户只能访问自己的数据
- 支持完全删除用户数据
- 导出数据时PII匿名化
- 详细的审计日志

## 性能要求

- 特征提取: <3秒
- 情感识别: <2秒
- 语音合成: <3秒
- 并发支持: ≥10用户

## 技术栈

- **后端**: Spring Boot, Java 11+, MySQL 8.0+
- **前端**: Vue 3, Pinia, ECharts
- **语音**: 豆包API或Mock服务
- **测试**: JUnit 5, Vitest, fast-check

## 依赖项目

- voice-interaction: 基础语音功能
- personality-analysis: 用户画像系统

## 开发指南

### 环境准备

1. 确保MySQL数据库运行
2. 确保语音服务可用（豆包或Mock）
3. 安装Java 11+和Maven
4. 安装Node.js 16+和npm

### 开发流程

1. 创建数据库表（任务1）
2. 实现后端服务（任务3-13）
3. 实现REST API（任务14-16）
4. 实现前端界面（任务17-22）
5. 集成和测试（任务32-34）

### 测试

```bash
# 后端测试
cd app-device
mvn test

# 前端测试
cd app-web
npm test

# 集成测试
./scripts/test-emotional-voice.sh
```

## 未来扩展

1. **深度学习模型** - 更准确的情感识别
2. **实时流式处理** - 实时语音流分析
3. **多模态融合** - 结合面部表情分析
4. **个性化训练** - 为每个用户训练专属模型
5. **情感对话策略** - 根据情感调整对话策略

## 贡献指南

1. 从tasks.md选择一个任务
2. 创建功能分支
3. 实现功能并编写测试
4. 提交代码并创建PR
5. 代码审查通过后合并

## 许可证

与主项目相同

## 联系方式

如有问题，请联系项目维护者。

---

**状态**: 📝 规划阶段  
**创建时间**: 2024-11-30  
**最后更新**: 2024-11-30
