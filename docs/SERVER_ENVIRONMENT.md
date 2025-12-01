# VoiceBox 服务器环境文档

## 服务器信息

- **IP地址**: 129.211.180.183
- **操作系统**: CentOS Linux 7
- **部署日期**: 2025-11-28

## 已安装服务

### 1. MySQL 5.7
- **版本**: 5.7.44
- **端口**: 3306
- **数据库**: voicebox_db
- **用户**: voicebox / voicebox123
- **Root密码**: root123
- **状态**: ✅ 运行中

### 2. Redis
- **版本**: 3.2.12
- **端口**: 6379 (默认)
- **状态**: ✅ 运行中
- **用途**: 
  - 用户信息缓存
  - 会话缓存
  - 热点数据缓存
  - API限流（待实现）

### 3. Java
- **版本**: OpenJDK 11.0.23
- **路径**: /usr/lib/jvm/java-11-openjdk

### 4. Maven
- **版本**: 3.9.9
- **安装路径**: /opt/maven
- **环境变量**: /etc/profile.d/maven.sh
- **状态**: ✅ 已升级
- **升级日期**: 2024-11-29
- **升级指南**: 参见 [Maven 升级指南](./MAVEN_UPGRADE_GUIDE.md)

### 5. Node.js
- **版本**: 16.20.2
- **npm版本**: 8.19.4
- **路径**: /opt/node-v16.20.2-linux-x64

### 6. Nginx
- **版本**: 1.20.1
- **端口**: 80
- **配置**: /etc/nginx/conf.d/voicebox.conf
- **状态**: ✅ 运行中

## 应用服务

### VoiceBox Backend
- **服务名**: voicebox-backend.service
- **端口**: 10088
- **工作目录**: /opt/voicebox
- **JAR文件**: /opt/voicebox/app-device/target/app-device-0.0.1-SNAPSHOT.jar
- **配置文件**: /opt/voicebox/config.properties
- **日志**: journalctl -u voicebox-backend -f
- **状态**: ✅ 运行中

## 目录结构

```
/opt/voicebox/          # 应用代码
├── app-device/         # 后端应用
├── app-web/           # 前端应用
│   └── dist/          # 前端构建产物
├── config.properties  # 应用配置
└── deploy/            # 部署脚本

/var/log/voicebox/     # 应用日志
├── backend.log        # 后端日志
└── backend-error.log  # 错误日志

/var/lib/voicebox/     # 应用数据
└── uploads/           # 上传文件
```

## 管理命令

### 服务管理
```bash
# 后端服务
systemctl start voicebox-backend
systemctl stop voicebox-backend
systemctl restart voicebox-backend
systemctl status voicebox-backend

# MySQL
systemctl start mysqld
systemctl stop mysqld
systemctl restart mysqld

# Redis
systemctl start redis
systemctl stop redis
systemctl restart redis

# Nginx
systemctl start nginx
systemctl stop nginx
systemctl restart nginx
```

### 日志查看
```bash
# 后端日志
journalctl -u voicebox-backend -f

# MySQL日志
tail -f /var/log/mysqld.log

# Redis日志
tail -f /var/log/redis/redis.log

# Nginx日志
tail -f /var/log/nginx/error.log
tail -f /var/log/nginx/access.log
```

### 数据库操作
```bash
# 连接数据库
mysql -uvoicebox -pvoicebox123 voicebox_db

# 查看表
mysql -uvoicebox -pvoicebox123 -e "SHOW TABLES" voicebox_db

# 备份数据库
mysqldump -uvoicebox -pvoicebox123 voicebox_db > backup.sql
```

### Redis操作
```bash
# 连接Redis
redis-cli

# 查看所有键
redis-cli KEYS '*'

# 查看Redis信息
redis-cli INFO

# 清空所有数据（慎用）
redis-cli FLUSHALL
```

## 网络配置

### 开放端口
- 80 (HTTP)
- 443 (HTTPS, 未配置)
- 3306 (MySQL, 仅本地)
- 6379 (Redis, 仅本地)
- 10088 (后端API, 仅本地)

### 访问地址
- **前端**: http://129.211.180.183
- **后端API**: http://129.211.180.183/api (通过Nginx代理)

## 安全配置

### 数据库
- MySQL仅监听localhost
- 使用独立的应用用户（voicebox）
- 密码策略：LOW（开发环境）

### Redis
- 仅监听localhost
- 未设置密码（开发环境）
- 建议生产环境设置密码

### 应用
- 以root用户运行（待优化）
- 建议创建专用用户运行应用

## 待实现功能

### Redis集成
虽然Redis已安装，但应用代码中尚未实现以下功能：

1. **用户信息缓存** (v1.0)
   - 用户基本信息缓存
   - 设备信息缓存
   - 会话信息缓存

2. **个性化分析缓存** (v2.0)
   - 用户画像缓存
   - 热点数据缓存
   - 计算结果缓存

3. **API限流** (v3.0)
   - 基于令牌桶的限流
   - 用户配额管理
   - 响应缓存

### 实现建议
需要在代码中添加：
- Spring Data Redis依赖
- RedisTemplate配置
- 缓存注解(@Cacheable, @CacheEvict等)
- 限流拦截器

## 监控和维护

### 磁盘空间
```bash
df -h
```

### 内存使用
```bash
free -h
```

### 服务状态
```bash
systemctl status mysqld redis nginx voicebox-backend
```

### 进程监控
```bash
ps aux | grep -E 'java|nginx|mysql|redis'
```

## 故障排查

### 后端无法启动
1. 检查日志：`journalctl -u voicebox-backend -n 100`
2. 检查MySQL连接：`mysql -uvoicebox -pvoicebox123 voicebox_db`
3. 检查端口占用：`netstat -tlnp | grep 10088`

### 前端无法访问
1. 检查Nginx状态：`systemctl status nginx`
2. 检查Nginx配置：`nginx -t`
3. 检查静态文件：`ls -la /opt/voicebox/app-web/dist/`

### 数据库连接失败
1. 检查MySQL状态：`systemctl status mysqld`
2. 检查用户权限：`mysql -uroot -proot123 -e "SHOW GRANTS FOR 'voicebox'@'localhost'"`
3. 检查防火墙：`firewall-cmd --list-all`

## 更新记录

- **2024-11-29**: Maven 升级完成 ✅
  - 升级 Maven 从 3.0.5 到 3.9.9
  - 安装路径: /opt/maven
  - 配置环境变量: /etc/profile.d/maven.sh
  - 移除旧的 yum 版本
  - 重新编译项目成功
  - 服务运行正常
  - 参见 [Maven 升级指南](./MAVEN_UPGRADE_GUIDE.md)

- **2024-11-28**: 初始部署
  - 安装MySQL 5.7
  - 安装Redis 3.2.12
  - 部署后端服务
  - 部署前端应用
  - 配置Nginx反向代理
