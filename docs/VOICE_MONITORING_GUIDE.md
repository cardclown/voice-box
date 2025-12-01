# 语音监控系统说明

## 概述

语音监控系统提供实时的语音服务使用情况和性能指标。所有数据都是**真实收集**的，而不是模拟数据。

## 数据来源

### 真实数据收集

监控数据通过 `VoiceMetricsService` 实时收集，每次语音请求都会被记录：

1. **STT（语音转文字）请求**
   - 在 `VoiceController.uploadVoice()` 中记录
   - 记录请求开始、成功/失败、响应时间

2. **TTS（文字转语音）请求**
   - 在 `VoiceController.synthesizeVoice()` 中记录
   - 记录请求开始、成功/失败、响应时间

3. **用户活跃度**
   - 自动跟踪每个用户的请求次数
   - 统计活跃用户数量

4. **语言使用统计**
   - 记录每种语言的使用次数
   - 自动识别最常用语言

5. **错误统计**
   - 记录所有错误类型和次数
   - 分类统计不同的错误原因

## 监控指标说明

### 整体指标 (Overall Metrics)

- **successRate**: 成功率（%）- 成功请求数 / 总请求数
- **avgResponseTime**: 平均响应时间（ms）
- **totalRequests**: 总请求数
- **failureRate**: 失败率（%）
- **uptimeMinutes**: 系统运行时间（分钟）

### STT 指标

- **accuracy**: STT 准确率（%）- 等同于成功率
- **avgProcessingTime**: 平均处理时间（ms）
- **totalProcessed**: 总处理次数

### TTS 指标

- **quality**: TTS 质量（%）- 等同于成功率
- **avgGenerationTime**: 平均生成时间（ms）
- **totalGenerated**: 总生成次数

### 用户指标

- **activeUsers**: 活跃用户数 - 发起过请求的用户数
- **newUsers**: 新用户数（暂未实现）
- **avgSessionDuration**: 平均会话时长（暂未实现）

### 语言指标

- **supportedLanguages**: 支持的语言数量
- **mostUsedLanguage**: 最常用语言
- **languageDistribution**: 各语言使用分布

### 错误指标

- **totalErrors**: 总错误数
- **errorRate**: 错误率（%）
- **commonErrors**: 常见错误类型及次数

### 性能指标

- **cpuUsage**: CPU 使用率（暂未实现）
- **memoryUsage**: 内存使用率（暂未实现）
- **diskUsage**: 磁盘使用率（暂未实现）
- **networkLatency**: 网络延迟（ms）- 等同于平均响应时间

## 为什么初始数据都是 0？

系统刚启动时，还没有任何语音请求，所以所有指标都是 0。这是**正常现象**。

当用户开始使用语音功能后：
- 上传语音文件进行识别
- 请求文字转语音
- 使用流式语音播放

监控数据就会开始累积，显示真实的使用情况。

## 如何测试监控数据

### 1. 使用语音交互页面

访问 `http://localhost:5173/voice-interaction`，进行以下操作：

- 录制并上传语音
- 请求语音合成
- 播放语音

### 2. 直接调用 API

```bash
# STT 测试
curl -X POST http://localhost:10088/api/voice/upload \
  -F "file=@test.wav" \
  -F "userId=1" \
  -F "sessionId=1" \
  -F "language=zh-CN"

# TTS 测试
curl -X POST http://localhost:10088/api/voice/synthesize \
  -H "Content-Type: application/json" \
  -d '{
    "text": "你好，这是测试",
    "userId": 1,
    "language": "zh-CN"
  }'
```

### 3. 查看监控数据

```bash
# 查看完整报告
curl http://localhost:10088/api/voice/monitoring/report

# 查看健康状态
curl http://localhost:10088/api/voice/monitoring/health

# 查看整体指标
curl http://localhost:10088/api/voice/monitoring/metrics/overall
```

## 数据持久化

目前监控数据存储在内存中，服务重启后会清零。如果需要持久化：

1. **数据库存储**
   - 定期将指标写入数据库
   - 支持历史数据查询

2. **时序数据库**
   - 使用 InfluxDB 或 Prometheus
   - 更适合时间序列数据

3. **日志分析**
   - 通过日志收集系统（ELK）
   - 从日志中提取指标

## 监控数据的用途

1. **性能优化**
   - 识别响应时间瓶颈
   - 优化慢速操作

2. **容量规划**
   - 了解用户使用模式
   - 预测资源需求

3. **问题诊断**
   - 快速发现错误趋势
   - 定位问题根源

4. **用户体验**
   - 监控服务质量
   - 确保高可用性

## API 端点

### 监控报告

- `GET /api/voice/monitoring/report` - 完整监控报告
- `GET /api/voice/monitoring/health` - 健康检查
- `GET /api/voice/monitoring/metrics/overall` - 整体指标
- `GET /api/voice/monitoring/metrics/stt` - STT 指标
- `GET /api/voice/monitoring/metrics/tts` - TTS 指标
- `GET /api/voice/monitoring/metrics/users` - 用户指标
- `GET /api/voice/monitoring/metrics/performance` - 性能指标

### 前端访问

前端通过 Vite 代理访问：

```javascript
// 自动代理到 http://localhost:10088
fetch('/api/voice/monitoring/report')
  .then(res => res.json())
  .then(data => console.log(data))
```

## 注意事项

1. **数据准确性**
   - 数据基于实际请求收集
   - 响应时间包含网络延迟

2. **性能影响**
   - 指标收集对性能影响极小
   - 使用原子操作和并发集合

3. **数据重置**
   - 服务重启会清空数据
   - 可以通过 API 手动重置（未实现）

4. **扩展性**
   - 支持添加自定义指标
   - 可以集成第三方监控系统

## 未来改进

1. **数据持久化** - 将指标存储到数据库
2. **历史数据** - 支持查询历史趋势
3. **告警系统** - 指标异常时发送告警
4. **可视化** - 更丰富的图表展示
5. **系统指标** - 添加 CPU、内存等系统级指标
6. **分布式追踪** - 集成 Zipkin 或 Jaeger
7. **实时推送** - 使用 WebSocket 推送实时数据

## 总结

语音监控系统提供的是**真实的运行数据**，而不是模拟数据。初始状态下数据为 0 是正常的，随着系统使用会逐渐累积真实的监控指标。
