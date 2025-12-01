# 语音交互功能执行指南

## 开始之前

### 前置条件

1. **环境准备**
   - JDK 11+
   - Node.js 16+
   - MySQL 8.0+
   - Redis 6.0+
   - Maven 3.6+

2. **账号准备**
   - 阿里云账号（语音服务）
   - 腾讯云账号（备用）
   - Azure账号（可选）

3. **文档阅读**
   - 完整阅读requirements.md
   - 完整阅读design.md
   - 理解tasks.md中的任务依赖

### 开发环境配置

```bash
# 1. 克隆项目
cd /path/to/voicebox

# 2. 配置数据库
mysql -u root -p < .kiro/specs/voice-interaction/init-voice-tables.sql

# 3. 配置语音服务密钥
cp config.properties.example config.properties
# 编辑config.properties，添加：
# voice.stt.aliyun.appkey=your_appkey
# voice.stt.aliyun.accesskey=your_accesskey
# voice.tts.aliyun.appkey=your_appkey
# voice.tts.aliyun.accesskey=your_accesskey

# 4. 创建音频存储目录
mkdir -p /data/voicebox/audio
chmod 755 /data/voicebox/audio

# 5. 安装前端依赖
cd app-web
npm install
```

## 执行流程

### 第一周：后端核心功能（任务1-6）

#### Day 1-2: 基础架构

**任务1: 搭建基础架构和数据模型**

```bash
# 1. 创建数据库表
cd app-device/src/main/resources/db/migration
# 创建 V1.5__voice_tables.sql

# 2. 创建实体类
cd app-device/src/main/java/com/example/voicebox/app/device/domain
# 创建 VoiceMessage.java

# 3. 创建Repository
cd app-device/src/main/java/com/example/voicebox/app/device/repository
# 创建 VoiceMessageRepository.java
# 创建 VoiceServiceLogRepository.java

# 4. 运行测试
mvn test -Dtest=VoiceMessageRepositoryTest
```

**检查点**:
- [ ] 数据库表创建成功
- [ ] 实体类编译通过
- [ ] Repository测试通过

#### Day 3-4: 服务代理层

**任务2: 实现语音服务代理层**

```bash
# 1. 创建服务接口
cd app-device/src/main/java/com/example/voicebox/app/device/service/voice
# 创建 VoiceServiceProxy.java
# 创建 AliCloudVoiceService.java
# 创建 TencentVoiceService.java

# 2. 实现降级逻辑
# 创建 VoiceDegradationService.java

# 3. 运行属性测试
mvn test -Dtest=VoiceServiceProxyPropertyTest
```

**检查点**:
- [ ] STT服务调用成功
- [ ] TTS服务调用成功
- [ ] 服务降级测试通过
- [ ] 重试机制测试通过

#### Day 5: 存储服务

**任务3: 实现语音存储服务**

```bash
# 1. 创建存储服务
cd app-device/src/main/java/com/example/voicebox/app/device/service/voice
# 创建 VoiceStorageService.java

# 2. 实现文件操作
# - saveAudioFile()
# - loadAudioFile()
# - deleteAudioFile()
# - cleanupOldFiles()

# 3. 运行属性测试
mvn test -Dtest=VoiceStoragePropertyTest
```

**检查点**:
- [ ] 文件保存成功
- [ ] 文件读取成功
- [ ] 存储完整性测试通过
- [ ] 文件清理测试通过

#### Day 6-7: 输入输出服务

**任务4-5: 实现语音输入输出服务**

```bash
# 1. 创建输入服务
# 创建 VoiceInputService.java

# 2. 创建输出服务
# 创建 VoiceOutputService.java

# 3. 运行测试
mvn test -Dtest=VoiceInputServiceTest
mvn test -Dtest=VoiceOutputServiceTest
```

**检查点**:
- [ ] 音频上传处理成功
- [ ] STT识别成功
- [ ] TTS合成成功
- [ ] 个性化音色选择正确

#### Day 8: REST API

**任务6: 实现REST API控制器**

```bash
# 1. 创建控制器
cd app-device/src/main/java/com/example/voicebox/app/device/controller
# 创建 VoiceController.java

# 2. 运行集成测试
mvn test -Dtest=VoiceIntegrationTest

# 3. 手动测试API
curl -X POST http://localhost:10088/api/voice/upload \
  -F "file=@test.mp3" \
  -F "userId=1" \
  -F "sessionId=1"
```

