# VoiceBox 服务器部署指南

## 服务器信息
- **地址**: 129.211.180.183
- **系统**: CentOS Linux 7
- **用户**: root

## 环境要求

### 必需软件
- **Java**: OpenJDK 11 或更高版本
- **Maven**: 3.9.9（需要升级，参见 [Maven 升级指南](../docs/MAVEN_UPGRADE_GUIDE.md)）
- **Node.js**: 16.x 或更高版本
- **MySQL**: 5.7 或更高版本
- **Redis**: 3.x 或更高版本
- **Nginx**: 1.20 或更高版本

### Maven 升级
服务器当前 Maven 版本为 3.0.5，需要升级到 3.9.9：

```bash
# 上传升级脚本
scp scripts/server/upgrade-maven.sh root@129.211.180.183:/tmp/

# 执行升级
ssh root@129.211.180.183
cd /tmp
chmod +x upgrade-maven.sh
./upgrade-maven.sh

# 验证版本
mvn -version
```

详细步骤参见 [Maven 升级指南](../docs/MAVEN_UPGRADE_GUIDE.md)

## 快速部署

### 方式一：自动部署（推荐）

1. 上传部署脚本到服务器：
```bash
scp -r deploy root@129.211.180.183:/root/
```

2. 连接服务器并执行：
```bash
ssh root@129.211.180.183
cd /root/deploy
chmod +x *.sh
./deploy-all.sh
```

### 方式二：分步部署

如果需要更细粒度的控制，可以分步执行：

```bash
# 1. 安装依赖环境（Java, Maven, Node.js, Docker, Nginx）
./01-install-dependencies.sh

# 2. 安装和配置 MySQL 数据库
./02-setup-database.sh

# 3. 部署应用（需要先上传代码）
./03-deploy-application.sh

# 4. 配置系统服务
./04-setup-services.sh
```

## 上传代码

在本地项目目录执行：
```bash
# 压缩代码
tar -czf voicebox.tar.gz --exclude=node_modules --exclude=target --exclude=.git .

# 上传到服务器
scp voicebox.tar.gz root@129.211.180.183:/opt/

# 解压
ssh root@129.211.180.183 "cd /opt && tar -xzf voicebox.tar.gz -C voicebox"
```

## 服务管理

### 后端服务
```bash
# 启动
systemctl start voicebox-backend

# 停止
systemctl stop voicebox-backend

# 重启
systemctl restart voicebox-backend

# 查看状态
systemctl status voicebox-backend

# 查看日志
journalctl -u voicebox-backend -f
```

### Nginx
```bash
# 启动
systemctl start nginx

# 停止
systemctl stop nginx

# 重启
systemctl restart nginx

# 测试配置
nginx -t
```

### MySQL
```bash
# 启动
systemctl start mysqld

# 停止
systemctl stop mysqld

# 连接数据库
mysql -uvoicebox -pVoiceBox@2024 voicebox
```

## 访问应用

- **前端**: http://129.211.180.183
- **后端 API**: http://129.211.180.183/api

## 数据库配置

- **主机**: localhost
- **端口**: 3306
- **数据库**: voicebox
- **用户名**: voicebox
- **密码**: VoiceBox@2024

## 防火墙端口

已开放的端口：
- 80 (HTTP)
- 443 (HTTPS)
- 8080 (后端服务)
- 3000 (开发服务器)

## 故障排查

### 查看后端日志
```bash
journalctl -u voicebox-backend -n 100
```

### 查看 Nginx 日志
```bash
tail -f /var/log/nginx/error.log
tail -f /var/log/nginx/access.log
```

### 检查端口占用
```bash
netstat -tlnp | grep -E '80|8080|3306'
```

### 检查服务状态
```bash
systemctl status voicebox-backend
systemctl status nginx
systemctl status mysqld
```

## 更新应用

```bash
# 1. 上传新代码
scp -r /path/to/voice-box root@129.211.180.183:/opt/voicebox-new

# 2. 停止服务
systemctl stop voicebox-backend

# 3. 备份旧版本
mv /opt/voicebox /opt/voicebox-backup-$(date +%Y%m%d)

# 4. 替换代码
mv /opt/voicebox-new /opt/voicebox

# 5. 重新构建
cd /opt/voicebox

# 确保使用正确的 Maven 版本
source /etc/profile.d/maven.sh
mvn -version  # 应该显示 3.9.9

# 构建后端
mvn clean package -DskipTests

# 构建前端
cd app-web && npm install && npm run build

# 6. 启动服务
systemctl start voicebox-backend
systemctl restart nginx
```

## 安全建议

1. 修改 MySQL root 密码
2. 配置 SSL 证书（使用 Let's Encrypt）
3. 设置定期备份
4. 配置日志轮转
5. 限制 SSH 访问（使用密钥认证）
