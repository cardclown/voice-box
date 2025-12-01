# 豆包语音服务集成说明

## 概述

本文档说明如何集成豆包（Doubao）实时语音交互服务到VoiceBox系统。

## 已完成的工作

### 1. 配置文件更新

在 `config.properties` 中添加了豆包语音服务配置：

```properties
# 豆包实时语音交互配置
voicebox.doubao.voice.appid=7112763635
voicebox.doubao.voice.token=xfjd9wi3AgzAmFVBckiWad9437lcx2HB
voicebox.doubao.voice.secret=NiiqP5oNG8uaUNsbaoC1PdQDL_ORqn46
voicebox.doubao.voice.url=wss://openspeech.bytedance.com/api/v1/tts
voicebox.doubao.voice.stt.url=wss://openspeech.bytedance.com/api/v1/asr
```

### 2. 核心服务实现

#### DoubaoVoiceService
- 位置：`app-device/src/main/java/com/example/voicebox/app/device/service/voice/DoubaoVoiceService.java`
- 功能：
  - 语音识别（STT）：通过WebSocket实时识别语音
  - 语音合成（TTS）：通过WebSocket实时合成语音
  - 流式语音合成：支持实时播放场景
  - 签名生成：使用HmacSHA256算法生成安全签名

#### VoiceServiceProxy
- 位置：`app-device/src/main/java/com/example/voicebox/app/device/service/voice/VoiceServiceProxy.java`
- 功能：
  - 服务代理和降级
  - 自动重试（指数退避策略）
  - 错误处理

#### VoiceInputService
- 位置：`app-device/src/main/java/com/example/voicebox/app/device/service/voice/VoiceInputService.java`
- 功能：
  - 音频文件上传处理
  - 文件验证（大小、格式）
  - 调用STT服务
  - 保存语音消息记录

#### VoiceOutputService
- 位置：`app-device/src/main/java/com/example/voicebox/app/device/service/voice/VoiceOutputService.java`
- 功能：
  - 文本转语音
  - 个性化音色选择（基于用户画像）
  - 流式语音合成
  - 保存语音消息记录

#### VoiceController
- 位置：`app-device/src/main/java/com/example/voicebox/app/device/controller/VoiceController.java`
- 功能：
  - REST API接口
  - POST /api/voice/upload - 上传语音文件
  - POST /api/voice/synthesize - 合成语音
  - GET /api/voice/audio/{fileId} - 获取音频文件

### 3. 数据模型

#### VoiceMessage实体
- 位置：`app-device/src/main/java/com/example/voicebox/app/device/domain/VoiceMessage.java`
- 字段：
  - 基本信息：userId, sessionId, messageId
  - 文件信息：fileId, filePath, fileSize, duration, format
  - 识别信息：recognizedText, confidence, language
  - 类型标识：isInput（true=用户输入，false=AI输出）

#### VoiceMessageRepository
- 位置：`app-device/src/main/java/com/example/voicebox/app/device/repository/VoiceMessageRepository.java`
- 功能：
  - 根据fileId查找
  - 根据userId查找所有消息
  - 根据sessionId查找所有消息
  - 查找过期消息（用于清理）

### 4. 数据库表

创建了数据库表结构：
- `voice_messages` - 语音消息表
- `voice_service_logs` - 语音服务调用日志表

SQL文件位置：`deploy/db/schema/04-voice-tables.sql`

## 技术特点

### 1. WebSocket实时通信
- 使用Java 11+ HttpClient的WebSocket API
- 支持异步处理
- 自动重连机制

### 2. 安全认证
- 使用HmacSHA256签名算法
- 时间戳防重放
- Token + Secret双重验证

### 3. 个性化音色
根据用户画像自动选择音色：
- 外向性 > 0.6：活力音色（zh_female_huoli）
- 外向性 ≤ 0.6：清新音色（zh_female_qingxin）

### 4. 错误处理
- 指数退避重试（1s, 2s, 4s）
- 最多重试3次
- 友好的错误提示

### 5. 流式处理
- 支持流式语音合成
- 实时播放，无需等待完整生成
- 音频片段缓冲

## API使用示例

### 上传语音并识别

```bash
curl -X POST http://localhost:10088/api/voice/upload \
  -F "file=@audio.mp3" \
  -F "userId=1" \
  -F "sessionId=1" \
  -F "language=zh-CN"
```

