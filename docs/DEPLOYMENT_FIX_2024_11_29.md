# 线上环境修复报告

**日期**: 2024-11-29  
**问题**: 线上环境调用后端失败  
**状态**: ✅ 已修复

---

## 问题诊断

### 1. 初始症状
- 前端无法调用后端 API
- 用户报告"调用后端失败"

### 2. 排查过程

#### 后端服务检查
```bash
# 后端服务正常运行
ps aux | grep java
# 结果: Java 进程运行正常，端口 10088 监听中

# 基础 API 可以访问
curl http://localhost:10088/api/chat/sessions
# 结果: 返回正常数据
```

#### 数据库检查
```bash
# 检查数据库表
mysql -uvoicebox -pvoicebox123 voicebox_db -e 'SHOW TABLES;'
# 结果: 只有基础表 (chat_history, chat_message, chat_session)
# 问题: 缺少 V2.0 个性化分析功能所需的表
```

#### 后端日志分析
```bash
journalctl -u voicebox-backend -n 50
# 发现错误: Table 'voicebox_db.user_profiles' doesn't exist
```

#### 前端配置检查
```bash
# 检查前端 API 配置
cat app-web/.env.production
# 问题: VITE_API_BASE=https://api.voicebox.com/api (错误的地址)
# 应该: VITE_API_BASE=/api (相对路径，通过 Nginx 代理)
```

---

## 修复措施

### 1. 创建数据库表

**文件**: `deploy/init-database.sql`

创建了以下表：
- `users` - 用户基本信息
- `user_profiles` - 用户画像（大五人格 + 偏好）
- `conversation_features` - 对话特征提取
- `user_tags` - 用户标签
- `user_feedback` - 用户反馈
- `interactions` - 用户交互记录
- `devices` - 设备管理

**执行**:
```bash
# 上传 SQL 脚本
scp deploy/init-database.sql root@129.211.180.183:/tmp/

# 执行初始化
ssh root@129.211.180.183
mysql -uvoicebox -pvoicebox123 voicebox_db < /tmp/init-database.sql
```

**注意事项**:
- MySQL 5.7 对 TIMESTAMP 字段有限制，需要显式设置 `NULL DEFAULT NULL`
- JSON 类型在 MySQL 5.7 中需要改为 TEXT 类型

### 2. 修复前端 API 配置

**文件**: `app-web/.env.production`

**修改前**:
```env
VITE_API_BASE=https://api.voicebox.com/api
```

**修改后**:
```env
VITE_API_BASE=/api
```

**原因**: 
- 使用相对路径 `/api`，通过 Nginx 反向代理到后端
- 避免跨域问题
- 简化配置

### 3. 重新构建前端

```bash
# 本地构建
cd app-web
npm run build

# 打包并上传
tar -czf /tmp/app-web-dist.tar.gz -C app-web/dist .
scp /tmp/app-web-dist.tar.gz root@129.211.180.183:/tmp/

# 服务器上部署
ssh root@129.211.180.183
cd /opt/voicebox/app-web/dist
rm -rf *
tar -xzf /tmp/app-web-dist.tar.gz

# 重载 Nginx
systemctl reload nginx
```

---

## 验证测试

### 1. 数据库表验证
```bash
mysql -uvoicebox -pvoicebox123 voicebox_db -e 'SHOW TABLES;'
```

**结果**: ✅ 所有表已创建
```
chat_history
chat_message
chat_session
conversation_features
devices
interactions
user_feedback
user_profiles
user_tags
users
```

### 2. API 端点测试

#### 基础聊天 API
```bash
curl http://129.211.180.183/api/chat/sessions
```
**结果**: ✅ 返回会话列表

#### 用户画像 API
```bash
curl http://129.211.180.183/api/personality/profile/1
```
**结果**: ✅ 返回用户画像数据
```json
{
  "data": {
    "personalityType": "平衡型",
    "needsUpdate": true,
    "profile": {
      "id": 1,
      "userId": 1,
      "openness": 0.5000,
      "conscientiousness": 0.5000,
      "extraversion": 0.5000,
      "agreeableness": 0.5000,
      "neuroticism": 0.5000,
      ...
    }
  },
  "success": true
}
```

