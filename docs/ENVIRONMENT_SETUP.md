# 环境设置指南

本文档说明如何设置开发环境和生产环境。

---

## 快速开始

### 1. 首次设置开发环境

```bash
# 1. 在服务器上创建开发数据库
chmod +x scripts/setup-dev-database.sh
./scripts/setup-dev-database.sh

# 2. 创建开发配置文件
cp env-example.properties config-dev.properties
# config-dev.properties 已经预配置好，无需修改

# 3. 启动开发环境
./start-all.sh dev
```

### 2. 首次设置生产环境（在服务器上）

```bash
# 1. 创建生产配置文件
cp env-example.properties config-prod.properties
# 编辑 config-prod.properties，确认配置正确

# 2. 启动生产环境
./start-all.sh prod
```

---

## 环境说明

### 开发环境 (dev)

**用途**: 本地开发和测试

**数据库**: 
- 主机: 129.211.180.183
- 数据库: voicebox_dev
- 用户: voicebox_dev
- 密码: dev123

**特点**:
- 使用独立的开发数据库
- 开启调试模式和详细日志
- 前端使用开发服务器（热重载）
- 可以随意测试，不影响生产

**启动命令**:
```bash
./start-all.sh dev
```

**访问地址**:
- 前端: http://localhost:5173
- 后端: http://localhost:10088

---

### 生产环境 (prod)

**用途**: 线上服务

**数据库**:
- 主机: localhost（服务器本地）
- 数据库: voicebox_db
- 用户: voicebox
- 密码: voicebox123

**特点**:
- 使用生产数据库
- 关闭调试模式
- 前端构建为静态文件
- 日志级别为 INFO

**启动命令**（在服务器上）:
```bash
./start-all.sh prod
```

---

## 详细设置步骤

### 步骤 1: 设置开发数据库

开发环境使用服务器上的独立数据库 `voicebox_dev`，与生产数据库 `voicebox_db` 完全隔离。

**自动设置**（推荐）:
```bash
./scripts/setup-dev-database.sh
```

**手动设置**:
```bash
# 连接到服务器
ssh root@129.211.180.183

# 创建数据库和用户
mysql -u root -proot123 << 'EOF'
CREATE DATABASE IF NOT EXISTS voicebox_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'voicebox_dev'@'%' IDENTIFIED BY 'dev123';
GRANT ALL PRIVILEGES ON voicebox_dev.* TO 'voicebox_dev'@'%';
FLUSH PRIVILEGES;
EOF

# 初始化数据库结构
cd /opt/voicebox
for schema_file in deploy/db/schema/*.sql; do
    mysql -u voicebox_dev -pdev123 voicebox_dev < "$schema_file"
done

# 插入测试数据
mysql -u voicebox_dev -pdev123 voicebox_dev < deploy/db/test-data/sample-data.sql

# 验证
mysql -u voicebox_dev -pdev123 voicebox_dev -e "SHOW TABLES;"
```

### 步骤 2: 配置开发环境

**创建配置文件**:
```bash
cp env-example.properties config-dev.properties
```

**config-dev.properties** 内容（已预配置）:
```properties
# 开发环境配置
db.host=129.211.180.183
db.port=3306
db.name=voicebox_dev
db.username=voicebox_dev
db.password=dev123
db.url=jdbc:mysql://${db.host}:${db.port}/${db.name}?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai

server.port=8080
log.level=DEBUG
log.file=logs/dev.log
debug.enabled=true
spring.jpa.show-sql=true
```

### 步骤 3: 配置生产环境

**在服务器上创建配置文件**:
```bash
ssh root@129.211.180.183
cd /opt/voicebox
cp env-example.properties config-prod.properties
```

**config-prod.properties** 内容:
```properties
# 生产环境配置
db.host=localhost
db.port=3306
db.name=voicebox_db
db.username=voicebox
db.password=voicebox123
db.url=jdbc:mysql://${db.host}:${db.port}/${db.name}?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai

server.port=8080
log.level=INFO
log.file=logs/prod.log
debug.enabled=false
spring.jpa.show-sql=false
```

---

## 日常使用

### 开发流程

```bash
# 1. 启动开发环境
./start-all.sh dev

# 2. 开发和测试
# 前端: http://localhost:5173
# 后端: http://localhost:10088

# 3. 查看日志
tail -f backend-dev.log
tail -f frontend-dev.log

# 4. 停止服务
./stop-all.sh
```

### 部署到生产

```bash
# 1. 确保在 main 分支
git checkout main

# 2. 合并开发分支
git merge develop

# 3. 提交并推送
git push origin main

# 4. 部署到生产
chmod +x deploy/deploy-prod.sh
./deploy/deploy-prod.sh
```

---

## 数据库操作

### 连接开发数据库

```bash
# 从本地连接
mysql -h 129.211.180.183 -u voicebox_dev -pdev123 voicebox_dev

# 查看表
SHOW TABLES;

# 查询数据
SELECT * FROM users LIMIT 10;
```

### 连接生产数据库

```bash
# 在服务器上连接
ssh root@129.211.180.183
mysql -u voicebox -pvoicebox123 voicebox_db

# 查看表
SHOW TABLES;
```

### 数据库迁移

