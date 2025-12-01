---
inclusion: always
---

# 数据库配置规范

## 核心原则

**所有数据库操作优先使用云服务器的 MySQL 数据库**，确保：
1. 开发和生产环境数据一致
2. 无需在本地安装和维护 MySQL
3. 团队成员共享同一数据源
4. 简化开发环境配置
5. **强制规则：任何数据库相关操作（查询、修改、测试）都必须连接到云服务器数据库**

---

## 数据库架构

### 服务器 MySQL 信息

| 配置项 | 值 | 说明 |
|--------|-----|------|
| **服务器地址** | `129.211.180.183` | 云服务器 IP |
| **端口** | `3306` | MySQL 默认端口 |
| **数据库名** | `voicebox_db` | 应用数据库 |
| **字符集** | `utf8mb4` | 支持 emoji 和特殊字符 |
| **排序规则** | `utf8mb4_unicode_ci` | Unicode 排序 |

### 用户账号

| 用户 | 密码 | 权限 | 用途 |
|------|------|------|------|
| `root` | `root123` | 全部权限 | 管理和维护 |
| `voicebox` | `voicebox123` | `voicebox_db.*` | 应用连接 |

---

## 配置方式

### 1. 本地开发环境配置（强制使用云服务器数据库）

#### ⚠️ 强制规则

**所有本地开发必须连接到云服务器数据库 `129.211.180.183`**
- 禁止使用 `localhost` 或 `127.0.0.1`
- 禁止连接本地 MySQL 实例
- 所有配置文件必须指向云服务器

#### Spring Boot 配置

**文件位置**: `app-device/src/main/resources/application.properties`

```properties
# 数据库连接配置（强制连接到云服务器）
spring.datasource.url=jdbc:mysql://129.211.180.183:3306/voicebox_db?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.username=voicebox
spring.datasource.password=voicebox123
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# 连接池配置
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

# JPA 配置
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL5InnoDBDialect
spring.jpa.properties.hibernate.format_sql=true
```

#### 或使用 config.properties

**文件位置**: `config.properties`（根目录）

```properties
# 数据库配置
db.host=129.211.180.183
db.port=3306
db.name=voicebox_db
db.username=voicebox
db.password=voicebox123
db.url=jdbc:mysql://${db.host}:${db.port}/${db.name}?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai
```

### 2. 服务器生产环境配置

**文件位置**: `/opt/voicebox/config.properties`

```properties
# 数据库配置（服务器本地连接）
db.host=localhost
db.port=3306
db.name=voicebox_db
db.username=voicebox
db.password=voicebox123
db.url=jdbc:mysql://${db.host}:${db.port}/${db.name}?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai
```

**注意**：
- 仅在服务器上运行时使用 `localhost`
- 本地开发环境必须使用 `129.211.180.183`

---

## 网络配置

### 1. 服务器防火墙配置

**必须开放 MySQL 端口**，允许远程连接：

```bash
# 在服务器上执行
ssh root@129.211.180.183

# 开放 MySQL 端口
firewall-cmd --permanent --add-port=3306/tcp
firewall-cmd --reload

# 验证
firewall-cmd --list-ports
```

### 2. MySQL 远程访问配置

**允许远程用户连接**：

```bash
# 在服务器上执行
mysql -u root -proot123

# 创建或更新远程访问权限
CREATE USER IF NOT EXISTS 'voicebox'@'%' IDENTIFIED BY 'voicebox123';
GRANT ALL PRIVILEGES ON voicebox_db.* TO 'voicebox'@'%';
FLUSH PRIVILEGES;

# 验证用户
SELECT user, host FROM mysql.user WHERE user='voicebox';
```

**修改 MySQL 配置文件**（如果需要）：

```bash
# 编辑 MySQL 配置
vim /etc/my.cnf

# 确保 bind-address 允许远程连接
[mysqld]
bind-address = 0.0.0.0
# 或注释掉 bind-address 行

# 重启 MySQL
systemctl restart mysqld
```