#### 用户信息 API
```bash
curl http://129.211.180.183/api/users/1/profile
```
**结果**: ✅ 返回用户信息

### 3. 前端访问测试

```bash
# 访问首页
curl -I http://129.211.180.183/
# 结果: HTTP/1.1 200 OK

# 检查前端资源
curl http://129.211.180.183/ | grep "index-"
# 结果: 新构建的资源文件已加载
```

### 4. 后端日志检查

```bash
journalctl -u voicebox-backend -n 20
```
**结果**: ✅ 无 `user_profiles` 表不存在的错误

---

## 环境同步

### 本地环境
- ✅ 数据库初始化脚本已创建: `deploy/init-database.sql`
- ✅ 前端配置已修复: `app-web/.env.production`
- ✅ 前端已重新构建
- ✅ 所有变更已提交到 Git

### 服务器环境
- ✅ 数据库表已创建
- ✅ 前端已部署
- ✅ Nginx 已重载
- ✅ 后端服务正常运行

---

## 根本原因分析

### 1. 数据库表缺失
- **原因**: V2.0 个性化分析功能开发时，只在代码中定义了实体类和 Repository，但没有创建对应的数据库表
- **影响**: 后端服务启动后，访问相关功能时报错 `Table doesn't exist`
- **教训**: 数据库变更必须同步到生产环境，需要维护数据库初始化脚本

### 2. 前端 API 配置错误
- **原因**: `.env.production` 中配置了错误的 API 地址
- **影响**: 前端无法正确调用后端 API
- **教训**: 生产环境配置需要使用相对路径，通过 Nginx 代理

---

## 预防措施

### 1. 数据库变更管理
- ✅ 创建 `deploy/init-database.sql` 作为标准初始化脚本
- ✅ 未来数据库变更需要创建迁移脚本放在 `deploy/migrations/`
- ✅ 部署前检查数据库表是否完整

### 2. 环境配置管理
- ✅ 维护 `env-example.properties` 作为配置模板
- ✅ 生产环境配置使用相对路径
- ✅ 部署前验证环境变量配置

### 3. 部署流程规范
- ✅ 部署前检查清单：
  - [ ] 数据库表是否完整
  - [ ] 环境变量配置是否正确
  - [ ] 前端构建是否成功
  - [ ] 后端编译是否成功
  - [ ] Nginx 配置是否正确

### 4. 测试流程
- ✅ 部署后必须进行完整测试：
  - [ ] 基础 API 测试
  - [ ] 新功能 API 测试
  - [ ] 前端页面访问测试
  - [ ] 后端日志检查

---

## 相关文件

### 新增文件
- `deploy/init-database.sql` - 数据库初始化脚本

### 修改文件
- `app-web/.env.production` - 前端生产环境配置
- `app-web/dist/*` - 前端构建产物

### 参考文档
- `app-device/DATABASE_SCHEMA.md` - 数据库架构文档
- `docs/versions/v2.0-personality-analysis/` - V2.0 功能文档
- `.kiro/steering/environment-sync.md` - 环境同步规范
- `.kiro/steering/testing-requirements.md` - 测试要求规范

---

## 总结

**问题**: 线上环境调用后端失败

**根本原因**:
1. 数据库缺少 V2.0 功能所需的表
2. 前端 API 配置错误

**解决方案**:
1. 创建并执行数据库初始化脚本
2. 修复前端 API 配置并重新构建部署

**当前状态**: ✅ 所有功能正常工作

**测试结果**:
- ✅ 数据库表完整
- ✅ 后端 API 正常响应
- ✅ 前端可以正常访问
- ✅ 无错误日志

**后续建议**:
1. 建立数据库迁移脚本管理机制
2. 完善部署前检查流程
3. 加强环境配置管理
4. 定期同步本地和服务器环境

---

**修复人员**: AI Assistant  
**审核状态**: 待审核  
**部署时间**: 2024-11-29 10:40 CST
