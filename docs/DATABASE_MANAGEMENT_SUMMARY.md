# 数据库管理体系总结

## 概述

VoiceBox 项目现已建立完整的数据库管理体系，支持在任何新环境中快速初始化和部署。

## 核心组件

### 1. 自动化初始化脚本

**位置**: `deploy/db/init-database.sh`

**功能**:
- ✅ 自动检查 MySQL 环境
- ✅ 创建数据库和用户
- ✅ 执行所有表结构脚本
- ✅ 创建索引和优化
- ✅ 验证安装结果
- ✅ 支持本地和远程部署
- ✅ 可选加载测试数据

**使用**:
```bash
cd deploy/db
./init-database.sh
```

### 2. 模块化表结构脚本

**位置**: `deploy/db/schema/`

| 文件 | 说明 | 包含的表 |
|------|------|----------|
| 01-base-tables.sql | 基础聊天功能 | chat_session, chat_message, chat_history |
| 02-user-tables.sql | 用户管理 | users, devices, interactions |
| 03-personality-tables.sql | 个性化分析（V2.0） | user_profiles, conversation_features, user_tags, user_feedback |
| 04-indexes.sql | 性能优化 | 各种索引和复合索引 |

**优势**:
- 模块化设计，易于维护
- 可以单独执行某个模块
- 清晰的功能划分
- 便于版本控制

### 3. 数据库迁移系统

**位置**: `deploy/db/migrations/`

**包含**:
- `template.sql` - 迁移脚本模板
- `README.md` - 详细的迁移指南

**使用流程**:
```bash
# 1. 创建迁移脚本
cp template.sql 20241129_120000_description.sql

# 2. 编辑脚本

# 3. 执行迁移
mysql -u voicebox -pvoicebox123 voicebox_db < 20241129_120000_description.sql
```

### 4. 测试数据

**位置**: `deploy/db/test-data/sample-data.sql`

**包含**:
- 3 个测试用户
- 3 个聊天会话
- 8 条聊天消息
- 用户画像数据
- 标签和反馈数据

**用途**: 仅用于开发和测试环境

### 5. 完整文档

| 文档 | 说明 |
|------|------|
| deploy/db/README.md | 数据库脚本使用指南 |
| deploy/db/migrations/README.md | 迁移脚本详细文档 |
| docs/DATABASE_SETUP_GUIDE.md | 数据库设置完整指南 |
| app-device/DATABASE_SCHEMA.md | 数据库架构文档 |

## 使用场景

### 场景 1: 全新环境部署

```bash
# 一键初始化
cd deploy/db
./init-database.sh -p your_password

# 验证
mysql -u voicebox -pvoicebox123 voicebox_db -e "SHOW TABLES;"
```

### 场景 2: 远程服务器部署

```bash
cd deploy/db
./init-database.sh -h 129.211.180.183 -u root -p your_password
```

### 场景 3: 开发环境（含测试数据）

```bash
cd deploy/db
./init-database.sh --test-data
```

### 场景 4: 数据库结构变更

```bash
# 创建迁移脚本
cd deploy/db/migrations
cp template.sql 20241129_add_new_feature.sql

# 编辑并执行
mysql -u voicebox -pvoicebox123 voicebox_db < 20241129_add_new_feature.sql
```

### 场景 5: 手动分步执行

```bash
# 1. 创建数据库
mysql -u root -p < deploy/db/quick-init.sql

# 2. 创建基础表
mysql -u voicebox -pvoicebox123 voicebox_db < deploy/db/schema/01-base-tables.sql

# 3. 创建用户表
mysql -u voicebox -pvoicebox123 voicebox_db < deploy/db/schema/02-user-tables.sql

# 4. 创建个性化表
mysql -u voicebox -pvoicebox123 voicebox_db < deploy/db/schema/03-personality-tables.sql

# 5. 创建索引
mysql -u voicebox -pvoicebox123 voicebox_db < deploy/db/schema/04-indexes.sql
```

## 数据库结构

### 表统计

- **总表数**: 10 张
- **基础表**: 3 张（聊天功能）
- **用户表**: 3 张（用户管理）
- **个性化表**: 4 张（V2.0 功能）

### 关系图

```
users (用户)
  ├── user_profiles (画像)
  ├── user_tags (标签)
  ├── user_feedback (反馈)
  ├── devices (设备)
  ├── interactions (交互)
  └── conversation_features (对话特征)

chat_session (会话)
  ├── chat_message (消息)
  └── chat_history (历史)
```

## 配置管理

### 默认配置

```properties
DB_HOST=localhost
DB_PORT=3306
DB_NAME=voicebox_db
DB_USER=voicebox
DB_PASSWORD=voicebox123
```

### 应用配置