**检查点**:
- [ ] 上传接口正常
- [ ] 合成接口正常
- [ ] 下载接口正常
- [ ] 集成测试通过

### 第二周：前端组件（任务7-10）

#### Day 9-10: 语音输入组件

**任务7: 实现前端语音输入组件**

```bash
cd app-web/src/components/voice

# 1. 创建组件
# 创建 VoiceInput.vue

# 2. 创建Composable
cd app-web/src/composables
# 创建 useVoiceInput.js

# 3. 测试组件
npm run dev
# 在浏览器中测试录音功能
```

**检查点**:
- [ ] 麦克风权限请求正常
- [ ] 录音功能正常
- [ ] 波形动画显示正常
- [ ] 时长限制生效
- [ ] 上传功能正常

#### Day 11: 语音播放组件

**任务8: 实现前端语音播放组件**

```bash
# 1. 创建组件
cd app-web/src/components/voice
# 创建 VoicePlayer.vue

# 2. 创建Composable
cd app-web/src/composables
# 创建 useVoicePlayer.js

# 3. 测试播放
npm run dev
```

**检查点**:
- [ ] 播放控制正常
- [ ] 进度条显示正确
- [ ] 暂停恢复正常
- [ ] 播放完成事件触发

#### Day 12-13: 集成到聊天界面

**任务10: 集成到聊天界面**

```bash
# 1. 修改InputArea组件
cd app-web/src/components/chat
# 编辑 InputArea.vue，添加语音按钮

# 2. 修改MessageItem组件
# 编辑 MessageItem.vue，添加播放器

# 3. 端到端测试
npm run dev
# 完整测试语音聊天流程
```

**检查点**:
- [ ] 语音按钮显示正常
- [ ] 录音发送消息成功
- [ ] AI回复带语音
- [ ] 历史消息可播放
- [ ] 会话切换暂停播放

### 第三周：高级功能（任务11-15）

#### Day 14-15: 多语言和流式播放

**任务11-12: 实现多语言和流式播放**

```bash
# 1. 添加语言选择UI
cd app-web/src/components/voice
# 创建 LanguageSelector.vue

# 2. 实现流式播放
cd app-device/src/main/java/com/example/voicebox/app/device/service/voice
# 创建 StreamingVoiceService.java

# 3. 测试
mvn test -Dtest=StreamingVoiceTest
```

**检查点**:
- [ ] 语言切换正常
- [ ] 流式播放流畅
- [ ] 音频连续性正确

#### Day 16-17: 错误处理和监控

**任务13-14: 实现错误处理和监控**

```bash
# 1. 创建错误处理器
cd app-device/src/main/java/com/example/voicebox/app/device/service/voice
# 创建 VoiceErrorHandler.java

# 2. 创建监控组件
# 创建 VoiceMetrics.java

# 3. 配置告警
# 编辑 application.yml
```

**检查点**:
- [ ] 各类错误处理正确
- [ ] 降级策略生效
- [ ] 监控指标记录正常
- [ ] 告警规则配置完成

#### Day 18: 辅助功能

**任务15: 实现语音辅助功能**

```bash
# 1. 添加字幕组件
cd app-web/src/components/voice
# 创建 VoiceSubtitle.vue

# 2. 添加控制面板
# 创建 VoiceControls.vue

# 3. 测试辅助功能
npm run dev
```

**检查点**:
- [ ] 字幕显示正常
- [ ] 语速调节生效
- [ ] 音量增强正常
- [ ] 降噪模式可用

### 第四周：优化和部署（任务16-18）

#### Day 19-20: 性能优化

**任务16: 性能优化**

```bash
# 1. 实现音频压缩
cd app-device/src/main/java/com/example/voicebox/app/device/util
# 创建 AudioCompressor.java

# 2. 实现缓存
cd app-device/src/main/java/com/example/voicebox/app/device/service/voice
# 编辑 VoiceOutputService.java，添加缓存

# 3. 运行性能测试
mvn test -Dtest=VoicePerformanceTest
```

**检查点**:
- [ ] 压缩质量达标
- [ ] 缓存命中率≥30%
- [ ] STT响应≤5秒
- [ ] TTS响应≤2秒

#### Day 21: 配置和部署

**任务17: 配置和部署**

```bash
# 1. 配置生产环境
cp config.properties config.properties.prod
# 编辑生产配置

# 2. 构建项目
mvn clean package -DskipTests
cd app-web && npm run build

# 3. 部署到服务器
./deploy/deploy-voice-feature.sh

# 4. 验证部署
curl https://your-domain.com/api/voice/health
```

