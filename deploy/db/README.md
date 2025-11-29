# 数据库管理脚本

本目录包含 VoiceBox 项目的所有数据库相关脚本，用于在新环境中快速初始化数据库。

## 目录结构

```
deploy/db/
├── README.md                    # 本文件
├── init-database.sh             # 数据库初始化主脚本
├── schema/                      # 数据库表结构
│   ├── 01-base-tables.sql      # 基础表（聊天相关）
│   ├── 02-user-tables.sql      # 用户相关表
│   ├── 03-personality-tables.sql # 个性化分析表
│   └── 04-indexes.sql          # 索引和优化
├── migrations/                  # 数据库迁移脚本
│   └── README.md               # 迁移脚本说明
└── test-data/                   # 测试数据
    └── sample-data.sql         # 示例数据

```

## 快速开始

### 1. 全新环境初始化

在全新环境中初始化数据库：

```bash
# 方式 1: 使用自动化脚本（推荐）
cd deploy/db
./init-database.sh

# 方式 2: 手动执行
mysql -u root -p < schema/01-base-tables.sql
mysql -u root -p < schema/02-user-tables.sql
mysql -u root -p < schema/03-personality-tables.sql
mysql -u root -p < schema/04-indexes.sql
```

### 2. 验证数据库

```bash
# 检查所有表是否创建成功
mysql -u voicebox -pvoicebox123 voicebox_db -e "SHOW TABLES;"

# 检查表结构
mysql -u voicebox -pvoicebox123 voicebox_db -e "DESCRIBE user_profiles;"
```

### 3. 加载测试数据（可选）

```bash
mysql -u voicebox -pvoicebox123 voicebox_db < test-data/sample-data.sql
```

## 脚本说明

### init-database.sh

主初始化脚本，自动完成以下操作：
1. 检查 MySQL 是否安装
2. 创建数据库和用户
3. 执行所有表结构脚本
4. 创建索引
5. 验证安装结果

**使用方法**:
```bash
./init-database.sh [选项]

选项:
  -h, --host HOST       MySQL 主机地址（默认: localhost）
  -u, --user USER       MySQL root 用户（默认: root）
  -p, --password PASS   MySQL root 密码
  -d, --database DB     数据库名称（默认: voicebox_db）
  --skip-create-db      跳过创建数据库步骤
  --test-data           加载测试数据
  --help                显示帮助信息
```

**示例**:
```bash
# 本地初始化
./init-database.sh -p mypassword

# 远程服务器初始化
./init-database.sh -h 129.211.180.183 -u root -p mypassword

# 只创建表结构，不创建数据库
./init-database.sh --skip-create-db

# 初始化并加载测试数据
./init-database.sh --test-data
```

### 表结构脚本

#### 01-base-tables.sql
基础聊天功能表：
- `chat_session` - 聊天会话
- `chat_message` - 聊天消息
- `chat_history` - 聊天历史

#### 02-user-tables.sql
用户管理表：
- `users` - 用户基本信息
- `devices` - 设备管理
- `interactions` - 用户交互记录

#### 03-personality-tables.sql
个性化分析表：
- `user_profiles` - 用户画像
- `conversation_features` - 对话特征
- `user_tags` - 用户标签
- `user_feedback` - 用户反馈

#### 04-indexes.sql
性能优化索引和约束

## 数据库迁移

当需要修改现有表结构时，使用迁移脚本：

```bash
# 创建新的迁移脚本
cd deploy/db/migrations
cp template.sql YYYYMMDD_HHMMSS_description.sql

# 执行迁移
mysql -u voicebox -pvoicebox123 voicebox_db < migrations/20241129_100000_add_user_preferences.sql
```

详见 `migrations/README.md`

## 环境配置

### 本地开发环境

```bash
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=voicebox_db
DB_USER=voicebox
DB_PASSWORD=voicebox123
```

### 生产环境

```bash
# 数据库配置（使用环境变量或配置文件）
DB_HOST=129.211.180.183
DB_PORT=3306
DB_NAME=voicebox_db
DB_USER=voicebox
DB_PASSWORD=voicebox123
```

## 备份和恢复

### 备份数据库

```bash
# 备份结构和数据
mysqldump -u voicebox -pvoicebox123 voicebox_db > backup_$(date +%Y%m%d_%H%M%S).sql

# 仅备份结构
mysqldump -u voicebox -pvoicebox123 --no-data voicebox_db > schema_backup.sql

# 仅备份数据
mysqldump -u voicebox -pvoicebox123 --no-create-info voicebox_db > data_backup.sql
```

### 恢复数据库

```bash
# 恢复完整备份
mysql -u voicebox -pvoicebox123 voicebox_db < backup_20241129_100000.sql

# 恢复到新数据库
mysql -u root -p -e "CREATE DATABASE voicebox_db_restore;"
mysql -u root -p voicebox_db_restore < backup_20241129_100000.sql
```

## 故障排查

### 常见问题

#### 1. 权限不足

```bash
# 错误: Access denied for user
# 解决: 检查用户权限
mysql -u root -p -e "GRANT ALL PRIVILEGES ON voicebox_db.* TO 'voicebox'@'localhost';"
mysql -u root -p -e "FLUSH PRIVILEGES;"
```

#### 2. 表已存在

```bash
# 错误: Table already exists
# 解决: 使用 IF NOT EXISTS 或删除现有表
mysql -u voicebox -pvoicebox123 voicebox_db -e "DROP TABLE IF EXISTS user_profiles;"
```

#### 3. 字符集问题

```bash
# 检查字符集
mysql -u voicebox -pvoicebox123 voicebox_db -e "SHOW VARIABLES LIKE 'character_set%';"

# 修改字符集
mysql -u root -p -e "ALTER DATABASE voicebox_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

#### 4. MySQL 版本兼容性

本项目使用 MySQL 5.7，注意：
- TIMESTAMP 字段需要显式设置 NULL DEFAULT NULL
- JSON 类型在某些版本中需要使用 TEXT 替代
- 某些语法在 MySQL 8.0 中可能不兼容

## 版本要求

- MySQL: 5.7+
- 字符集: utf8mb4
- 排序规则: utf8mb4_unicode_ci

## 相关文档

- [数据库架构文档](../../app-device/DATABASE_SCHEMA.md)
- [部署指南](../README.md)
- [环境同步规范](../../.kiro/steering/environment-sync.md)

## 维护日志

| 日期 | 版本 | 说明 | 作者 |
|------|------|------|------|
| 2024-11-29 | 1.0 | 初始版本，包含所有基础表 | AI Assistant |

## 注意事项

1. **生产环境操作前务必备份**
2. **测试数据不要在生产环境加载**
3. **密码不要提交到版本控制**
4. **定期检查和优化索引**
5. **监控数据库性能和大小**
