# 语音监控错误分析

## 🔍 错误原因

### 错误信息
```
java.nio.file.FileSystemException: /data: Read-only file system
```

### 错误统计
- **总错误数**: 8 个
- **错误类型**: INTERNAL_ERROR
- **失败率**: 88.89%

## 📊 问题分析

### 根本原因

`VoiceStorageService` 尝试将语音文件保存到 `/data/voicebox/audio` 目录，但：

1. **macOS 系统限制**: `/data` 目录在 macOS 上是只读的
2. **权限问题**: 应用没有权限在 `/data` 创建目录
3. **配置问题**: 默认路径不适合本地开发环境

### 错误发生位置

```java
// VoiceStorageService.java:78
Files.createDirectories(storagePath.getParent());
```

当系统尝试创建 `/data/voicebox/audio/user_1/` 目录时失败。

### 为什么有 8 个错误？

测试脚本发送了 9 个 TTS 请求：
- 5 个来自第一轮测试
- 4 个来自语言测试
- 其中 8 个失败（可能第一个成功或有其他原因）

每个失败的请求都因为无法写入文件而报错。

## 💡 解决方案

### 方案 1：修改配置文件（推荐）

在 `config.properties` 或 `application.properties` 中添加：

```properties
# 语音存储路径（使用项目目录）
voice.storage.base-path=./voice-box-uploads
```

或者使用系统临时目录：

```properties
# 使用系统临时目录
voice.storage.base-path=${java.io.tmpdir}/voicebox/audio
```

### 方案 2：创建可写目录

```bash
# 在项目根目录创建
mkdir -p voice-box-uploads

# 或者使用用户目录
mkdir -p ~/voicebox/audio
```

然后配置：
```properties
voice.storage.base-path=./voice-box-uploads
# 或
voice.storage.base-path=${user.home}/voicebox/audio
```

### 方案 3：修改代码默认值

修改 `VoiceStorageService.java`：

```java
@Value("${voice.storage.base-path:./voice-box-uploads}")
private String basePath;
```

将默认值从 `/data/voicebox/audio` 改为 `./voice-box-uploads`。

## 🔧 快速修复

### 1. 创建配置文件

创建或修改 `config.properties`：

```bash
cat >> config.properties << 'EOF'

# 语音存储配置
voice.storage.base-path=./voice-box-uploads
voice.storage.max-file-size=10485760
EOF
```

### 2. 创建存储目录

```bash
mkdir -p voice-box-uploads
```

### 3. 重启后端

```bash
# 停止后端
# 重新启动
cd app-device
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=10088"
```

### 4. 验证修复

```bash
# 运行测试
./scripts/test-monitoring-data.sh

# 检查错误率
curl http://localhost:10088/api/voice/monitoring/report | python3 -c "import sys, json; data=json.load(sys.stdin); print(f'错误率: {data[\"overall\"][\"failureRate\"]}%')"
```

## 📈 修复后的预期结果

### 修复前
```json
{
  "overall": {
    "successRate": 0.0,
    "failureRate": 88.89,
    "totalRequests": 9
  },
  "errors": {
    "totalErrors": 8,
    "commonErrors": {
      "INTERNAL_ERROR": 8
    }
  }
}
```

### 修复后
```json
{
  "overall": {
    "successRate": 100.0,
    "failureRate": 0.0,
    "totalRequests": 9
  },
  "errors": {
    "totalErrors": 0,
    "commonErrors": {}
  }
}
```

## 🎯 其他可能的错误

### 1. 豆包 API 配置问题

如果使用真实的豆包服务而不是 Mock：

```
Caused by: java.net.ProtocolException: Expected HTTP 101 response but was '404 Not Found'
```

**原因**: 豆包 WebSocket 连接失败

**解决**: 
- 检查豆包 API 配置
- 确认 API Key 和 Endpoint 正确
- 或者继续使用 Mock 服务

### 2. 数据库连接问题

```
Caused by: java.sql.SQLSyntaxErrorException: Access denied for user 'voicebox'@'%' to database 'voicebox_db'
```

**原因**: 数据库权限问题

**解决**: 参考 `docs/DATABASE_SETUP_GUIDE.md`

## 📝 监控数据说明

### 错误统计的意义

监控系统记录的错误数据帮助我们：

1. **快速发现问题** - 88.89% 的失败率立即引起注意
2. **定位错误类型** - INTERNAL_ERROR 指向系统内部问题
3. **追踪错误趋势** - 持续监控错误率变化
4. **优化系统** - 根据错误数据改进代码

### 这就是真实监控的价值

- ✅ 真实数据暴露了文件系统配置问题
- ✅ 错误统计帮助快速定位问题
- ✅ 监控指标指导系统优化

如果使用模拟数据，这个问题可能会被隐藏，直到生产环境才发现！

## 🚀 总结

**8 个内部错误的原因**：
- 文件系统只读：`/data` 目录在 macOS 上无法写入
- 配置不当：默认路径不适合本地开发
- 权限问题：应用无权限创建目录

**解决方法**：
1. 修改配置使用可写目录
2. 创建存储目录
3. 重启服务验证

**监控价值**：
- 真实数据帮助发现配置问题
- 错误统计指导问题修复
- 这就是为什么需要真实监控数据！
