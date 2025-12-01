# VoiceBox 优先实施计划

## 📋 实施范围

基于深度分析，以下优化将被实施（**排除国际化和无障碍性**）：

✅ 架构优化（3项）
✅ 用户体验优化（4项）
✅ 性能优化（3项）
✅ 安全优化（3项）
✅ 可维护性优化（3项）
✅ 智能化功能（3项）
✅ 移动端优化（2项）
✅ 数据管理（3项）

**总计：24项核心优化**

---

## 🎯 第一阶段：基础架构和安全（第1-2周）

### 优先级：🔴 紧急

#### 1. 创建统一的 API 客户端
**文件**: `app-web/src/services/apiClient.js`

**功能**:
- 统一的请求拦截和错误处理
- 自动重试机制（3次）
- 请求取消支持
- 请求/响应日志
- 超时控制

**依赖**: 无

#### 2. 实现 Toast 通知系统
**文件**: 
- `app-web/src/components/common/Toast.vue`
- `app-web/src/composables/useToast.js`

**功能**:
- 成功/错误/警告/信息 4种类型
- 自动消失（可配置时长）
- 支持多个 Toast 堆叠
- 动画效果

**依赖**: 无

#### 3. XSS 防护
**文件**: 
- `app-web/src/utils/sanitize.js`
- 更新 `MessageItem.vue`

**功能**:
- 使用 DOMPurify 清理 HTML
- 白名单标签和属性
- 防止脚本注入

**依赖**: `npm install dompurify`

#### 4. 环境变量管理
**文件**:
- `.env.development`
- `.env.production`
- 更新所有硬编码的 API_BASE

**功能**:
- 开发/生产环境分离
- API 地址配置化
- 敏感信息保护

**依赖**: 无（Vite 内置支持）

#### 5. 后端线程池优化
**文件**: `app-device/src/main/java/com/example/voicebox/app/device/controller/DeviceApiController.java`

**功能**:
- 使用 ThreadPoolExecutor 替代 newCachedThreadPool
- 设置核心线程数、最大线程数
- 配置任务队列和拒绝策略
- 添加线程池监控

**依赖**: 无

---

## 🎨 第二阶段：用户体验提升（第3-4周）

### 优先级：🔴 高

#### 6. Markdown 和代码高亮
**文件**:
- `app-web/src/utils/markdown.js`
- 更新 `MessageItem.vue`

**功能**:
- Markdown 渲染（标题、列表、链接、表格）
- 代码语法高亮
- 代码块复制按钮
- 数学公式支持（可选）

**依赖**: 
```bash
npm install marked highlight.js
npm install katex  # 可选，用于数学公式
```

#### 7. 消息操作功能
**文件**: 更新 `MessageItem.vue`

**功能**:
- 复制消息内容
- 重新生成 AI 回复
- 编辑用户消息
- 删除消息
- 收藏消息

**依赖**: Toast 系统（第2项）

#### 8. 搜索功能增强
**文件**:
- 更新 `SessionSidebar.vue`
- 新增后端搜索 API

**功能**:
- 搜索消息内容（不只是会话标题）
- 高亮搜索结果
- 搜索历史记录
- 按日期/模型筛选

**依赖**: API 客户端（第1项）

#### 9. 加载状态优化
**文件**:
- `app-web/src/components/common/Skeleton.vue`
- 更新各个组件

**功能**:
- 骨架屏加载
- 加载进度条
- 优雅的加载动画

**依赖**: 无

---

## ⚡ 第三阶段：性能优化（第5-6周）

### 优先级：🟡 中高

#### 10. 虚拟滚动
**文件**: 更新 `MessageList.vue`

**功能**:
- 长对话列表虚拟滚动
- 只渲染可见区域的消息
- 支持动态高度

**依赖**: 
```bash
npm install vue-virtual-scroller
```

#### 11. 会话列表分页和缓存
**文件**:
- 更新 `ChatContainer.vue`
- 新增 `app-web/src/utils/cache.js`

