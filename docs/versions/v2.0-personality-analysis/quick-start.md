# v2.0 用户个性分析系统 - 快速开始

**版本**: v2.0  
**更新时间**: 2024-01-15

---

## 🚀 快速部署

### 1. 数据库初始化

```bash
# 连接到MySQL数据库
mysql -u root -p voicebox

# 执行迁移脚本
source app-device/src/main/resources/db/migration/V2.0__personality_analysis_tables.sql

# 验证表创建
SHOW TABLES LIKE '%profile%';
SHOW TABLES LIKE '%feature%';
SHOW TABLES LIKE '%feedback%';
```

### 2. 启动应用

```bash
# 进入后端目录
cd app-device

# 编译并启动
mvn clean install
mvn spring-boot:run
```

### 3. 验证服务

```bash
# 健康检查
curl http://localhost:10088/api/personality/health

# 预期响应
{
  "success": true,
  "message": "服务正常",
  "data": {
    "status": "ok",
    "service": "personality-analysis",
    "version": "2.0"
  }
}
```

---

## 📖 API使用指南

### 1. 获取用户画像

```bash
curl http://localhost:10088/api/personality/profile/1
```

**响应示例**:
```json
{
  "success": true,
  "message": "获取成功",
  "data": {
    "profile": {
      "userId": 1,
      "openness": 0.750,
      "conscientiousness": 0.650,
      "extraversion": 0.800,
      "agreeableness": 0.700,
      "neuroticism": 0.400,
      "confidenceScore": 0.600,
      "responseLengthPreference": "balanced",
      "languageStylePreference": "casual"
    },
    "personalityType": "外向开放严谨",
    "isConfident": true,
    "needsUpdate": false
  }
}
```

### 2. 分析用户个性

```bash
curl -X POST http://localhost:10088/api/personality/analyze/1
```

### 3. 提取消息特征

```bash
curl -X POST http://localhost:10088/api/personality/extract-features \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "sessionId": 1,
    "messageId": 1,
    "content": "你好，我想学习Java编程"
  }'
```

### 4. 获取响应策略

```bash
curl http://localhost:10088/api/personality/strategy/1
```

**响应示例**:
```json
{
  "success": true,
  "message": "获取成功",
  "data": {
    "responseLength": "balanced",
    "languageStyle": "casual",
    "detailLevel": "medium",
    "exampleUsage": true,
    "interactionTone": "friendly",
    "codeFormatting": true,
    "promptAdjustment": "用户对新想法和创新方法感兴趣。用户喜欢互动和交流。"
  }
}
```

### 5. 提交用户反馈

```bash
curl -X POST http://localhost:10088/api/personality/feedback \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "sessionId": 1,
    "messageId": 1,
    "feedbackType": "like",
    "feedbackValue": 1,
    "feedbackText": "回答很好"
  }'
```

### 6. 获取用户统计

```bash
curl http://localhost:10088/api/personality/stats/1
```

---

## 🔧 集成到现有系统

### 1. 在消息处理中添加特征提取

```java
@Autowired
private MessageFeatureInterceptor messageFeatureInterceptor;

// 在用户发送消息后
public void handleUserMessage(Long userId, Long sessionId, Long messageId, String content) {
    // 原有的消息处理逻辑
    // ...
    
    // 异步提取特征
    messageFeatureInterceptor.processMessage(userId, sessionId, messageId, content);
}
```

### 2. 在AI响应生成时应用策略

```java
@Autowired
private ResponseStrategyService responseStrategyService;

// 在生成AI响应前
public String generateAIResponse(Long userId, String userMessage) {
    // 获取用户的响应策略
    ResponseStrategyService.ResponseStrategy strategy = 
        responseStrategyService.generateStrategy(userId);
    
    // 根据策略调整提示词
    String systemPrompt = buildSystemPrompt(strategy);
    
    // 调用AI生成响应
    String response = callAI(systemPrompt, userMessage);
    
    return response;
}

private String buildSystemPrompt(ResponseStrategyService.ResponseStrategy strategy) {
    StringBuilder prompt = new StringBuilder();
    prompt.append("你是一个智能助手。");
    
    // 根据策略调整
    if ("concise".equals(strategy.getResponseLength())) {
        prompt.append("请提供简洁的回答。");
    } else if ("detailed".equals(strategy.getResponseLength())) {
        prompt.append("请提供详细的解释。");
    }
    
    if ("casual".equals(strategy.getLanguageStyle())) {
        prompt.append("使用轻松友好的语气。");
    } else if ("formal".equals(strategy.getLanguageStyle())) {
        prompt.append("使用正式专业的语气。");
    }
    
    // 添加个性化调整
    if (!strategy.getPromptAdjustment().isEmpty()) {
        prompt.append(strategy.getPromptAdjustment());
    }
    
    return prompt.toString();
}
```

