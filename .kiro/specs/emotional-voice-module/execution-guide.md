# 情感化语音交互模块 - 执行指南

## 开始之前

### 前置条件

1. ✅ 已完成voice-interaction基础功能
2. ✅ 已完成personality-analysis用户画像系统
3. ✅ MySQL数据库运行正常
4. ✅ 语音服务可用（豆包或Mock）
5. ✅ 开发环境配置完成

### 检查清单

```bash
# 检查数据库
mysql -h 129.211.180.183 -u voicebox -pvoicebox123 voicebox_db -e "SHOW TABLES;"

# 检查语音API
curl http://129.211.180.183:10088/api/voice/synthesize

# 检查用户画像API
curl http://129.211.180.183:10088/api/personality/profile/1
```

---

## 执行步骤

### 第1步：创建数据库表（任务1）

<function_calls>
<invoke name="taskStatus">
<parameter name="taskFilePath">.kiro/specs/emotional-voice-module/tasks.md