### 3. 本地连接测试

**测试远程连接**：

```bash
# 在本地执行
mysql -h 129.211.180.183 -u voicebox -pvoicebox123 voicebox_db

# 如果连接成功，会看到 MySQL 提示符
mysql>

# 测试查询
SHOW TABLES;
SELECT DATABASE();

# 退出
exit
```

**使用 telnet 测试端口**：

```bash
# 测试 MySQL 端口是否开放
telnet 129.211.180.183 3306

# 或使用 nc
nc -zv 129.211.180.183 3306
```

---

## 数据库操作规范

### 1. 查询数据（强制使用云服务器）

**⚠️ 所有数据库查询必须连接到云服务器 `129.211.180.183`**

```bash
# 方式 A: 使用 MySQL 客户端（推荐）
mysql -h 129.211.180.183 -u voicebox -pvoicebox123 voicebox_db

# 方式 B: 使用 SSH 隧道（更安全）
ssh -L 3307:localhost:3306 root@129.211.180.183
# 然后在另一个终端
mysql -h 127.0.0.1 -P 3307 -u voicebox -pvoicebox123 voicebox_db

# 方式 C: 使用 MySQL Workbench 等 GUI 工具
# 连接信息：
# Host: 129.211.180.183
# Port: 3306
# Username: voicebox
# Password: voicebox123

# ❌ 禁止使用本地数据库
# mysql -h localhost -u root -p  # 错误！
```

### 2. 修改数据库结构

**重要**：数据库结构变更必须通过迁移脚本：

```bash
# 1. 创建迁移脚本
# 文件: deploy/migrations/YYYYMMDD_HHMMSS_description.sql
cat > deploy/migrations/20241128_160000_add_user_preferences.sql << 'EOF'
-- 添加用户偏好表
CREATE TABLE IF NOT EXISTS user_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    preference_key VARCHAR(100) NOT NULL,
    preference_value TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    UNIQUE KEY uk_user_preference (user_id, preference_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
EOF

# 2. 在服务器上应用迁移
scp deploy/migrations/20241128_160000_add_user_preferences.sql root@129.211.180.183:/tmp/
ssh root@129.211.180.183 "mysql -u voicebox -pvoicebox123 voicebox_db < /tmp/20241128_160000_add_user_preferences.sql"

# 3. 验证变更
mysql -h 129.211.180.183 -u voicebox -pvoicebox123 voicebox_db -e "SHOW TABLES;"

# 4. 提交迁移脚本到版本控制
git add deploy/migrations/20241128_160000_add_user_preferences.sql
git commit -m "数据库迁移: 添加用户偏好表"
```

### 3. 备份数据库

**定期备份**（在服务器上执行）：

```bash
# 备份整个数据库
ssh root@129.211.180.183 "mysqldump -u voicebox -pvoicebox123 voicebox_db > /tmp/voicebox_db_backup_$(date +%Y%m%d_%H%M%S).sql"

# 下载备份到本地
scp root@129.211.180.183:/tmp/voicebox_db_backup_*.sql ./backups/

# 仅备份结构（不含数据）
ssh root@129.211.180.183 "mysqldump -u voicebox -pvoicebox123 --no-data voicebox_db > /tmp/schema.sql"
```

### 4. 恢复数据库

**从备份恢复**：

```bash
# 上传备份文件到服务器
scp backup.sql root@129.211.180.183:/tmp/

# 在服务器上恢复
ssh root@129.211.180.183 "mysql -u voicebox -pvoicebox123 voicebox_db < /tmp/backup.sql"
```

---

## 安全注意事项

### 1. 密码管理

**不要在代码中硬编码密码**：

```properties
# ❌ 错误：硬编码密码
spring.datasource.password=voicebox123

# ✅ 正确：使用环境变量
spring.datasource.password=${DB_PASSWORD:voicebox123}
```

