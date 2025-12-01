# v2.0 用户个性分析系统 - API文档

**版本**: v2.0  
**Base URL**: `http://localhost:10088`  
**更新时间**: 2024-01-15

---

## 📋 目录

- [个性分析API](#个性分析api)
- [集成API](#集成api)
- [数据模型](#数据模型)
- [错误码](#错误码)

---

## 个性分析API

### 1. 获取用户画像

获取指定用户的个性画像信息。

**请求**

```http
GET /api/personality/profile/{userId}
```

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户ID |

**响应示例**

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
      "responseLengthPreference": "balanced",
      "languageStylePreference": "casual",
      "interactionStyle": "active",
      "totalMessages": 120,
      "totalSessions": 15,
      "confidenceScore": 0.650,
      "createdAt": "2024-01-01T10:00:00",
      "updatedAt": "2024-01-15T15:30:00",
      "lastAnalyzedAt": "2024-01-15T15:30:00"
    },
    "personalityType": "外向开放严谨",
    "isConfident": true,
    "needsUpdate": false
  },
  "timestamp": 1705315800000
}
```

---

### 2. 分析用户个性

触发用户个性分析，基于历史对话数据生成或更新用户画像。

**请求**

```http
POST /api/personality/analyze/{userId}
```

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户ID |

**响应示例**

```json
{
  "success": true,
  "message": "分析成功",
  "data": {
    "profile": {
      "userId": 1,
      "openness": 0.750,
      "conscientiousness": 0.650,
      "extraversion": 0.800,
      "agreeableness": 0.700,
      "neuroticism": 0.400,
      "confidenceScore": 0.650
    },
    "message": "分析完成"
  },
  "timestamp": 1705315800000
}
```

---

### 3. 提取消息特征

从用户消息中提取语言学和语义特征。

**请求**

```http
POST /api/personality/extract-features
```

**请求体**

```json
{
  "userId": 1,
  "sessionId": 1,
  "messageId": 1,
  "content": "你好，我想学习Java编程，请问有什么好的教程推荐吗？"
}
```

**请求参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户ID |
| sessionId | Long | 是 | 会话ID |
| messageId | Long | 是 | 消息ID |
| content | String | 是 | 消息内容 |

**响应示例**

```json
{
  "success": true,
  "message": "特征提取成功",
  "data": {
    "id": 1,
    "userId": 1,
    "sessionId": 1,
    "messageId": 1,
    "messageLength": 28,
    "wordCount": 15,
    "sentenceCount": 1,
    "avgWordLength": 1.87,
    "vocabularyRichness": 0.933,
    "topics": "[\"学习\", \"技术\"]",
    "sentimentScore": 0.200,
    "intent": "question",
    "keywords": "[\"学习\", \"Java\", \"编程\", \"教程\"]",
    "questionCount": 1,
    "exclamationCount": 0,
    "emojiCount": 0,
    "codeBlockCount": 0,
    "createdAt": "2024-01-15T15:30:00"
  },
  "timestamp": 1705315800000
}
```

---

### 4. 获取响应策略

获取针对特定用户的个性化响应策略。

**请求**

```http
GET /api/personality/strategy/{userId}
```

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户ID |

**响应示例**

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
  },
  "timestamp": 1705315800000
}
```

---

### 5. 提交用户反馈

提交用户对AI响应的反馈，用于学习和优化。

**请求**

```http
POST /api/personality/feedback
```

**请求体**

```json
{
  "userId": 1,
  "sessionId": 1,
  "messageId": 1,
  "feedbackType": "like",
  "feedbackValue": 1,
  "feedbackText": "回答很好，很有帮助",
  "feedbackTags": "[\"helpful\", \"clear\"]",
  "aiResponseId": 2,
  "responseStrategy": "{\"responseLength\":\"balanced\"}"
}
```

**请求参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户ID |
| sessionId | Long | 是 | 会话ID |
| messageId | Long | 是 | 消息ID |
| feedbackType | String | 是 | 反馈类型：like/dislike/regenerate |
| feedbackValue | Integer | 否 | 反馈值：1=正面，-1=负面，0=中性 |
| feedbackText | String | 否 | 反馈文字 |
| feedbackTags | String | 否 | 反馈标签（JSON数组） |
| aiResponseId | Long | 否 | AI响应ID |
| responseStrategy | String | 否 | 使用的响应策略（JSON） |

**响应示例**

```json
{
  "success": true,
  "message": "反馈提交成功",
  "data": null,
  "timestamp": 1705315800000
}
```

---

### 6. 获取用户统计

获取用户的综合统计信息，包括画像、特征、反馈等。

**请求**

```http
GET /api/personality/stats/{userId}
```

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户ID |

**响应示例**

```json
{
  "success": true,
  "message": "获取成功",
  "data": {
    "profile": {
      "userId": 1,
      "openness": 0.750,
      "confidenceScore": 0.650
    },
    "averageFeatures": {
      "avg_message_length": 85.5,
      "avg_word_count": 42.3,
      "avg_sentiment_score": 0.15
    },
    "sentimentDistribution": {
      "positive_count": 80,
      "neutral_count": 30,
      "negative_count": 10,
      "total_count": 120
    },
    "feedbackStatistics": {
      "positive_count": 45,
      "neutral_count": 5,
      "negative_count": 10,
      "total_count": 60,
      "avg_feedback_value": 0.58
    },
    "learningEffect": {
      "totalFeedback": 60,
      "positiveRatio": 0.75,
      "negativeRatio": 0.17,
      "learningQuality": "good",
      "profileConfidence": 0.650
    }
  },
  "timestamp": 1705315800000
}
```

---

### 7. 获取对话特征历史

获取用户的历史对话特征记录。

**请求**

```http
GET /api/personality/features/{userId}?limit=20
```

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户ID |

**查询参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| limit | Integer | 否 | 20 | 返回记录数量 |

**响应示例**

```json
{
  "success": true,
  "message": "获取成功",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "sessionId": 1,
      "messageId": 1,
      "messageLength": 28,
      "wordCount": 15,
      "sentimentScore": 0.200,
      "intent": "question",
      "createdAt": "2024-01-15T15:30:00"
    }
  ],
  "timestamp": 1705315800000
}
```

---

### 8. 获取反馈历史

获取用户的历史反馈记录。

**请求**

```http
GET /api/personality/feedback/{userId}?limit=20
```

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户ID |

**查询参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| limit | Integer | 否 | 20 | 返回记录数量 |

**响应示例**

```json
{
  "success": true,
  "message": "获取成功",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "sessionId": 1,
      "messageId": 1,
      "feedbackType": "like",
      "feedbackValue": 1,
      "feedbackText": "回答很好",
      "createdAt": "2024-01-15T15:30:00"
    }
  ],
  "timestamp": 1705315800000
}
```

---

### 9. 批量学习历史反馈

触发批量学习用户的历史反馈，优化用户画像。

**请求**

```http
POST /api/personality/learn/{userId}
```

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户ID |

**响应示例**

```json
{
  "success": true,
  "message": "学习完成",
  "data": null,
  "timestamp": 1705315800000
}
```

---

### 10. 获取性格维度统计

获取所有用户的性格维度平均统计。

**请求**

```http
GET /api/personality/personality-stats
```

**响应示例**

```json
{
  "success": true,
  "message": "获取成功",
  "data": {
    "avg_openness": 0.625,
    "avg_conscientiousness": 0.580,
    "avg_extraversion": 0.650,
    "avg_agreeableness": 0.720,
    "avg_neuroticism": 0.450,
    "avg_confidence": 0.600
  },
  "timestamp": 1705315800000
}
```

---

### 11. 健康检查

检查个性分析服务的健康状态。

**请求**

```http
GET /api/personality/health
```

**响应示例**

```json
{
  "success": true,
  "message": "服务正常",
  "data": {
    "status": "ok",
    "service": "personality-analysis",
    "version": "2.0"
  },
  "timestamp": 1705315800000
}
```

---

## 集成API

### 1. 发送消息（集成）

发送消息并触发个性化处理。

**请求**

```http
POST /api/chat-integration/send-message
```

**请求体**

```json
{
  "userId": 1,
  "sessionId": 1,
  "messageId": 1,
  "content": "你好，我想学习Java编程"
}
```

**响应示例**

```json
{
  "success": true,
  "message": "消息已发送",
  "personalizedPrompt": "你是一个智能助手。请提供适度详细的回答，平衡简洁性和完整性。使用轻松友好的语气，像朋友一样交流。保持热情友好，积极互动。适当使用示例来说明概念。代码要格式规范，添加必要的注释。用户对新想法和创新方法感兴趣。用户喜欢互动和交流。"
}
```

---

### 2. 开始新会话

创建新会话并初始化个性化上下文。

**请求**

```http
POST /api/chat-integration/start-session
```

**请求体**

```json
{
  "userId": 1,
  "title": "Java学习讨论"
}
```

**响应示例**

```json
{
  "success": true,
  "session": {
    "userId": 1,
    "title": "Java学习讨论",
    "personalizationContext": "{\"profileConfidence\":0.650,\"personalityType\":\"外向开放严谨\"}"
  },
  "needsProfileUpdate": false,
  "personalizationSuggestions": {
    "available": true,
    "personalityType": "外向开放严谨",
    "confidence": 0.650,
    "tips": [
      "您对新想法很感兴趣，我会为您提供创新的解决方案",
      "您喜欢互动交流，我会使用更友好的语气"
    ],
    "preferences": {
      "responseLength": "balanced",
      "languageStyle": "casual",
      "interactionStyle": "active"
    }
  }
}
```

---

### 3. 获取个性化建议

获取用户的个性化建议和提示。

**请求**

```http
GET /api/chat-integration/suggestions/{userId}
```

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户ID |

**响应示例**

```json
{
  "success": true,
  "data": {
    "available": true,
    "personalityType": "外向开放严谨",
    "confidence": 0.650,
    "tips": [
      "您对新想法很感兴趣，我会为您提供创新的解决方案",
      "您注重细节，我会提供更详细准确的信息",
      "您喜欢互动交流，我会使用更友好的语气"
    ],
    "preferences": {
      "responseLength": "balanced",
      "languageStyle": "casual",
      "interactionStyle": "active"
    }
  }
}
```

---

### 4. 生成个性化提示词

生成针对特定用户的个性化AI提示词。

**请求**

```http
POST /api/chat-integration/generate-prompt
```

**请求体**

```json
{
  "userId": 1,
  "basePrompt": "你是一个智能助手。"
}
```

**响应示例**

```json
{
  "success": true,
  "basePrompt": "你是一个智能助手。",
  "personalizedPrompt": "你是一个智能助手。请提供适度详细的回答，平衡简洁性和完整性。使用轻松友好的语气，像朋友一样交流。保持热情友好，积极互动。适当使用示例来说明概念。代码要格式规范，添加必要的注释。用户对新想法和创新方法感兴趣。用户喜欢互动和交流。"
}
```

---

### 5. 触发画像分析

手动触发用户画像分析（后台异步执行）。

**请求**

```http
POST /api/chat-integration/trigger-analysis/{userId}
```

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户ID |

**响应示例**

```json
{
  "success": true,
  "message": "画像分析已触发，将在后台执行"
}
```

---

## 数据模型

### UserProfile (用户画像)

```json
{
  "id": 1,
  "userId": 1,
  "openness": 0.750,
  "conscientiousness": 0.650,
  "extraversion": 0.800,
  "agreeableness": 0.700,
  "neuroticism": 0.400,
  "responseLengthPreference": "balanced",
  "languageStylePreference": "casual",
  "contentFormatPreference": "[\"text\", \"code\"]",
  "interactionStyle": "active",
  "totalMessages": 120,
  "totalSessions": 15,
  "avgSessionDuration": 25.5,
  "confidenceScore": 0.650,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-15T15:30:00",
  "lastAnalyzedAt": "2024-01-15T15:30:00"
}
```

### ConversationFeature (对话特征)

```json
{
  "id": 1,
  "userId": 1,
  "sessionId": 1,
  "messageId": 1,
  "messageLength": 28,
  "wordCount": 15,
  "sentenceCount": 1,
  "avgWordLength": 1.87,
  "vocabularyRichness": 0.933,
  "topics": "[\"学习\", \"技术\"]",
  "sentimentScore": 0.200,
  "intent": "question",
  "keywords": "[\"学习\", \"Java\", \"编程\"]",
  "questionCount": 1,
  "exclamationCount": 0,
  "emojiCount": 0,
  "codeBlockCount": 0,
  "createdAt": "2024-01-15T15:30:00"
}
```

### UserFeedback (用户反馈)

```json
{
  "id": 1,
  "userId": 1,
  "sessionId": 1,
  "messageId": 1,
  "feedbackType": "like",
  "feedbackValue": 1,
  "feedbackText": "回答很好",
  "feedbackTags": "[\"helpful\", \"clear\"]",
  "aiResponseId": 2,
  "responseStrategy": "{\"responseLength\":\"balanced\"}",
  "createdAt": "2024-01-15T15:30:00"
}
```

### ResponseStrategy (响应策略)

```json
{
  "responseLength": "balanced",
  "languageStyle": "casual",
  "detailLevel": "medium",
  "exampleUsage": true,
  "interactionTone": "friendly",
  "codeFormatting": true,
  "promptAdjustment": "用户对新想法和创新方法感兴趣。"
}
```

---

## 错误码

### HTTP状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 业务错误码

所有API响应都包含`success`字段，表示业务是否成功。

**成功响应**

```json
{
  "success": true,
  "message": "操作成功",
  "data": { ... },
  "timestamp": 1705315800000
}
```

**失败响应**

```json
{
  "success": false,
  "message": "错误描述",
  "data": null,
  "timestamp": 1705315800000
}
```

### 常见错误

| 错误信息 | 原因 | 解决方案 |
|----------|------|----------|
| "用户画像不存在" | 用户还没有画像数据 | 先调用分析API创建画像 |
| "数据不足" | 用户消息数量太少 | 等待用户发送更多消息 |
| "置信度不足" | 画像置信度低于阈值 | 收集更多数据或降低阈值 |
| "参数错误" | 请求参数格式不正确 | 检查参数类型和格式 |

---

## 使用限制

### 速率限制

| API | 限制 | 说明 |
|-----|------|------|
| 获取画像 | 100次/分钟 | 建议使用缓存 |
| 分析个性 | 10次/分钟 | 计算密集型操作 |
| 提取特征 | 1000次/分钟 | 异步处理 |
| 提交反馈 | 100次/分钟 | 正常使用 |

### 数据限制

| 项目 | 限制 | 说明 |
|------|------|------|
| 消息长度 | 10000字符 | 超长消息会被截断 |
| 历史记录 | 最近1000条 | 自动清理旧数据 |
| 反馈记录 | 最近500条 | 自动清理旧数据 |

---

## 最佳实践

### 1. 缓存策略

```javascript
// 缓存用户画像，减少API调用
const profileCache = new Map();

async function getUserProfile(userId) {
  if (profileCache.has(userId)) {
    return profileCache.get(userId);
  }
  
  const profile = await fetchProfile(userId);
  profileCache.set(userId, profile);
  
  // 5分钟后过期
  setTimeout(() => profileCache.delete(userId), 5 * 60 * 1000);
  
  return profile;
}
```

### 2. 错误处理

```javascript
async function analyzePersonality(userId) {
  try {
    const response = await fetch(`/api/personality/analyze/${userId}`, {
      method: 'POST'
    });
    
    const data = await response.json();
    
    if (!data.success) {
      console.error('分析失败:', data.message);
      return null;
    }
    
    return data.data;
    
  } catch (error) {
    console.error('网络错误:', error);
    return null;
  }
}
```

### 3. 批量操作

```javascript
// 批量提取特征
async function batchExtractFeatures(messages) {
  const promises = messages.map(msg => 
    fetch('/api/personality/extract-features', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(msg)
    })
  );
  
  return Promise.all(promises);
}
```

---

**文档维护**: VoiceBox开发团队  
**最后更新**: 2024-01-15  
**版本**: v2.0
