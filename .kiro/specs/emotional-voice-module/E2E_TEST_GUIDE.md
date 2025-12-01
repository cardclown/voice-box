# 情感语音模块端到端测试指南

## 概述

本文档描述如何运行情感语音模块的端到端测试，验证所有功能是否正常工作。

## 测试覆盖范围

### 后端测试 (Java)

**文件**: `app-device/src/test/java/com/example/voicebox/app/device/emotional/EmotionalVoiceE2ETest.java`

测试场景：
1. ✅ 完整的语音分析流程
2. ✅ 完整的情感语音合成流程
3. ✅ 用户画像更新流程
4. ✅ 标签生成和置信度计算
5. ✅ 统计数据展示
6. ✅ 错误处理 - 无效音频文件
7. ✅ 性能测试 - 并发请求

### 前端测试 (Vue/Vitest)

**文件**: `app-web/src/components/emotional/__tests__/EmotionalVoiceE2E.test.js`

测试场景：
1. ✅ 完整的语音分析流程
2. ✅ 情感语音合成流程
3. ✅ 用户画像展示
4. ✅ 实时情绪反馈
5. ✅ 错误处理
6. ✅ 数据持久化
7. ✅ 响应式设计
8. ✅ 性能测试

## 运行测试

### 方式 1: 使用测试脚本（推荐）

```bash
# 运行完整的端到端测试套件
./scripts/test-emotional-voice.sh
```

这个脚本会：
- 运行后端 Java 测试
- 运行前端 Vue 测试
- 验证 API 端点
- 检查数据库表结构
- 验证所有组件和服务文件
- 生成测试报告

### 方式 2: 单独运行测试

#### 后端测试

```bash
cd app-device
mvn test -Dtest=EmotionalVoiceE2ETest
```

#### 前端测试

```bash
cd app-web
npm run test -- EmotionalVoiceE2E.test.js
```

### 方式 3: 运行所有测试

```bash
# 后端所有测试
cd app-device
mvn test

# 前端所有测试
cd app-web
npm run test
```

## 测试前准备

### 1. 启动后端服务

```bash
cd app-device
mvn spring-boot:run
```

等待服务启动完成（看到 "Started Application" 日志）。

### 2. 启动前端服务（可选）

```bash
cd app-web
npm run dev
```

### 3. 确保数据库可访问

```bash
# 测试数据库连接
mysql -h 129.211.180.183 -u voicebox -pvoicebox123 voicebox_db -e "SELECT 1"
```

### 4. 准备测试数据（可选）

如果需要测试真实音频：

```bash
# 创建测试音频目录
mkdir -p test-data

# 添加测试音频文件
# 可以录制或下载音频文件到 test-data/ 目录
```

## 测试结果解读

### 成功标志

```
✓ 语音分析流程测试通过
✓ 情感语音合成流程测试通过
✓ 用户画像更新流程测试通过
✓ 标签生成流程测试通过
✓ 统计数据展示测试通过
✓ 错误处理测试通过
✓ 并发性能测试通过

🎉 所有测试通过！
```

### 失败处理

如果测试失败，检查：

1. **服务是否运行**
   ```bash
   curl http://localhost:10088/actuator/health
   ```

2. **数据库是否可访问**
   ```bash
   mysql -h 129.211.180.183 -u voicebox -pvoicebox123 voicebox_db -e "SHOW TABLES"
   ```

3. **查看详细错误日志**
   - 后端日志: `app-device/logs/`
   - 前端控制台输出

4. **检查依赖是否安装**
   ```bash
   # 后端
   cd app-device && mvn dependency:resolve
   
   # 前端
   cd app-web && npm install
   ```

## 性能基准

测试会验证以下性能指标：

| 操作 | 目标时间 | 测试验证 |
|------|---------|---------|
| 语音特征提取 | < 3秒 | ✅ |
| 情感识别 | < 2秒 | ✅ |
| 语音合成 | < 3秒 | ✅ |
| 并发处理 (5用户) | 平均 < 3秒/请求 | ✅ |

## 测试覆盖率

### 功能覆盖

- [x] 语音分析 API
- [x] 情感合成 API
- [x] 用户画像 API
- [x] 特征提取服务
- [x] 情绪识别服务
- [x] 标签生成服务
- [x] 前端组件交互
- [x] 错误处理
- [x] 性能测试

### 需求覆盖

测试覆盖了以下需求：
- 需求 1.1-1.4: 语音特征提取
- 需求 2.1-2.5: 性别识别
- 需求 3.1-3.5: 性格特征识别
- 需求 4.1-4.5: 情绪识别
- 需求 5.1-5.5: 语气风格识别
- 需求 6.1-6.5: 标签生成
- 需求 7.1-7.5: 用户画像管理
- 需求 8.1-8.5: 情感语音合成
- 需求 12.1-12.3: 前端界面
- 需求 13.1-13.5: 实时反馈
- 需求 20.1-20.5: 错误处理

## 持续集成

### 在 CI/CD 中运行

```yaml
# .github/workflows/test.yml 示例
name: E2E Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Set up JDK
        uses: actions/setup-java@v2
        with:
          java-version: '17'
      
      - name: Set up Node.js
        uses: actions/setup-node@v2
        with:
          node-version: '18'
      
      - name: Run E2E Tests
        run: ./scripts/test-emotional-voice.sh
```

## 调试技巧

### 1. 查看详细测试输出

```bash
# 后端详细输出
mvn test -Dtest=EmotionalVoiceE2ETest -X

# 前端详细输出
npm run test -- EmotionalVoiceE2E.test.js --reporter=verbose
```

### 2. 单独运行特定测试

```bash
# 后端单个测试方法
mvn test -Dtest=EmotionalVoiceE2ETest#testCompleteVoiceAnalysisFlow

# 前端单个测试
npm run test -- EmotionalVoiceE2E.test.js -t "完整的语音分析流程"
```

### 3. 使用调试模式

```bash
# 后端调试
mvn test -Dtest=EmotionalVoiceE2ETest -Dmaven.surefire.debug

# 前端调试
npm run test:debug -- EmotionalVoiceE2E.test.js
```

## 常见问题

### Q: 测试超时怎么办？

A: 增加超时时间：
```bash
# 后端
mvn test -Dtest=EmotionalVoiceE2ETest -Dsurefire.timeout=300

# 前端
npm run test -- EmotionalVoiceE2E.test.js --testTimeout=30000
```

### Q: 数据库连接失败？

A: 检查：
1. 数据库服务是否运行
2. 网络连接是否正常
3. 用户名密码是否正确
4. 防火墙是否开放 3306 端口

### Q: Mock 数据不生效？

A: 确保：
1. Mock 在测试前正确设置
2. 使用 `vi.clearAllMocks()` 清理之前的 mock
3. Mock 返回的数据格式正确

## 下一步

测试通过后，可以：

1. ✅ 部署到测试环境
2. ✅ 进行用户验收测试
3. ✅ 优化性能（如果需要）
4. ✅ 添加更多测试场景
5. ✅ 部署到生产环境

## 参考资料

- [JUnit 5 文档](https://junit.org/junit5/docs/current/user-guide/)
- [Vitest 文档](https://vitest.dev/)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [Vue Test Utils](https://test-utils.vuejs.org/)