**功能**:
- 分页加载会话（每页20条）
- IndexedDB 缓存
- 懒加载和无限滚动

**依赖**:
```bash
npm install idb
```

#### 12. 图片和附件优化
**文件**:
- 更新 `InputArea.vue`
- 新增 `app-web/src/utils/fileUpload.js`

**功能**:
- 图片压缩
- 上传进度显示
- 图片预览
- 大文件分片上传

**依赖**:
```bash
npm install browser-image-compression
```

---

## 🏗️ 第四阶段：架构重构（第7-8周）

### 优先级：🟡 中

#### 13. Pinia 状态管理
**文件**:
- `app-web/src/stores/chat.js`
- `app-web/src/stores/ui.js`
- 重构 `ChatContainer.vue`

**功能**:
- 集中管理聊天状态
- 会话管理
- 消息管理
- UI 状态管理

**依赖**: 已安装 Pinia

#### 14. TypeScript 或 JSDoc
**文件**: 所有 `.js` 文件

**功能**:
- 添加类型注解
- 提升代码提示
- 减少运行时错误

**依赖**: 
```bash
npm install -D typescript @types/node  # 如果选择 TypeScript
```

#### 15. 测试覆盖率提升
**文件**: `app-web/src/**/__tests__/`

**功能**:
- 组件单元测试
- API 服务测试
- 工具函数测试
- E2E 测试

**依赖**:
```bash
npm install -D @playwright/test  # E2E 测试
```

---

## 🤖 第五阶段：智能化功能（第9-10周）

### 优先级：🟡 中

#### 16. 智能上下文管理
**文件**:
- `app-web/src/utils/contextManager.js`
- 更新 `ChatContainer.vue`

**功能**:
- 自动管理上下文窗口
- Token 估算
- 长对话自动总结

**依赖**: API 客户端（第1项）

#### 17. 智能推荐
**文件**:
- `app-web/src/components/chat/PromptSuggestions.vue`
- 更新 `InputArea.vue`

**功能**:
- 提示词推荐
- 快捷回复
- 自动补全

**依赖**: 无

#### 18. 用户画像和标签系统实现
**文件**:
- 实现 `TagGenerationServiceImpl.java`
- 实现 `PersonalizationServiceImpl.java`
- 新增 NLP 服务

**功能**:
- 从对话中提取关键词
- 生成用户标签
- 基于标签个性化回复

**依赖**: 
```xml
<!-- 添加 NLP 库 -->
<dependency>
    <groupId>edu.stanford.nlp</groupId>
    <artifactId>stanford-corenlp</artifactId>
    <version>4.5.4</version>
</dependency>
```

---

## 📱 第六阶段：移动端优化（第11周）

### 优先级：🟡 中

#### 19. 触摸体验优化
**文件**: 更新所有组件的 CSS

**功能**:
- 确保触摸目标 ≥ 44px
- 添加触摸反馈动画
- 优化滑动手势
- 下拉刷新

**依赖**:
```bash
npm install pulltorefreshjs
```

#### 20. 键盘遮挡处理
**文件**: 
- `app-web/src/composables/useKeyboard.js`
- 更新 `InputArea.vue`

**功能**:
- 监听键盘弹出
- 自动调整布局
- 滚动到输入框

**依赖**: 无

---

## 💾 第七阶段：数据管理（第12周）

### 优先级：🟢 低

#### 21. 离线支持
**文件**:
- `app-web/public/sw.js`
- `app-web/src/registerServiceWorker.js`

**功能**:
- Service Worker 注册
- 离线缓存策略
- 离线时查看历史消息

**依赖**: 无

#### 22. 实时同步（WebSocket）
**文件**:
- 后端 WebSocket 配置
- `app-web/src/services/websocket.js`

**功能**:
- 多设备实时同步
- 新消息推送
- 在线状态同步

**依赖**: Spring WebSocket

#### 23. 数据导出和备份
**文件**:
- `app-web/src/utils/export.js`
- 新增导出按钮

