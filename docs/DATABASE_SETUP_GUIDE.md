# 数据库设置指南

本指南介绍如何在新环境中快速设置 VoiceBox 数据库。

## 快速开始

### 方式 1: 使用自动化脚本（推荐）

```bash
cd deploy/db
./init-database.sh
```

脚本会提示输入 MySQL root 密码，然后自动完成：
- ✅ 创建数据库 `voicebox_db`
- ✅ 创建用户 `voicebox`
- ✅ 创建所有表结构
- ✅ 创建索引
- ✅ 验证安装

### 方式 2: 手动执行 SQL 脚本

```bash
# 1. 创建数据库和用户
mysql -u root -p < deploy/db/quick-init.sql

# 2. 创建表结构
mysql -u voicebox -pvoicebox123 voicebox_db < deploy/db/schema/01-base-tables.sql
mysql -u voicebox -pvoicebox123 voicebox_db < deploy/db/schema/02-user-tables.sql
mysql -u voicebox -pvoicebox123 voicebox_db < deploy/db/schema/03-personality-tables.sql
mysql -u voicebox -pvoicebox123 voicebox_db < deploy/db/schema/04-indexes.sql

# 3. （可选）加载测试数据
mysql -u voicebox -pvoicebox123 voicebox_db < deploy/db/test-data/sample-data.sql
```

### 方式 3: 使用完整的 SQL 文件

```bash
# 使用单个文件初始化（包含所有表）
mysql -u root -p < deploy/init-database.sql
```

## 不同场景的使用方法

### 场景 1: 本地开发环境初始化

```bash
cd deploy/db
./init-database.sh --test-data
```

这会创建数据库并加载测试数据，方便开发调试。

### 场景 2: 远程服务器初始化

```bash
cd deploy/db
./init-database.sh -h 129.211.180.183 -u root -p your_password
```

### 场景 3: 只创建表结构（数据库已存在）

```bash
cd deploy/db
./init-database.sh --skip-create-db
```

### 场景 4: 自定义数据库名称和用户

```bash
cd deploy/db
./init-database.sh \
  -d my_voicebox_db \
  --db-user my_user \
  --db-password my_password
```

## 验证安装

### 1. 检查数据库和表

```bash
mysql -u voicebox -pvoicebox123 voicebox_db -e "SHOW TABLES;"
```

应该看到以下表：
```
+-------------------------+
| Tables_in_voicebox_db   |
+-------------------------+
| chat_history            |
| chat_message            |
| chat_session            |
| conversation_features   |
| devices                 |
| interactions            |
| user_feedback           |
| user_profiles           |
| user_tags               |
| users                   |
+-------------------------+
```

### 2. 检查表结构

```bash
mysql -u voicebox -pvoicebox123 voicebox_db -e "DESCRIBE user_profiles;"
```

### 3. 检查数据

```bash
mysql -u voicebox -pvoicebox123 voicebox_db -e "SELECT * FROM users;"
```

应该至少有一个默认用户。

## 配置应用连接

初始化完成后，更新应用配置文件：

### config.properties

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/voicebox_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
spring.datasource.username=voicebox
spring.datasource.password=voicebox123
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

### 环境变量方式

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=voicebox_db
export DB_USER=voicebox
export DB_PASSWORD=voicebox123
```

## 数据库结构说明

### 基础表（聊天功能）

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| chat_session | 聊天会话 | id, title, model, created_at |
| chat_message | 聊天消息 | id, session_id, role, content |
| chat_history | 聊天历史 | id, session_id, user_message, assistant_message |

### 用户表

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| users | 用户信息 | id, username, email, preferences |
| devices | 设备管理 | id, user_id, device_type, api_key |
| interactions | 交互记录 | id, user_id, interaction_type, created_at |

### 个性化分析表（V2.0）

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| user_profiles | 用户画像 | id, user_id, openness, extraversion, confidence_score |
| conversation_features | 对话特征 | id, user_id, message_length, sentiment_score |
| user_tags | 用户标签 | id, user_id, category, tag_name, confidence |
| user_feedback | 用户反馈 | id, user_id, feedback_type, feedback_content |

## 数据库迁移

当需要修改表结构时：

```bash
# 1. 创建迁移脚本
cd deploy/db/migrations
cp template.sql 20241129_120000_add_new_field.sql

