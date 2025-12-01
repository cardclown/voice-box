# 语音交互功能规格文档

## 概述

本规格文档描述了VoiceBox系统的语音交互功能，包括语音输入（STT）和语音输出（TTS）能力，为用户提供更自然、便捷的AI交互体验。

## 文档结构

- **requirements.md** - 需求文档
  - 15个主要需求
  - 涵盖语音输入、输出、存储、质量控制、多语言、个性化等方面
  - 所有需求遵循EARS模式和INCOSE质量规则

- **design.md** - 设计文档
  - 整体架构（前端、后端、服务代理、存储）
  - 组件和接口设计
  - 数据模型设计
  - 15个正确性属性
  - 错误处理和降级策略
  - 测试策略
  - 部署和运维方案

- **tasks.md** - 实施计划
  - 18个主要任务
  - 包含核心功能、高级功能、优化和部署
  - 所有任务都是必需的（包括测试）
  - 估算工作量：17-24天

## 核心功能

### 语音输入
- 录音功能（WebRTC）
- 音频上传
- STT语音识别
- 识别结果编辑
- 录音时长限制（5分钟）

### 语音输出
- TTS语音合成
- 音频播放控制
- 播放进度显示
- 个性化音色选择
- 流式播放支持

### 语音存储
- 音频文件管理
- 元数据记录
- 历史消息回放
- 自动清理（90天）

### 质量保证
- 识别准确率≥95%
- 语音自然度≥4.0/5.0
- STT响应≤5秒
- TTS响应≤2秒

## 技术栈

### 前端
- Vue 3 + Composition API
- WebRTC MediaRecorder
- HTML5 Audio API
- Pinia状态管理

### 后端
- Spring Boot
- MySQL（元数据）
- 文件系统（音频文件）
- Redis（缓存）

### 语音服务
- 阿里云语音服务（主）
- 腾讯云语音服务（备用）
- Azure语音服务（备用）

## 开发流程

### 阶段1: 基础架构（任务1-6）
- 数据库表和实体类
- 语音服务代理
- 存储服务
- 输入输出服务
- REST API

### 阶段2: 前端组件（任务7-10）
- 语音输入组件
- 语音播放组件
- Composables
- 集成到聊天界面

### 阶段3: 高级功能（任务11-15）
- 多语言支持
- 流式播放
- 错误处理
- 监控日志
- 辅助功能

### 阶段4: 优化部署（任务16-18）
- 性能优化
- 配置部署
- 最终测试

## 测试策略

### 属性测试（15个）
使用JUnit-Quickcheck验证通用属性：
- 存储完整性
- 识别准确性
- 播放一致性
- 个性化匹配
- 服务降级等

### 单元测试
- 音频文件处理
- 服务代理逻辑
- 错误处理
- 指标记录

### 集成测试
- 完整输入流程
- 完整输出流程
- 端到端场景

### 性能测试
- STT响应时间
- TTS响应时间
- 文件上传速度
- 播放启动时间

## 里程碑

### MVP（最小可行产品）
完成任务1-10，实现基础语音输入输出功能

### 完整版
完成任务1-15，包含所有高级功能

### 优化版
完成所有任务，包括性能优化和生产部署

## 开始开发

1. 阅读需求文档了解功能范围
2. 阅读设计文档了解技术方案
3. 按照任务列表顺序开发
4. 每完成一个任务运行对应测试
5. 遇到问题参考设计文档的错误处理部分

## 相关文档

- [项目结构](../../../app-web/PROJECT_STRUCTURE.md)
- [API文档](../../../app-web/src/services/README_API.md)
- [数据库架构](../../../app-device/DATABASE_SCHEMA.md)
- [功能路线图](../../../docs/FEATURE_ROADMAP.md)

## 联系方式

如有问题，请参考：
- 需求文档中的验收标准
- 设计文档中的组件接口
- 任务列表中的实施说明

---

## 🎉 最新进展（2024-11-29）

### ✅ 后端核心功能已完成

**已实现的组件**：
1. ✅ **DoubaoVoiceService** - 豆包语音服务集成
   - WebSocket实时通信
   - STT语音识别
   - TTS语音合成
   - 流式语音合成
   - HmacSHA256安全签名

2. ✅ **VoiceServiceProxy** - 服务代理层
   - 服务降级和自动切换
   - 指数退避重试（1s, 2s, 4s）
   - 异常处理

3. ✅ **VoiceInputService** - 语音输入服务
   - 音频文件上传处理
   - 文件验证（大小、格式）
   - STT调用和文本识别
   - 识别结果保存

4. ✅ **VoiceOutputService** - 语音输出服务
   - TTS调用和语音合成
   - 个性化音色选择（基于用户画像）
   - 流式语音合成
   - 语音文件保存

5. ✅ **VoiceController** - REST API接口
   - POST /api/voice/upload - 上传语音并识别
   - POST /api/voice/synthesize - 文本转语音
   - GET /api/voice/audio/{fileId} - 获取音频文件

6. ✅ **数据模型和存储**
   - VoiceMessage实体
   - VoiceMessageRepository
   - 数据库表结构（voice_messages, voice_service_logs）
   - VoiceStorageService（文件存储）

**配置文件**：
- ✅ 豆包服务配置已添加到 `config.properties`
- ✅ 数据库迁移脚本：`deploy/db/schema/04-voice-tables.sql`

**文档**：
- ✅ **doubao-integration.md** - 豆包集成详细说明
- ✅ **deployment-checklist.md** - 部署检查清单
- ✅ **test-voice-api.sh** - API测试脚本

### 🔄 进行中

- 前端语音输入组件（任务7）
- 前端语音播放组件（任务8）
- 单元测试和集成测试（任务1.1-6.2）

### 📋 待完成

- 多语言支持（任务11）
- 流式播放UI（任务12）
- 辅助功能（任务15）
- 性能优化（任务16）
- 生产部署（任务17）

## 快速开始

### 1. 初始化数据库

```bash
mysql -u root -p voicebox_db < deploy/db/schema/04-voice-tables.sql
```

### 2. 配置已完成

豆包服务配置已在 `config.properties` 中：
```properties
voicebox.doubao.voice.appid=7112763635
voicebox.doubao.voice.token=xfjd9wi3AgzAmFVBckiWad9437lcx2HB
voicebox.doubao.voice.secret=NiiqP5oNG8uaUNsbaoC1PdQDL_ORqn46
```

### 3. 启动服务

```bash
./start-all.sh dev
```

### 4. 测试API

```bash
# 运行测试脚本
./scripts/test-voice-api.sh

# 或手动测试
curl -X POST http://localhost:10088/api/voice/synthesize \
  -H "Content-Type: application/json" \
  -d '{"text":"你好","userId":1,"language":"zh-CN"}'
```

## 新增文档

- 📄 [豆包集成说明](./doubao-integration.md) - API使用、配置、故障排查
- 📄 [部署检查清单](./deployment-checklist.md) - 完整的部署步骤和验证

## 技术特点

- **实时通信**：WebSocket双向通信，低延迟
- **智能重试**：指数退避策略，最多重试3次
- **个性化**：基于用户画像自动选择音色
- **流式处理**：支持流式语音合成，实时播放
- **安全认证**：HmacSHA256签名，防重放攻击
- **完善错误处理**：友好的错误提示和降级方案

---

**版本**: 1.1  
**创建日期**: 2024-11-29  
**更新日期**: 2024-11-29  
**状态**: 后端核心功能已完成，前端开发中
