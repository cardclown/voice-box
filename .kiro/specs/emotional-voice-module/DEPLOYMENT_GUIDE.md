# 情感语音模块 - 部署指南

## 概述

情感语音模块已成功集成到VoiceBox应用中，提供语音情感分析、用户画像构建和情感化语音合成功能。

## 已完成的功能

### 后端服务 ✅
- 语音特征提取和分析
- 情绪识别（开心、悲伤、愤怒、平静、焦虑等）
- 性格特征识别
- 性别识别
- 语气风格分析
- 自动标签生成
- 用户情感画像管理
- 情感化语音合成
- REST API接口

### 前端界面 ✅
- 情感语音交互主页面
- 语音输入组件（支持录音和文本输入）
- 实时情绪反馈显示
- 标签云可视化
- 历史记录管理
- 统计图表展示
- 响应式设计（支持桌面和移动端）

### 状态管理 ✅
- Pinia状态管理
- API服务封装
- Composable函数

## 部署步骤

### 1. 数据库初始化

```bash
# 在服务器上执行SQL脚本
mysql -h 129.211.180.183 -u voicebox -pvoicebox123 voicebox_db < .kiro/specs/emotional-voice-module/init-emotional-tables.sql
```

### 2. 后端部署

```bash
# 编译后端代码
cd app-device
mvn clean install

# 启动后端服务
mvn spring-boot:run
```

### 3. 前端部署

```bash
# 安装依赖
cd app-web
npm install

# 开发模式
npm run dev

# 生产构建
npm run build
```

### 4. 验证部署

访问应用并：
1. 点击侧边栏的"Emotion"按钮
2. 测试语音录音或文本输入功能
3. 查看情绪反馈和标签
4. 浏览历史记录和统计数据

## API端点

### 语音分析
- `POST /api/emotional-voice/analyze` - 分析语音情感
- `POST /api/emotional-voice/analyze-text` - 分析文本情感

### 用户画像
- `GET /api/emotional-voice/profile/{userId}` - 获取用户画像
- `POST /api/emotional-voice/profile/{userId}` - 更新用户画像
- `DELETE /api/emotional-voice/profile/{userId}` - 清除用户画像

### 语音合成
- `POST /api/emotional-voice/synthesize/{userId}` - 情感化语音合成
- `POST /api/emotional-voice/synthesize/batch/{userId}` - 批量语音合成
- `GET /api/emotional-voice/synthesize/params/{userId}` - 获取推荐参数

### 统计和历史
- `GET /api/emotional-voice/statistics/{userId}` - 获取统计数据
- `GET /api/emotional-voice/history/{userId}` - 获取历史记录
- `DELETE /api/emotional-voice/history/{userId}/{recordId}` - 删除历史记录

### 健康检查
- `GET /api/emotional-voice/health` - 服务健康状态

## 使用说明

### 语音分析

1. 点击"开始录音"按钮
2. 说话（可以看到实时音量显示）
3. 点击"停止录音"
4. 系统自动分析并显示：
   - 主要情绪
   - 情绪强度
   - 性格特征
   - 情感标签

### 文本分析

1. 在文本输入框输入内容
2. 点击"分析文本"按钮
3. 查看分析结果

### 查看用户画像

在"标签可视化"区域可以看到：
- 标签云展示
- 标签分类统计
- 标签详情

### 历史记录

在"情绪历史"区域可以：
- 查看所有分析记录
- 筛选特定情绪
- 播放历史音频
- 删除记录

### 统计数据

在"情感统计"区域可以看到：
- 总分析次数
- 主导情绪
- 情绪分布饼图
- 情绪趋势图
- 性格雷达图

## 配置说明

### 前端配置

在 `app-web/src/services/emotionalVoiceService.js` 中可以配置：
- API超时时间
- 请求重试策略

在 `app-web/src/stores/emotionalVoiceStore.js` 中可以配置：
- 缓存有效期
- 分页大小

### 后端配置

在 `application.properties` 中可以配置：
- 数据库连接
- 语音服务API密钥
- 文件上传限制

## 注意事项

1. **浏览器兼容性**：需要支持Web Audio API的现代浏览器
2. **麦克风权限**：首次使用需要授予麦克风权限
3. **网络要求**：语音分析需要稳定的网络连接
4. **数据隐私**：语音数据仅用于分析，不会永久存储原始音频

## 故障排查

### 无法录音
- 检查浏览器麦克风权限
- 确认使用HTTPS或localhost
- 检查浏览器控制台错误

### API调用失败
- 检查后端服务是否运行
- 验证API端点配置
- 查看网络请求状态

### 数据不显示
- 检查用户ID是否正确
- 清除浏览器缓存
- 查看浏览器控制台错误

## 后续优化建议

### 可选功能（未实现）
- 数据加密和隐私保护（任务26-28）
- 性能优化和缓存（任务29）
- 错误处理增强（任务30）
- 多语言支持（任务31）

### 测试
- 单元测试（标记为*的任务）
- 集成测试
- 端到端测试

## 技术栈

- **后端**: Spring Boot, Java, MySQL
- **前端**: Vue 3, Pinia, Vite
- **语音**: Web Audio API, MediaRecorder API
- **图表**: 原生SVG实现

## 支持

如有问题，请查看：
- 设计文档：`.kiro/specs/emotional-voice-module/design.md`
- 需求文档：`.kiro/specs/emotional-voice-module/requirements.md`
- 任务列表：`.kiro/specs/emotional-voice-module/tasks.md`
