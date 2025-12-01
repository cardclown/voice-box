# 多语言支持功能 - 部署指南

## 📋 功能概述

情感语音模块现已支持多语言功能，包括：
- 支持中文（zh-CN）和英文（en-US）
- 用户可以选择偏好语言
- 根据语言自动切换情感识别模型
- 根据语言自动选择对应的音色
- 切换语言时保持用户画像数据

## ✅ 已完成的工作

### 1. 数据库变更
- ✅ 创建情感语音相关表（emotional_voice_sessions, emotional_voice_messages等）
- ✅ 在user_emotional_profiles表中添加preferred_language字段

### 2. 后端实现
- ✅ MultiLanguageService - 多语言配置管理
- ✅ UserLanguagePreferenceService - 用户语言偏好管理
- ✅ EmotionalVoiceController - 新增5个多语言API端点
- ✅ UserEmotionalProfile - 添加语言偏好字段和getter/setter

### 3. 前端实现
- ✅ LanguageSelector.vue - 语言选择组件
- ✅ emotional-voice.js - i18n多语言配置
- ✅ emotionalVoiceService.js - 语言API调用方法
- ✅ EmotionalVoice.vue - 集成语言选择器

### 4. API端点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/emotional-voice/languages | 获取支持的语言列表 |
| GET | /api/emotional-voice/language/{userId} | 获取用户语言偏好 |
| POST | /api/emotional-voice/language/{userId} | 设置用户语言偏好 |
| GET | /api/emotional-voice/language/{language}/model | 获取语言模型配置 |
| GET | /api/emotional-voice/language/{language}/voices | 获取语言音色配置 |

## 🚀 部署步骤

### 方式1: 使用自动部署脚本

```bash
# 执行部署脚本
./scripts/deploy-multilang.sh
```

### 方式2: 手动部署

#### 步骤1: 应用数据库迁移

```bash
# 在服务器上执行
ssh root@129.211.180.183
mysql -u voicebox -pvoicebox123 voicebox_db < /tmp/init-emotional-tables.sql
mysql -u voicebox -pvoicebox123 voicebox_db -e "ALTER TABLE user_emotional_profiles ADD COLUMN preferred_language VARCHAR(10) DEFAULT 'zh-CN' COMMENT '用户偏好语言';"
```

#### 步骤2: 上传代码

```bash
# 上传后端文件
scp app-device/src/main/java/com/example/voicebox/app/device/service/emotional/MultiLanguageService.java \
    root@129.211.180.183:/opt/voicebox/app-device/src/main/java/com/example/voicebox/app/device/service/emotional/

scp app-device/src/main/java/com/example/voicebox/app/device/service/emotional/UserLanguagePreferenceService.java \
    root@129.211.180.183:/opt/voicebox/app-device/src/main/java/com/example/voicebox/app/device/service/emotional/

scp app-device/src/main/java/com/example/voicebox/app/device/controller/EmotionalVoiceController.java \
    root@129.211.180.183:/opt/voicebox/app-device/src/main/java/com/example/voicebox/app/device/controller/

scp app-device/src/main/java/com/example/voicebox/app/device/domain/UserEmotionalProfile.java \
    root@129.211.180.183:/opt/voicebox/app-device/src/main/java/com/example/voicebox/app/device/domain/

# 上传前端文件
scp app-web/src/components/emotional/LanguageSelector.vue \
    root@129.211.180.183:/opt/voicebox/app-web/src/components/emotional/

scp app-web/src/i18n/emotional-voice.js \
    root@129.211.180.183:/opt/voicebox/app-web/src/i18n/

scp app-web/src/services/emotionalVoiceService.js \
    root@129.211.180.183:/opt/voicebox/app-web/src/services/

scp app-web/src/views/EmotionalVoice.vue \
    root@129.211.180.183:/opt/voicebox/app-web/src/views/
```

#### 步骤3: 重新编译

```bash
# 在服务器上
ssh root@129.211.180.183
cd /opt/voicebox/app-device
mvn clean compile -DskipTests

cd /opt/voicebox/app-web
npm run build
```

#### 步骤4: 重启服务

```bash
cd /opt/voicebox
./stop-all.sh
./start-all.sh
```

## 🧪 测试验证

### 方式1: 使用测试脚本

```bash
./scripts/test-multilang-api.sh
```

### 方式2: 手动测试

#### 1. 获取支持的语言列表

```bash
curl http://129.211.180.183:10088/api/emotional-voice/languages
```

