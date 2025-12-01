---
inclusion: always
---

# 中文交流规则

## 语言设置

- **主要语言**: 中文（简体）
- **适用范围**: 所有消息回复、文档编写、代码注释

## 交流规范

### 消息回复
- 所有与用户的对话都使用中文
- 保持专业、友好的语气
- 使用清晰、简洁的表达方式
- 技术术语可以保留英文，但需要提供中文解释

### 文档编写
- 所有规格文档（requirements.md, design.md, tasks.md）使用中文编写
- 代码注释使用中文
- README 文件使用中文
- 技术文档使用中文，专业术语可保留英文原文

### 代码规范
- 变量名、函数名、类名使用英文（遵循编程规范）
- 注释使用中文
- 文档字符串使用中文
- 提交信息使用中文

### 示例

**好的做法**：
```javascript
// 发送聊天消息到后端
async function sendMessage(content) {
  // 验证消息内容
  if (!content.trim()) {
    throw new Error('消息内容不能为空');
  }
  
  // 调用 API
  return await chatService.send(content);
}
```

**避免的做法**：
```javascript
// Send chat message to backend
async function sendMessage(content) {
  // Validate message content
  if (!content.trim()) {
    throw new Error('Message content cannot be empty');
  }
  
  // Call API
  return await chatService.send(content);
}
```

## 工作流程规范

### 文档优先原则
- **执行项目相关操作前，必须先阅读相关文档**
- 查看项目文档以获取上下文信息，包括：
  - 项目结构文档（如 PROJECT_STRUCTURE.md）
  - 快速开始指南（QUICK_START.md）
  - 相关功能的设计文档（docs/versions/）
  - API 文档（README_API.md）
  - 数据库架构（DATABASE_SCHEMA.md）
- 基于文档信息做出决策，避免盲目操作
- 如果文档不存在或不清晰，先询问用户或查看代码结构

### 操作顺序
1. 理解用户需求
2. 查找并阅读相关文档
3. 分析现有代码结构
4. 执行操作
5. 验证结果

## 特殊情况

- 当用户明确要求使用英文时，可以切换到英文
- 国际化的配置文件（如 package.json）保持英文
- 第三方库的配置遵循其官方文档的语言
