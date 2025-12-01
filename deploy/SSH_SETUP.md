# SSH 免密登录配置指南

## 概述

本文档说明如何配置 SSH 密钥认证，实现免密登录服务器，避免每次操作都需要输入密码。

---

## 配置状态

✅ **已完成配置**

- SSH 密钥已生成
- 公钥已复制到服务器
- SSH 配置文件已创建
- 可以使用别名 `voicebox-server` 或 `vb` 连接

---

## 快速测试

```bash
# 测试免密登录
ssh voicebox-server "echo '连接成功！'"

# 或使用短别名
ssh vb "echo '连接成功！'"
```

如果看到 "连接成功！" 且没有要求输入密码，说明配置正确。

---

## SSH 配置详情

### 配置文件位置

`~/.ssh/config`

### 配置内容

```
# VoiceBox 服务器配置
Host voicebox-server
    HostName 129.211.180.183
    User root
    Port 22
    IdentityFile ~/.ssh/id_rsa
    ServerAliveInterval 60
    ServerAliveCountMax 3
    ConnectTimeout 10
    
# 简短别名
Host vb
    HostName 129.211.180.183
    User root
    Port 22
    IdentityFile ~/.ssh/id_rsa
    ServerAliveInterval 60
    ServerAliveCountMax 3
    ConnectTimeout 10
```

### 配置说明

- `HostName`: 服务器 IP 地址
- `User`: 登录用户名
- `IdentityFile`: SSH 私钥路径
- `ServerAliveInterval`: 每 60 秒发送心跳，保持连接
- `ServerAliveCountMax`: 最多 3 次心跳失败后断开
- `ConnectTimeout`: 连接超时时间 10 秒

---

## 使用方法

### 1. 直接连接服务器

```bash
# 使用完整别名
ssh voicebox-server

# 使用短别名
ssh vb

# 执行单个命令
ssh voicebox-server "ls -la /opt/voicebox"
```

### 2. 文件传输

```bash
# 上传文件
scp local-file.txt voicebox-server:/opt/voicebox/

# 下载文件
scp voicebox-server:/opt/voicebox/config.properties ./

# 上传目录
scp -r local-dir voicebox-server:/opt/voicebox/

# 下载目录
scp -r voicebox-server:/opt/voicebox/logs ./
```

### 3. 使用快捷脚本

我们提供了一些快捷脚本，简化常用操作：

```bash
# 连接到服务器
./scripts/server/ssh-connect.sh

# 查看实时日志
./scripts/server/view-logs.sh

# 重启服务
./scripts/server/restart-service.sh

# 同步文件到服务器
./scripts/server/sync-file.sh config.properties

# 从服务器下载文件
./scripts/server/download-file.sh config.properties
```

### 4. 部署到服务器

```bash
# 完整部署流程
./deploy/deploy-from-local.sh
```

---

## 故障排查

### 问题 1: 仍然要求输入密码

**可能原因**：
- 公钥未正确复制到服务器
- 服务器上的权限不正确

**解决方案**：

```bash
# 重新复制公钥
ssh-copy-id root@129.211.180.183

# 或手动检查服务器权限
ssh root@129.211.180.183
chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys
```

### 问题 2: 连接超时

**可能原因**：
- 网络问题
- 服务器防火墙阻止

**解决方案**：

```bash
# 测试网络连接
ping 129.211.180.183

# 测试 SSH 端口
nc -zv 129.211.180.183 22

# 使用详细模式查看连接过程
ssh -v voicebox-server
```

### 问题 3: 权限被拒绝

**可能原因**：
- 私钥权限不正确
- 私钥文件损坏

**解决方案**：

```bash
# 检查私钥权限
ls -la ~/.ssh/id_rsa
# 应该是 -rw------- (600)

# 修复权限
chmod 600 ~/.ssh/id_rsa
chmod 644 ~/.ssh/id_rsa.pub

# 测试密钥
ssh-keygen -y -f ~/.ssh/id_rsa
```

---

## 安全建议

### 1. 保护私钥

```bash
# 私钥应该只有所有者可读
chmod 600 ~/.ssh/id_rsa

# 不要分享私钥
# 不要将私钥提交到 Git
```

### 2. 使用密码保护私钥（可选）

如果需要更高的安全性，可以为私钥设置密码：

```bash
# 为现有私钥添加密码
ssh-keygen -p -f ~/.ssh/id_rsa

# 使用 ssh-agent 避免重复输入密码
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_rsa
```

### 3. 定期更新密钥

建议每年更新一次 SSH 密钥：

```bash
# 生成新密钥
ssh-keygen -t rsa -b 4096 -C "your_email@example.com"

# 复制到服务器
ssh-copy-id -i ~/.ssh/id_rsa.pub root@129.211.180.183
```

---

## 高级用法

### 1. SSH 隧道（端口转发）

```bash
# 将服务器的 MySQL 端口转发到本地
ssh -L 3307:localhost:3306 voicebox-server -N -f

# 现在可以通过 localhost:3307 访问服务器的 MySQL
mysql -h 127.0.0.1 -P 3307 -u voicebox -pvoicebox123 voicebox_db

# 关闭隧道
ps aux | grep "ssh -L 3307"
kill [PID]
```

### 2. 保持连接

如果连接经常断开，可以在 SSH 配置中添加：

```
Host voicebox-server
    ...
    ServerAliveInterval 30
    ServerAliveCountMax 5
    TCPKeepAlive yes
```

### 3. 多个服务器

如果有多个服务器，可以在 `~/.ssh/config` 中添加多个配置：

```
Host voicebox-prod
    HostName 129.211.180.183
    User root
    IdentityFile ~/.ssh/id_rsa

Host voicebox-dev
    HostName 192.168.1.100
    User root
    IdentityFile ~/.ssh/id_rsa
```

---

## 验证配置

运行以下命令验证配置是否正确：

```bash
# 1. 测试免密登录
ssh -o BatchMode=yes voicebox-server "echo 'OK'"
# 应该输出 "OK" 且不要求密码

# 2. 测试文件传输
echo "test" > /tmp/test.txt
scp /tmp/test.txt voicebox-server:/tmp/
# 应该成功且不要求密码

# 3. 测试部署脚本
./deploy/deploy-from-local.sh
# 应该能够完整运行且不要求密码
```

---

## 常用命令速查

```bash
# 连接服务器
ssh voicebox-server

# 执行命令
ssh voicebox-server "command"

# 上传文件
scp file voicebox-server:/path/

# 下载文件
scp voicebox-server:/path/file ./

# 查看日志
ssh voicebox-server "tail -f /var/log/voicebox/backend.log"

# 重启服务
ssh voicebox-server "systemctl restart voicebox-backend"

# 查看服务状态
ssh voicebox-server "systemctl status voicebox-backend"
```

---

## 相关文档

- [服务器连接故障排查规范](.kiro/steering/server-connection-troubleshooting.md)
- [环境同步规范](.kiro/steering/environment-sync.md)
- [部署文档](deploy/README.md)

---

## 总结

✅ SSH 密钥认证已配置完成，现在可以：

1. 无需密码连接服务器
2. 无需密码传输文件
3. 使用自动化部署脚本
4. 使用快捷脚本简化操作

如有问题，请参考故障排查部分或查看相关文档。
