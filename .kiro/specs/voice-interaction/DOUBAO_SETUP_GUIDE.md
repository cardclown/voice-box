# 豆包语音服务配置指南

## 概述

本指南帮助你配置豆包语音服务，使语音识别和合成功能正常工作。

## 前置条件

1. **豆包账号**: 需要在豆包开放平台注册账号
2. **API密钥**: 获取语音服务的API密钥
3. **网络访问**: 确保服务器可以访问豆包API

## 配置步骤

### 1. 获取API密钥

1. 访问豆包开放平台: https://www.volcengine.com/
2. 注册并登录账号
3. 进入"语音技术"服务
4. 创建应用并获取以下信息：
   - App ID
   - Access Key
   - Secret Key

### 2. 配置应用

编辑 `config.properties` 文件：

```properties
# 豆包语音服务配置
doubao.voice.app.id=你的AppID
doubao.voice.access.key=你的AccessKey
doubao.voice.secret.key=你的SecretKey

# STT配置
doubao.voice.stt.url=wss://openspeech.bytedance.com/api/v1/stt
doubao.voice.stt.language=zh-CN
doubao.voice.stt.format=wav

# TTS配置
doubao.voice.tts.url=wss://openspeech.bytedance.com/api/v1/tts
doubao.voice.tts.voice=zh_female_qingxin
doubao.voice.tts.speed=1.0
doubao.voice.tts.volume=1.0

# 文件存储配置
voicebox.voice.storage.path=/opt/voicebox/voice-files
voicebox.voice.max.file.size=10485760
```

### 3. 测试连接

启动应用后，使用测试脚本验证连接：

```bash
# 测试语音合成
curl -X POST http://localhost:10088/api/voice/synthesize \
  -H "Content-Type: application/json" \
  -d '{
    "text": "你好，这是测试",
    "userId": 1,
    "language": "zh-CN",
    "voiceName": "zh_female_qingxin"
  }'

# 测试语音识别（需要准备音频文件）
curl -X POST http://localhost:10088/api/voice/upload \
  -F "file=@test.wav" \
  -F "userId=1" \
  -F "sessionId=1" \
  -F "language=zh-CN"
```

## 常见问题

### 1. 连接超时

**问题**: `Failed to connect to openspeech.bytedance.com`

**解决方案**:
- 检查网络连接
- 确认服务器可以访问外网
- 检查防火墙设置
- 可能需要配置代理

### 2. 认证失败

**问题**: `Authentication failed`

**解决方案**:
- 检查API密钥是否正确
- 确认密钥是否已激活
- 检查应用是否有语音服务权限

### 3. 音频格式不支持

**问题**: `Unsupported audio format`

**解决方案**:
- 确保音频格式为WAV、MP3或WebM
- 检查采样率（推荐16000Hz）
- 检查文件大小（不超过10MB）

### 4. 识别结果为空

**问题**: 语音识别返回空文本

**解决方案**:
- 检查音频质量
- 确认语言设置正确
- 检查音频时长（至少1秒）
- 查看后端日志获取详细错误

## 网络配置

### 使用代理

如果服务器需要通过代理访问外网：

```properties
# 添加到config.properties
http.proxyHost=proxy.example.com
http.proxyPort=8080
https.proxyHost=proxy.example.com
https.proxyPort=8080
```

### 防火墙配置

确保以下端口开放：
- 443 (HTTPS)
- 用于WebSocket连接

## 监控和日志

### 查看日志

```bash
# 查看应用日志
tail -f logs/voicebox.log

# 查看语音服务日志
grep "VoiceService" logs/voicebox.log

# 查看错误日志
grep "ERROR" logs/voicebox.log | grep "voice"
```

### 监控指标

检查以下指标：
- STT成功率
- TTS成功率
- 平均响应时间
- 错误率

## 性能优化

### 1. 连接池配置

```properties
# OkHttp连接池
okhttp.connection.pool.max.idle=5
okhttp.connection.pool.keep.alive=300
```

### 2. 超时配置

```properties
# 连接超时
doubao.voice.connect.timeout=10000
# 读取超时
doubao.voice.read.timeout=30000
# 写入超时
doubao.voice.write.timeout=30000
```

### 3. 重试配置

```properties
# 最大重试次数
doubao.voice.max.retries=3
# 重试间隔（毫秒）
doubao.voice.retry.interval=1000
```

## 安全建议

1. **不要将API密钥提交到版本控制**
   - 使用环境变量
   - 使用配置管理工具

2. **定期轮换密钥**
   - 建议每3个月更换一次

3. **限制API调用频率**
   - 设置合理的限流策略

4. **加密敏感数据**
   - 音频文件加密存储
   - 传输使用HTTPS

## 下一步

配置完成后：
1. 重启应用
2. 运行测试脚本
3. 检查日志确认连接成功
4. 在前端测试语音功能

## 支持

如遇问题，请查看：
- 豆包开放平台文档
- 项目日志文件
- GitHub Issues

---

**注意**: 豆包服务可能需要付费，请确认你的账号有足够的配额。