**功能**:
- 导出为 JSON
- 导出为 Markdown
- 导出为 PDF（可选）

**依赖**:
```bash
npm install jspdf  # 如果需要 PDF 导出
```

---

## 🔒 第八阶段：安全加固（第13周）

### 优先级：🔴 高

#### 24. 认证系统
**文件**:
- 后端 Spring Security 配置
- `app-web/src/views/Login.vue`
- `app-web/src/stores/auth.js`

**功能**:
- 用户注册/登录
- JWT Token 认证
- 刷新 Token 机制
- CSRF 保护

**依赖**: Spring Security

---

## 📊 实施时间表

| 阶段 | 周数 | 优先级 | 任务数 | 关键里程碑 |
|------|------|--------|--------|-----------|
| 第一阶段 | 1-2周 | 🔴 紧急 | 5项 | 基础架构完成 |
| 第二阶段 | 3-4周 | 🔴 高 | 4项 | 用户体验提升 |
| 第三阶段 | 5-6周 | 🟡 中高 | 3项 | 性能优化完成 |
| 第四阶段 | 7-8周 | 🟡 中 | 3项 | 架构重构完成 |
| 第五阶段 | 9-10周 | 🟡 中 | 3项 | 智能化功能上线 |
| 第六阶段 | 11周 | 🟡 中 | 2项 | 移动端优化 |
| 第七阶段 | 12周 | 🟢 低 | 3项 | 数据管理完善 |
| 第八阶段 | 13周 | 🔴 高 | 1项 | 安全加固 |

**总计：约 3 个月完成所有优化**

---

## 🚀 快速开始

### 立即开始（今天）

1. **创建 Toast 组件**（最快见效）
2. **封装 API 客户端**（后续依赖）
3. **添加环境变量**（部署必需）

### 本周完成

4. **XSS 防护**（安全关键）
5. **线程池优化**（稳定性关键）

### 下周开始

6. **Markdown 支持**（体验提升）
7. **消息操作**（基本功能）

---

## 📦 依赖安装清单

```bash
# 第一阶段
npm install dompurify

# 第二阶段
npm install marked highlight.js
npm install katex  # 可选

# 第三阶段
npm install vue-virtual-scroller
npm install idb
npm install browser-image-compression

# 第六阶段
npm install pulltorefreshjs

# 第七阶段
npm install jspdf  # 可选
```

```xml
<!-- 后端依赖 -->
<!-- pom.xml -->
<dependency>
    <groupId>edu.stanford.nlp</groupId>
    <artifactId>stanford-corenlp</artifactId>
    <version>4.5.4</version>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

---

## ✅ 验收标准

每个阶段完成后需要验证：

### 第一阶段验收
- [ ] Toast 通知正常显示
- [ ] API 错误能被捕获并提示
- [ ] 环境变量正确加载
- [ ] XSS 攻击被阻止
- [ ] 线程池不会无限增长

### 第二阶段验收
- [ ] Markdown 正确渲染
- [ ] 代码高亮显示
- [ ] 消息可以复制/重新生成
- [ ] 搜索功能正常
- [ ] 加载状态友好

### 第三阶段验收
- [ ] 1000+ 条消息不卡顿
- [ ] 会话列表分页加载
- [ ] 图片自动压缩
- [ ] 上传进度显示

### 后续阶段类似...

---

## 🎯 成功指标

优化完成后，系统应达到：

- **性能**: 首屏加载 < 2s，流式响应延迟 < 100ms
- **稳定性**: 无内存泄漏，无线程耗尽
- **安全性**: 通过基本安全审计
- **用户体验**: 用户满意度 > 90%
- **代码质量**: 测试覆盖率 > 70%

---

## 📞 需要帮助？

如果在实施过程中遇到问题：
1. 查看 `DEEP_OPTIMIZATION_ANALYSIS.md` 的详细说明
2. 参考代码示例
3. 随时向我提问

**准备好开始了吗？我们从哪个任务开始？**