**使用环境变量**：

```bash
# 在 ~/.bashrc 或 ~/.zshrc 中设置
export DB_PASSWORD=voicebox123

# 或在启动时指定
DB_PASSWORD=voicebox123 mvn spring-boot:run
```

### 2. 网络安全

**使用 SSH 隧道（推荐）**：

```bash
# 创建 SSH 隧道
ssh -L 3307:localhost:3306 root@129.211.180.183 -N -f

# 应用连接到本地端口
spring.datasource.url=jdbc:mysql://localhost:3307/voicebox_db?...

# 关闭隧道
ps aux | grep "ssh -L 3307"
kill [PID]
```

**优点**：
- 数据传输加密
- 不需要开放 MySQL 端口到公网
- 更安全

### 3. 访问控制

**限制远程访问 IP**（可选）：

```sql
-- 只允许特定 IP 访问
CREATE USER 'voicebox'@'你的IP地址' IDENTIFIED BY 'voicebox123';
GRANT ALL PRIVILEGES ON voicebox_db.* TO 'voicebox'@'你的IP地址';

-- 查看当前用户权限
SHOW GRANTS FOR 'voicebox'@'%';
```

---

## 常见问题排查

### 问题 1: 无法连接到远程 MySQL

**症状**：
```
ERROR 2003 (HY000): Can't connect to MySQL server on '129.211.180.183' (110)
```

**排查步骤**：

```bash
# 1. 测试网络连接
ping 129.211.180.183

# 2. 测试 MySQL 端口
telnet 129.211.180.183 3306
nc -zv 129.211.180.183 3306

# 3. 检查服务器防火墙
ssh root@129.211.180.183 "firewall-cmd --list-ports"

# 4. 检查 MySQL 是否运行
ssh root@129.211.180.183 "systemctl status mysqld"

# 5. 检查 MySQL 监听地址
ssh root@129.211.180.183 "netstat -tlnp | grep 3306"
```

**解决方案**：

```bash
# 开放防火墙端口
ssh root@129.211.180.183 "firewall-cmd --permanent --add-port=3306/tcp && firewall-cmd --reload"

# 修改 MySQL 配置允许远程连接
ssh root@129.211.180.183
vim /etc/my.cnf
# 设置 bind-address = 0.0.0.0
systemctl restart mysqld
```

### 问题 2: 权限被拒绝

**症状**：
```
ERROR 1045 (28000): Access denied for user 'voicebox'@'你的IP' (using password: YES)
```

**排查步骤**：

```bash
# 检查用户权限
ssh root@129.211.180.183
mysql -u root -proot123

SELECT user, host FROM mysql.user WHERE user='voicebox';
SHOW GRANTS FOR 'voicebox'@'%';
```

**解决方案**：

```sql
-- 创建或更新用户权限
CREATE USER IF NOT EXISTS 'voicebox'@'%' IDENTIFIED BY 'voicebox123';
GRANT ALL PRIVILEGES ON voicebox_db.* TO 'voicebox'@'%';
FLUSH PRIVILEGES;
```

### 问题 3: 连接超时

**症状**：
```
Communications link failure
The last packet sent successfully to the server was 0 milliseconds ago.
```

**排查步骤**：

```bash
# 1. 检查网络稳定性
ping -c 10 129.211.180.183

# 2. 增加连接超时时间
spring.datasource.hikari.connection-timeout=60000

# 3. 使用 SSH 隧道
ssh -L 3307:localhost:3306 root@129.211.180.183 -N -f
```

### 问题 4: 字符集问题

**症状**：中文或 emoji 显示为乱码

**解决方案**：