**检查点**:
- [ ] 生产配置正确
- [ ] 构建成功
- [ ] 部署成功
- [ ] 健康检查通过

#### Day 22: 最终测试

**任务18: 最终检查点**

```bash
# 1. 运行所有测试
mvn clean test
cd app-web && npm run test

# 2. 手动测试所有功能
# - 语音输入
# - 语音输出
# - 多语言
# - 流式播放
# - 错误处理
# - 辅助功能

# 3. 性能测试
# - 并发测试
# - 压力测试
# - 长时间运行测试

# 4. 生成测试报告
mvn jacoco:report
```

**检查点**:
- [ ] 所有单元测试通过
- [ ] 所有属性测试通过
- [ ] 所有集成测试通过
- [ ] 性能指标达标
- [ ] 无严重bug

## 常见问题

### 1. STT识别准确率低

**原因**:
- 音频质量差
- 背景噪音大
- 语言模型不匹配

**解决方案**:
```java
// 提高采样率
recorder.setSampleRate(44100);

// 启用降噪
audioProcessor.enableNoiseReduction();

// 选择正确的语言模型
sttService.setLanguage("zh-CN");
```

### 2. TTS语音不自然

**原因**:
- 音色选择不当
- 语速设置不合理
- 文本格式问题

**解决方案**:
```java
// 调整语音参数
VoiceProfile profile = new VoiceProfile();
profile.setVoiceName("xiaoyun");  // 更自然的音色
profile.setSpeechRate(1.0);       // 正常语速
profile.setPitch(1.0);            // 正常音调

// 预处理文本
String processedText = textProcessor.normalize(text);
```

### 3. 音频播放卡顿

**原因**:
- 网络延迟
- 缓冲不足
- 文件过大

**解决方案**:
```javascript
// 增加缓冲
audio.preload = 'auto'

// 使用流式播放
const stream = await fetch(audioUrl)
const reader = stream.body.getReader()

// 压缩音频
ffmpeg -i input.wav -b:a 64k output.mp3
```

### 4. 服务调用失败

**原因**:
- API密钥错误
- 配额用尽
- 网络问题

**解决方案**:
```java
// 检查配置
logger.info("API Key: {}", apiKey.substring(0, 10) + "...");

// 检查配额
long remaining = quotaService.getRemainingQuota();
logger.info("Remaining quota: {}", remaining);

// 启用降级
if (primaryService.isDown()) {
    return backupService.call();
}
```

## 测试清单

### 功能测试

- [ ] 语音录制正常
- [ ] 语音识别准确
- [ ] 语音合成自然
- [ ] 语音播放流畅
- [ ] 多语言切换正常
- [ ] 个性化音色正确
- [ ] 历史消息可播放
- [ ] 错误提示友好

### 性能测试

- [ ] STT响应时间≤5秒
- [ ] TTS响应时间≤2秒
- [ ] 文件上传速度正常
- [ ] 播放启动时间≤500ms
- [ ] 并发100用户无问题
- [ ] 内存使用正常
- [ ] CPU使用正常

### 兼容性测试

- [ ] Chrome浏览器正常
- [ ] Firefox浏览器正常
- [ ] Safari浏览器正常
- [ ] Edge浏览器正常
- [ ] 移动端浏览器正常
- [ ] 不同操作系统正常

### 安全测试

- [ ] 权限控制正常
- [ ] 文件上传限制生效
- [ ] API密钥加密存储
- [ ] 敏感数据脱敏
- [ ] SQL注入防护
- [ ] XSS攻击防护

## 交付物

### 代码
- [ ] 后端代码（Java）
- [ ] 前端代码（Vue）
- [ ] 测试代码
- [ ] 配置文件

### 文档
- [ ] API文档
- [ ] 部署文档
- [ ] 运维文档
- [ ] 用户手册

### 测试报告
- [ ] 单元测试报告
- [ ] 集成测试报告
- [ ] 性能测试报告
- [ ] 覆盖率报告

## 后续优化

### 短期（1-2周）
- 优化识别准确率
- 减少响应延迟
- 提升音质

### 中期（1-2月）
- 支持更多语言
- 添加更多音色
- 实现离线模式

### 长期（3-6月）
- 实时语音对话
- 语音情感识别
- 声纹识别

---

**祝开发顺利！**