**创建迁移脚本**:
```bash
# 文件命名: deploy/db/migrations/YYYYMMDD_HHMMSS_description.sql
vim deploy/db/migrations/20241129_120000_add_new_table.sql
```

**应用到开发环境**:
```bash
mysql -h 129.211.180.183 -u voicebox_dev -pdev123 voicebox_dev < deploy/db/migrations/20241129_120000_add_new_table.sql
```

**应用到生产环境**:
```bash
# 上传迁移脚本
scp deploy/db/migrations/20241129_120000_add_new_table.sql root@129.211.180.183:/tmp/

# 在服务器上应用
ssh root@129.211.180.183 "mysql -u voicebox -pvoicebox123 voicebox_db < /tmp/20241129_120000_add_new_table.sql"
```

---

## 环境切换

### 本地开发 → 生产部署

```bash
# 1. 在本地开发和测试
./start-all.sh dev

# 2. 提交代码
git add .
git commit -m "feat: 新功能"
git push

# 3. 合并到 main 分支
git checkout main
git merge develop
git push origin main

# 4. 部署到生产
./deploy/deploy-prod.sh
```

### 生产紧急修复 → 本地同步

```bash
# 1. 在服务器上修复问题
ssh root@129.211.180.183
cd /opt/voicebox
# 修改代码
./restart-all.sh

# 2. 下载变更到本地
scp root@129.211.180.183:/opt/voicebox/[修改的文件] ./[修改的文件]

# 3. 提交到版本控制
git add [修改的文件]
git commit -m "fix: 紧急修复"
git push
```

---

## 故障排查

### 开发环境无法连接数据库

**检查步骤**:
```bash
# 1. 测试网络连接
ping 129.211.180.183

# 2. 测试数据库端口
telnet 129.211.180.183 3306

# 3. 测试数据库连接
mysql -h 129.211.180.183 -u voicebox_dev -pdev123 voicebox_dev -e "SELECT 1;"

# 4. 检查防火墙
ssh root@129.211.180.183 "firewall-cmd --list-ports"
```

**解决方案**:
```bash
# 开放 MySQL 端口
ssh root@129.211.180.183 "firewall-cmd --permanent --add-port=3306/tcp && firewall-cmd --reload"
```

### 配置文件不存在

**错误信息**:
```
❌ 错误: 配置文件 config-dev.properties 不存在
```

**解决方案**:
```bash
# 从模板创建配置文件
cp env-example.properties config-dev.properties

# 或使用预配置的开发配置
# config-dev.properties 已经在项目中，确保没有被删除
```

### 服务启动失败

**检查日志**:
```bash
# 开发环境
tail -f backend-dev.log
tail -f frontend-dev.log

# 生产环境
ssh root@129.211.180.183 "tail -f /opt/voicebox/backend-prod.log"
```

---

## 最佳实践

### 1. 开发流程

✅ **推荐**:
- 始终在 `develop` 分支开发
- 使用开发环境测试（`./start-all.sh dev`）
- 测试通过后合并到 `main` 分支
- 使用自动化脚本部署到生产

❌ **避免**:
- 直接在 `main` 分支开发
- 在生产环境直接修改代码
- 跳过测试直接部署

### 2. 数据库管理

✅ **推荐**:
- 使用迁移脚本管理数据库变更
- 先在开发环境测试迁移
- 备份后再应用到生产环境

❌ **避免**:
- 直接在生产数据库执行 DDL
- 不备份就修改数据库结构
- 在开发环境使用生产数据

### 3. 配置管理

✅ **推荐**:
- 使用环境特定的配置文件
- 敏感信息不提交到 Git
- 使用配置模板

❌ **避免**:
- 硬编码配置
- 提交包含密码的配置文件
- 在多个环境使用相同配置

---

## 快速参考

### 常用命令

```bash
# 启动开发环境
./start-all.sh dev

# 启动生产环境（在服务器上）
./start-all.sh prod

# 停止服务
./stop-all.sh

# 查看服务状态
./status.sh

# 部署到生产
./deploy/deploy-prod.sh

# 设置开发数据库
./scripts/setup-dev-database.sh

# 连接开发数据库
mysql -h 129.211.180.183 -u voicebox_dev -pdev123 voicebox_dev

# 查看日志
tail -f backend-dev.log    # 开发环境后端
tail -f frontend-dev.log   # 开发环境前端
ssh root@129.211.180.183 "tail -f /opt/voicebox/backend-prod.log"  # 生产环境
```

### 配置文件位置

```
项目根目录/
├── config-dev.properties      # 开发环境配置
├── config-prod.properties     # 生产环境配置（在服务器上）
└── env-example.properties     # 配置模板
```

### 数据库信息

| 环境 | 主机 | 数据库 | 用户 | 密码 |
|------|------|--------|------|------|
| 开发 | 129.211.180.183 | voicebox_dev | voicebox_dev | dev123 |
| 生产 | localhost | voicebox_db | voicebox | voicebox123 |

---

## 相关文档

- [环境隔离规范](.kiro/steering/environment-isolation.md)
- [数据库配置规范](.kiro/steering/database-configuration.md)
- [环境同步规范](.kiro/steering/environment-sync.md)
- [快速开始指南](../QUICK_START.md)