# 2. 编辑脚本添加变更

# 3. 执行迁移
mysql -u voicebox -pvoicebox123 voicebox_db < 20241129_120000_add_new_field.sql
```

详见 [数据库迁移文档](../deploy/db/migrations/README.md)

## 备份和恢复

### 备份数据库

```bash
# 完整备份
mysqldump -u voicebox -pvoicebox123 voicebox_db > backup_$(date +%Y%m%d_%H%M%S).sql

# 仅备份结构
mysqldump -u voicebox -pvoicebox123 --no-data voicebox_db > schema_backup.sql
```

### 恢复数据库

```bash
# 恢复备份
mysql -u voicebox -pvoicebox123 voicebox_db < backup_20241129_120000.sql
```

## 故障排查

### 问题 1: 连接被拒绝

```bash
# 检查 MySQL 是否运行
systemctl status mysql

# 检查端口
netstat -tlnp | grep 3306
```

### 问题 2: 权限不足

```bash
# 重新授权
mysql -u root -p -e "GRANT ALL PRIVILEGES ON voicebox_db.* TO 'voicebox'@'localhost';"
mysql -u root -p -e "FLUSH PRIVILEGES;"
```

### 问题 3: 表已存在

```bash
# 删除现有数据库（谨慎！）
mysql -u root -p -e "DROP DATABASE IF EXISTS voicebox_db;"

# 重新初始化
cd deploy/db && ./init-database.sh
```

### 问题 4: 字符集问题

```bash
# 检查字符集
mysql -u voicebox -pvoicebox123 voicebox_db -e "SHOW VARIABLES LIKE 'character_set%';"

# 修改字符集
mysql -u root -p -e "ALTER DATABASE voicebox_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

## 性能优化建议

### 1. 定期优化表

```bash
mysql -u voicebox -pvoicebox123 voicebox_db -e "OPTIMIZE TABLE chat_message;"
```

### 2. 分析慢查询

```bash
# 启用慢查询日志
mysql -u root -p -e "SET GLOBAL slow_query_log = 'ON';"
mysql -u root -p -e "SET GLOBAL long_query_time = 2;"
```

### 3. 监控表大小

```bash
mysql -u voicebox -pvoicebox123 voicebox_db -e "
SELECT 
    table_name AS 'Table',
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'Size (MB)'
FROM information_schema.TABLES
WHERE table_schema = 'voicebox_db'
ORDER BY (data_length + index_length) DESC;
"
```

## 环境要求

- MySQL: 5.7+
- 字符集: utf8mb4
- 排序规则: utf8mb4_unicode_ci
- 存储引擎: InnoDB

## 相关文档

- [数据库架构文档](../app-device/DATABASE_SCHEMA.md)
- [数据库脚本文档](../deploy/db/README.md)
- [迁移脚本文档](../deploy/db/migrations/README.md)
- [部署指南](../deploy/README.md)

## 常见问题

### Q: 如何重置数据库？

```bash
# 删除并重新创建
mysql -u root -p -e "DROP DATABASE IF EXISTS voicebox_db;"
cd deploy/db && ./init-database.sh
```

### Q: 如何更改数据库密码？

```bash
mysql -u root -p -e "ALTER USER 'voicebox'@'localhost' IDENTIFIED BY 'new_password';"
mysql -u root -p -e "ALTER USER 'voicebox'@'%' IDENTIFIED BY 'new_password';"
mysql -u root -p -e "FLUSH PRIVILEGES;"
```

### Q: 如何在生产环境部署？

1. 备份现有数据（如果有）
2. 在低峰期执行初始化脚本
3. 验证所有表和数据
4. 更新应用配置
5. 重启应用服务
6. 监控日志和性能

### Q: 测试数据可以在生产环境使用吗？

**不可以！** 测试数据仅用于开发和测试环境。生产环境不要加载测试数据。

## 下一步

数据库设置完成后：

1. ✅ 更新应用配置文件
2. ✅ 启动后端服务: `./start-all.sh`
3. ✅ 验证功能: `./status.sh`
4. ✅ 测试 API 端点
5. ✅ 检查日志

---

**最后更新**: 2024-11-29  
**维护者**: VoiceBox Team