```sql
-- 检查数据库字符集
SHOW CREATE DATABASE voicebox_db;

-- 修改数据库字符集
ALTER DATABASE voicebox_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 修改表字符集
ALTER TABLE table_name CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

## 开发工作流

### 1. 启动本地开发

```bash
# 1. 测试数据库连接
mysql -h 129.211.180.183 -u voicebox -pvoicebox123 voicebox_db -e "SELECT 1;"

# 2. 启动后端服务
cd app-device
mvn spring-boot:run

# 3. 查看连接日志
# 应该看到类似：
# HikariPool-1 - Starting...
# HikariPool-1 - Start completed.
```

### 2. 数据库变更流程

```bash
# 1. 创建迁移脚本
vim deploy/migrations/YYYYMMDD_HHMMSS_description.sql

# 2. 在服务器上应用
scp deploy/migrations/[脚本].sql root@129.211.180.183:/tmp/
ssh root@129.211.180.183 "mysql -u voicebox -pvoicebox123 voicebox_db < /tmp/[脚本].sql"

# 3. 验证变更
mysql -h 129.211.180.183 -u voicebox -pvoicebox123 voicebox_db -e "SHOW TABLES;"

# 4. 更新 init-database.sql
ssh root@129.211.180.183 "mysqldump -u voicebox -pvoicebox123 --no-data voicebox_db > /tmp/schema.sql"
scp root@129.211.180.183:/tmp/schema.sql deploy/init-database.sql

# 5. 提交到版本控制
git add deploy/migrations/[脚本].sql deploy/init-database.sql
git commit -m "数据库变更: [描述]"
```

### 3. 数据查看和调试

```bash
# 使用 MySQL Workbench 或命令行
mysql -h 129.211.180.183 -u voicebox -pvoicebox123 voicebox_db

# 常用查询
SHOW TABLES;
DESCRIBE table_name;
SELECT * FROM table_name LIMIT 10;
```

---

## 配置文件模板

### application.properties 模板

```properties
# ============================================
# 数据库配置 - 连接到服务器 MySQL
# ============================================
spring.datasource.url=jdbc:mysql://129.211.180.183:3306/voicebox_db?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.username=voicebox
spring.datasource.password=${DB_PASSWORD:voicebox123}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# 连接池配置
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

# JPA 配置
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL5InnoDBDialect
```

---

## 最佳实践

1. **强制使用云服务器数据库** - 所有开发、测试都连接到 `129.211.180.183`
2. **使用连接池** - 避免频繁创建连接
3. **设置合理的超时** - 防止连接挂起
4. **定期备份** - 每天自动备份数据库
5. **使用迁移脚本** - 所有结构变更通过脚本管理
6. **监控连接数** - 避免连接泄漏
7. **使用 SSH 隧道** - 提高安全性（可选）
8. **谨慎操作生产数据** - 虽然使用同一数据库，但要注意数据安全

---

## 快速参考

```bash
# ✅ 正确：连接云服务器数据库
mysql -h 129.211.180.183 -u voicebox -pvoicebox123 voicebox_db

# ✅ 测试连接
mysql -h 129.211.180.183 -u voicebox -pvoicebox123 -e "SELECT 1;"

# ✅ 查看表
mysql -h 129.211.180.183 -u voicebox -pvoicebox123 voicebox_db -e "SHOW TABLES;"

# ✅ 备份数据库
ssh root@129.211.180.183 "mysqldump -u voicebox -pvoicebox123 voicebox_db > /tmp/backup.sql"

# ✅ 应用迁移
ssh root@129.211.180.183 "mysql -u voicebox -pvoicebox123 voicebox_db < /tmp/migration.sql"

# ❌ 错误：使用本地数据库
# mysql -h localhost -u root -p  # 禁止！
# mysql -u root -p voicebox_db   # 禁止！
```

---

## ⚠️ 重要提醒

**所有数据库操作必须使用云服务器数据库 `129.211.180.183`**

如果你发现配置中使用了 `localhost` 或 `127.0.0.1`（除非在服务器上运行），立即修改为 `129.211.180.183`。