### 3. 添加反馈收集

```java
@Autowired
private UserFeedbackRepository userFeedbackRepository;

@Autowired
private LearningService learningService;

// 在用户点击反馈按钮时
public void handleUserFeedback(Long userId, Long sessionId, Long messageId, 
                               String feedbackType, String feedbackText) {
    // 创建反馈记录
    UserFeedback feedback = new UserFeedback();
    feedback.setUserId(userId);
    feedback.setSessionId(sessionId);
    feedback.setMessageId(messageId);
    feedback.setFeedbackType(feedbackType);
    feedback.setFeedbackValue(getFeedbackValue(feedbackType));
    feedback.setFeedbackText(feedbackText);
    
    // 保存反馈
    userFeedbackRepository.create(feedback);
    
    // 触发学习
    learningService.learnFromFeedback(userId, feedback);
}

private Integer getFeedbackValue(String feedbackType) {
    switch (feedbackType) {
        case "like": return 1;
        case "dislike": return -1;
        default: return 0;
    }
}
```

---

## 📊 监控与维护

### 1. 查看定时任务日志

```bash
# 查看应用日志
tail -f logs/app-device.log | grep "PersonalityAnalysisScheduler"
```

### 2. 手动触发画像分析

```bash
# 为特定用户触发分析
curl -X POST http://localhost:10088/api/personality/analyze/1

# 批量学习历史反馈
curl -X POST http://localhost:10088/api/personality/learn/1
```

### 3. 查看系统统计

```bash
# 获取性格维度统计
curl http://localhost:10088/api/personality/personality-stats
```

---

## 🧪 测试

### 运行单元测试

```bash
cd app-device
mvn test -Dtest=FeatureExtractionServiceTest
```

### 测试场景

#### 场景1: 新用户首次使用

```bash
# 1. 用户发送第一条消息
curl -X POST http://localhost:10088/api/personality/extract-features \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 100,
    "sessionId": 1,
    "messageId": 1,
    "content": "你好，我是新用户"
  }'

# 2. 查看用户画像（应该是默认值）
curl http://localhost:10088/api/personality/profile/100

# 3. 用户发送更多消息后，触发分析
# ... 发送10-20条消息 ...

# 4. 手动触发分析
curl -X POST http://localhost:10088/api/personality/analyze/100

# 5. 查看更新后的画像
curl http://localhost:10088/api/personality/profile/100
```

#### 场景2: 用户反馈学习

```bash
# 1. 用户提交正面反馈
curl -X POST http://localhost:10088/api/personality/feedback \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "sessionId": 1,
    "messageId": 10,
    "feedbackType": "like",
    "feedbackValue": 1
  }'

# 2. 查看学习效果
curl http://localhost:10088/api/personality/stats/1
```

---

## ⚠️ 常见问题

### Q1: 数据库表创建失败？
**A**: 检查MySQL版本是否为5.7+，确保有CREATE权限。

### Q2: 特征提取不准确？
**A**: 
- 扩充情感词典（在FeatureExtractionService中）
- 增加主题关键词
- 收集更多训练数据

### Q3: 画像置信度一直很低？
**A**: 
- 确保用户有足够的消息数据（建议50+条）
- 检查消息内容是否有效
- 查看日志是否有错误

### Q4: 定时任务没有执行？
**A**: 
- 检查@EnableScheduling是否启用
- 查看日志确认任务是否被触发
- 检查cron表达式是否正确

---

## 📚 相关文档

- [需求文档](versions/v2.0-personality-analysis/requirements.md)
- [设计文档](versions/v2.0-personality-analysis/design.md)
- [实施计划](versions/v2.0-personality-analysis/implementation-plan.md)
- [执行指南](versions/v2.0-personality-analysis/execution-guide.md)
- [实施进度](V2.0_IMPLEMENTATION_PROGRESS.md)

---

**文档维护**: VoiceBox开发团队  
**最后更新**: 2024-01-15