响应：
```json
{
  "success": true,
  "fileId": "abc123...",
  "recognizedText": "你好，这是一段测试语音",
  "confidence": 0.95,
  "duration": 5
}
```

### 合成语音

```bash
curl -X POST http://localhost:10088/api/voice/synthesize \
  -H "Content-Type: application/json" \
  -d '{
    "text": "你好，我是AI助手",
    "userId": 1,
    "language": "zh-CN",
    "voiceName": "zh_female_qingxin"
  }'
```

响应：
```json
{
  "success": true,
  "fileId": "def456...",
  "audioUrl": "/api/voice/audio/def456...",
  "duration": 3
}
```

### 获取音频文件

```bash
curl http://localhost:10088/api/voice/audio/def456... -o output.mp3
```

## 配置说明

### 必需配置

在 `config.properties` 中配置：

```properties
# 豆包AppID
voicebox.doubao.voice.appid=YOUR_APP_ID

# 豆包Token
voicebox.doubao.voice.token=YOUR_TOKEN

# 豆包Secret
voicebox.doubao.voice.secret=YOUR_SECRET
```

### 可选配置

```properties
# 主服务提供商（默认：doubao）
voicebox.voice.primary.provider=doubao

# 最大重试次数（默认：3）
voicebox.voice.max.retries=3

# 文件存储路径（默认：/data/voicebox/audio）
voice.storage.base-path=/data/voicebox/audio

# 最大文件大小（默认：10MB）
voice.storage.max-file-size=10485760
```

## 支持的语言

- zh-CN：中文（普通话）
- en-US：英语（美国）
- ja-JP：日语
- ko-KR：韩语

## 支持的音色

### 中文音色
- zh_female_qingxin：清新女声（默认）
- zh_female_huoli：活力女声
- zh_male_wenhe：温和男声
- zh_male_chenwen：沉稳男声

## 下一步工作

### 前端集成（待实现）
1. 创建语音输入组件（VoiceInput.vue）
2. 创建语音播放组件（VoicePlayer.vue）
3. 集成到聊天界面
4. 实现录音功能
5. 实现播放控制

### 测试（待实现）
1. 单元测试
2. 集成测试
3. 属性测试
4. 性能测试

### 优化（待实现）
1. 音频压缩
2. 缓存机制
3. 预加载
4. 监控和日志

## 部署步骤

### 1. 数据库初始化

```bash
# 执行数据库脚本
mysql -u root -p voicebox_db < deploy/db/schema/04-voice-tables.sql
```

### 2. 创建存储目录

```bash
# 创建音频存储目录
sudo mkdir -p /data/voicebox/audio
sudo chown -R voicebox:voicebox /data/voicebox/audio
sudo chmod 755 /data/voicebox/audio
```

### 3. 配置文件

确保 `config.properties` 中包含豆包配置。

### 4. 重启服务

```bash
cd /opt/voicebox
./restart-all.sh
```

### 5. 验证

```bash
# 检查服务状态
./status.sh

# 测试API
curl http://localhost:10088/api/voice/upload --help
```

## 故障排查

### 问题1：WebSocket连接失败

**症状**：日志显示"WebSocket连接失败"

**解决方案**：
1. 检查网络连接
2. 验证AppID、Token、Secret是否正确
3. 检查防火墙设置

### 问题2：语音识别失败

**症状**：返回"[识别失败]"

**解决方案**：
1. 检查音频格式是否支持
2. 检查音频质量
3. 查看详细错误日志

### 问题3：文件保存失败

**症状**：上传成功但无法播放

**解决方案**：
1. 检查存储目录权限
2. 检查磁盘空间
3. 查看日志中的文件路径

## 安全注意事项

1. **不要将配置文件提交到Git**
   - `config.properties` 已在 `.gitignore` 中
   - 使用环境变量或密钥管理服务

2. **定期更新Token和Secret**
   - 建议每3个月更新一次
   - 更新后重启服务

3. **限制API访问**
   - 添加认证中间件
   - 限制请求频率
   - 记录访问日志

4. **加密存储**
   - 考虑对敏感音频文件加密
   - 使用HTTPS传输

## 参考资料

- 豆包语音服务文档：https://www.volcengine.com/docs/6561/79820
- WebSocket API文档：https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/WebSocket.html
- Spring Boot文件上传：https://spring.io/guides/gs/uploading-files/