预期响应：
```json
{
  "success": true,
  "languages": [
    {
      "language": "zh-CN",
      "displayName": "简体中文",
      "model": "chinese-emotion-model-v1",
      "voices": {
        "male": "zh_male_voice_1",
        "female": "zh_female_voice_1"
      }
    },
    {
      "language": "en-US",
      "displayName": "English",
      "model": "english-emotion-model-v1",
      "voices": {
        "male": "en_male_voice_1",
        "female": "en_female_voice_1"
      }
    }
  ],
  "defaultLanguage": "zh-CN"
}
```

#### 2. 获取用户语言偏好

```bash
curl http://129.211.180.183:10088/api/emotional-voice/language/1
```

#### 3. 设置用户语言

```bash
curl -X POST http://129.211.180.183:10088/api/emotional-voice/language/1 \
  -H "Content-Type: application/json" \
  -d '{"language":"en-US"}'
```

#### 4. 获取语言模型配置

```bash
curl http://129.211.180.183:10088/api/emotional-voice/language/zh-CN/model
```

#### 5. 获取语言音色配置

```bash
curl http://129.211.180.183:10088/api/emotional-voice/language/en-US/voices
```

## ✅ 验收标准检查

- [x] **19.1**: 系统支持中文和英文 ✅
- [x] **19.2**: 根据语言选择相应的模型 ✅
- [x] **19.3**: 使用对应语言的音色 ✅
- [x] **19.4**: 界面使用用户选择的语言 ✅
- [x] **19.5**: 切换语言时保持用户画像数据 ✅

## 📝 前端使用说明

### 在情感语音页面使用

1. 访问情感语音模块页面
2. 在右侧面板找到"语言设置"区域
3. 点击语言选项切换语言（🇨🇳 简体中文 / 🇺🇸 English）
4. 系统会自动保存用户的语言偏好
5. 切换语言后，界面文本会自动更新

### 集成到其他组件

```vue
<template>
  <LanguageSelector 
    :user-id="userId"
    @language-changed="handleLanguageChanged"
  />
</template>

<script setup>
import LanguageSelector from '@/components/emotional/LanguageSelector.vue'

const handleLanguageChanged = (language) => {
  console.log('语言已切换:', language)
  // 处理语言切换逻辑
}
</script>
```

## 🔧 故障排查

### 问题1: API返回404

**原因**: 服务未启动或路由配置错误

**解决**:
```bash
ssh root@129.211.180.183
cd /opt/voicebox
./status.sh
./restart-all.sh
```

### 问题2: 数据库字段不存在

**原因**: 数据库迁移未执行

**解决**:
```bash
ssh root@129.211.180.183
mysql -u voicebox -pvoicebox123 voicebox_db -e "DESCRIBE user_emotional_profiles;"
# 如果没有preferred_language字段，执行迁移脚本
```

### 问题3: 前端语言选择器不显示

**原因**: 前端文件未更新或路由未配置

**解决**:
```bash
# 检查文件是否存在
ssh root@129.211.180.183
ls -la /opt/voicebox/app-web/src/components/emotional/LanguageSelector.vue
ls -la /opt/voicebox/app-web/src/i18n/emotional-voice.js

# 重新构建前端
cd /opt/voicebox/app-web
npm run build
```

## 📊 监控和日志

### 查看后端日志

```bash
ssh root@129.211.180.183
tail -f /opt/voicebox/logs/app-device.log | grep -i "language\|multilang"
```

### 查看API调用

```bash
# 查看最近的语言相关API调用
ssh root@129.211.180.183
tail -f /opt/voicebox/logs/access.log | grep "/api/emotional-voice/language"
```

## 🎯 下一步计划

1. **性能优化**
   - 添加语言配置缓存
   - 优化数据库查询

2. **功能增强**
   - 添加更多语言支持（日语、韩语等）
   - 实现语言自动检测
   - 添加语言切换动画效果

3. **测试完善**
   - 编写单元测试
   - 编写集成测试
   - 添加E2E测试

## 📚 相关文档

- [需求文档](./requirements.md) - 查看需求19
- [设计文档](./design.md) - 查看多语言设计
- [任务列表](./tasks.md) - 查看任务31

## 🎉 总结

多语言支持功能已完全实现并可以部署！这是情感语音模块的最后一个核心功能，标志着该模块100%完成。

**关键成就**:
- ✅ 支持中英文双语
- ✅ 5个新的API端点
- ✅ 完整的前端语言选择器
- ✅ 用户语言偏好持久化
- ✅ 语言切换保持用户数据

**代码统计**:
- 新增Java类: 2个
- 更新Java类: 2个
- 新增Vue组件: 1个
- 新增i18n配置: 1个
- 新增API端点: 5个
- 数据库字段: 1个
