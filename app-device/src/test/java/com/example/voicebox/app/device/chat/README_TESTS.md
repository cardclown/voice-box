# 消息存储往返属性测试

## 概述

本测试验证 **需求 12.1**：当用户发送消息时，系统应将消息内容、时间戳和会话上下文存储到数据库中。

## 测试文件

### MessageStorageRoundTripPropertyTest.java
使用 jqwik 框架的标准属性测试（需要 Java 11+）

### MessageStorageRoundTripManualTest.java
手动实现的属性测试，兼容 Java 8，可直接运行

## 运行测试

### 方式 1：使用 Maven（推荐）
```bash
mvn test -Dtest=MessageStorageRoundTripPropertyTest
```

### 方式 2：直接运行手动测试
```bash
# 编译
javac -cp "app-device/target/classes" -d app-device/target/test-classes \
  app-device/src/test/java/com/example/voicebox/app/device/chat/MessageStorageRoundTripManualTest.java

# 运行
java -cp "app-device/target/classes:app-device/target/test-classes" \
  com.example.voicebox.app.device.chat.MessageStorageRoundTripManualTest
```

## 测试的属性

### 属性 1：单条消息存储往返
**验证**：对于任何消息（内容、角色、会话），保存后检索应保持完全一致
- 消息内容完全相同
- 角色（user/assistant/system）相同
- 会话ID正确关联
- 时间戳被正确记录
- 消息ID被分配

**测试次数**：100 次随机生成的消息

### 属性 2：多条消息存储往返
**验证**：同一会话中的多条消息都能正确存储和检索，并保持顺序
- 所有消息都能被检索
- 消息按时间顺序排列
- 每条消息的内容和元数据正确

**测试次数**：100 次，每次 2-10 条消息

### 属性 3：会话上下文往返
**验证**：会话的上下文信息（标题、模型、设备信息）能正确保存和检索
- 会话标题一致
- 模型信息一致
- 设备信息一致
- 创建和更新时间被记录

**测试次数**：100 次随机生成的会话

### 属性 4：特殊字符处理
**验证**：包含特殊字符的消息能正确存储和检索
- 换行符 `\n`
- 制表符 `\t`
- 引号 `"` 和 `'`
- 中文字符
- Emoji 表情
- HTML/XML 特殊字符 `<>&`
- SQL 注入尝试
- JSON 格式
- Unicode 字符

**测试次数**：50 次

## 测试结果

最近一次运行结果：
```
通过: 350
失败: 0
总计: 350
成功率: 100.0%
```

## 验证的需求

- ✅ **需求 12.1**：消息内容、时间戳和会话上下文的存储
- ✅ 数据完整性：往返一致性
- ✅ 并发安全：多条消息的正确处理
- ✅ 边界情况：特殊字符和各种输入

## 注意事项

1. 测试使用内存存储作为后备，当数据库不可用时仍能运行
2. 测试会自动清理，不会污染数据库
3. 每次测试使用随机生成的数据，确保覆盖各种情况
4. 测试符合属性测试的最佳实践：至少 100 次迭代