**config.properties**:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/voicebox_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
spring.datasource.username=voicebox
spring.datasource.password=voicebox123
```

## 维护操作

### 备份

```bash
# 完整备份
mysqldump -u voicebox -pvoicebox123 voicebox_db > backup_$(date +%Y%m%d_%H%M%S).sql

# 仅结构
mysqldump -u voicebox -pvoicebox123 --no-data voicebox_db > schema_backup.sql
```

### 恢复

```bash
mysql -u voicebox -pvoicebox123 voicebox_db < backup_20241129_120000.sql
```

### 优化

```bash
# 优化表
mysql -u voicebox -pvoicebox123 voicebox_db -e "OPTIMIZE TABLE chat_message;"

# 分析表
mysql -u voicebox -pvoicebox123 voicebox_db -e "ANALYZE TABLE user_profiles;"
```

## 版本兼容性

- **MySQL**: 5.7+
- **字符集**: utf8mb4
- **排序规则**: utf8mb4_unicode_ci
- **存储引擎**: InnoDB

### MySQL 5.7 特殊处理

- TIMESTAMP 字段使用 `NULL DEFAULT NULL`
- JSON 类型使用 TEXT 替代
- 避免使用 MySQL 8.0 特有语法

## 最佳实践

### 1. 新环境部署

✅ 使用自动化脚本  
✅ 先在测试环境验证  
✅ 备份现有数据（如果有）  
✅ 在低峰期执行  
✅ 执行后验证

### 2. 结构变更

✅ 使用迁移脚本  
✅ 包含回滚方案  
✅ 先在测试环境执行  
✅ 记录变更历史  
✅ 更新文档

### 3. 数据维护

✅ 定期备份  
✅ 监控表大小  
✅ 优化慢查询  
✅ 清理过期数据  
✅ 检查索引效率

### 4. 安全管理

✅ 使用强密码  
✅ 限制远程访问  
✅ 定期更新密码  
✅ 不在代码中硬编码密码  
✅ 使用环境变量或配置文件

## 故障排查

### 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 连接被拒绝 | MySQL 未启动 | `systemctl start mysql` |
| 权限不足 | 用户权限未授予 | 重新执行 GRANT 语句 |
| 表已存在 | 重复执行脚本 | 使用 IF NOT EXISTS 或删除表 |
| 字符集错误 | 字符集不匹配 | 修改数据库字符集 |

### 检查命令

```bash
# 检查 MySQL 状态
systemctl status mysql

# 检查端口
netstat -tlnp | grep 3306

# 检查字符集
mysql -u voicebox -pvoicebox123 voicebox_db -e "SHOW VARIABLES LIKE 'character_set%';"

# 检查表
mysql -u voicebox -pvoicebox123 voicebox_db -e "SHOW TABLES;"
```

## 文档索引

### 快速开始
- [数据库设置指南](DATABASE_SETUP_GUIDE.md)
- [数据库脚本文档](../deploy/db/README.md)

### 详细文档
- [数据库架构文档](../app-device/DATABASE_SCHEMA.md)
- [迁移脚本文档](../deploy/db/migrations/README.md)
- [部署指南](../deploy/README.md)

### 修复记录
- [线上环境修复报告](DEPLOYMENT_FIX_2024_11_29.md)

## 更新历史

| 日期 | 版本 | 说明 |
|------|------|------|
| 2024-11-29 | 1.0 | 创建完整的数据库管理体系 |
| 2024-11-29 | 1.0 | 添加自动化初始化脚本 |
| 2024-11-29 | 1.0 | 创建模块化表结构脚本 |
| 2024-11-29 | 1.0 | 建立迁移脚本系统 |
| 2024-11-29 | 1.0 | 添加测试数据 |
| 2024-11-29 | 1.0 | 完善文档体系 |

## 下一步计划

- [ ] 添加数据库监控脚本
- [ ] 创建自动备份脚本
- [ ] 添加性能分析工具
- [ ] 建立数据清理策略
- [ ] 添加数据库健康检查

## 总结

VoiceBox 项目现已具备：

✅ **完整的初始化体系** - 支持一键部署到任何环境  
✅ **模块化的表结构** - 易于维护和扩展  
✅ **规范的迁移系统** - 安全地进行结构变更  
✅ **丰富的测试数据** - 便于开发和测试  
✅ **详细的文档** - 覆盖所有使用场景  

这套体系确保了数据库管理的：
- **可重复性** - 在任何环境都能得到一致的结果
- **可维护性** - 清晰的结构和完整的文档
- **可扩展性** - 易于添加新表和功能
- **安全性** - 规范的操作流程和权限管理

---

**维护者**: VoiceBox Team  
**最后更新**: 2024-11-29
