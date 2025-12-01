---
inclusion: always
---

# 服务器连接故障排查规范

## 核心原则

**在执行服务器操作时，如果命令长时间无响应，必须主动排查是否需要密码输入或其他交互**

---

## 常见问题识别

### 1. SSH/SCP 命令无响应

#### 症状
- 命令执行后长时间（超过 10 秒）无输出
- 没有错误信息，也没有成功信息
- 终端光标一直闪烁等待

#### 可能原因
1. **等待密码输入** ⭐ 最常见
2. 网络连接问题
3. 服务器无响应
4. 防火墙阻止
5. SSH 密钥问题

#### 排查步骤

**第一步：检查是否需要密码**
```bash
# 如果命令卡住，很可能是在等待密码输入
# 常见的需要密码的命令：

# SSH 连接
ssh root@129.211.180.183
# 如果没有配置密钥，会提示: root@129.211.180.183's password:

# SCP 文件传输
scp file.txt root@129.211.180.183:/path/
# 如果没有配置密钥，会提示: root@129.211.180.183's password:

# MySQL 操作
mysql -u root -p
# 会提示: Enter password:

# Sudo 命令
sudo systemctl restart nginx
# 会提示: [sudo] password for user:
```

**第二步：使用超时参数**
```bash
# SSH 连接设置超时
ssh -o ConnectTimeout=10 root@129.211.180.183

# SCP 传输设置超时
scp -o ConnectTimeout=10 file.txt root@129.211.180.183:/path/

# 如果超时，说明网络或服务器有问题
# 如果提示密码，说明需要配置密钥认证
```

**第三步：测试网络连接**
```bash
# Ping 服务器
ping -c 4 129.211.180.183

# 测试 SSH 端口
nc -zv 129.211.180.183 22
# 或
telnet 129.211.180.183 22
```

---

## 解决方案

### 方案 1: 配置 SSH 密钥认证（推荐）

**优点**：
- 无需每次输入密码
- 更安全
- 支持自动化脚本

**配置步骤**：

```bash
# 1. 生成 SSH 密钥（如果还没有）
ssh-keygen -t rsa -b 4096 -C "your_email@example.com"
# 按提示操作，可以直接回车使用默认路径

# 2. 复制公钥到服务器
ssh-copy-id root@129.211.180.183
# 需要输入一次密码

# 3. 测试免密登录
ssh root@129.211.180.183
# 应该不再需要密码

# 4. 验证密钥
ls -la ~/.ssh/
# 应该看到 id_rsa（私钥）和 id_rsa.pub（公钥）
```

**手动配置（如果 ssh-copy-id 不可用）**：

```bash
# 1. 查看本地公钥
cat ~/.ssh/id_rsa.pub

# 2. 登录服务器
ssh root@129.211.180.183
# 输入密码

# 3. 在服务器上添加公钥
mkdir -p ~/.ssh
chmod 700 ~/.ssh
echo "你的公钥内容" >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys

# 4. 退出并测试
exit
ssh root@129.211.180.183
# 应该不再需要密码
```

### 方案 2: 使用 sshpass（临时方案）

**注意**：不推荐用于生产环境，仅用于测试

```bash
# 安装 sshpass
# macOS
brew install hudochenkov/sshpass/sshpass

# Linux
sudo apt-get install sshpass  # Ubuntu/Debian
sudo yum install sshpass      # CentOS/RHEL

# 使用 sshpass
sshpass -p '你的密码' ssh root@129.211.180.183
sshpass -p '你的密码' scp file.txt root@129.211.180.183:/path/
```

### 方案 3: 使用 Expect 脚本

**适用于**：需要自动化但无法配置密钥的场景

查看现有的 expect 脚本：
- `deploy/ssh-connect.exp` - SSH 连接脚本
- `deploy/scp-upload.exp` - SCP 上传脚本

**使用示例**：

```bash
# SSH 连接
./deploy/ssh-connect.exp

# SCP 上传
./deploy/scp-upload.exp local_file remote_path
```

---

## 操作规范

### 执行服务器命令时

**1. 设置合理的超时时间**
```bash
# 使用 timeout 命令
timeout 30s ssh root@129.211.180.183 "command"

# 如果超时，说明有问题需要排查
```

**2. 添加详细输出**
```bash
# SSH 使用 verbose 模式
ssh -v root@129.211.180.183

# 可以看到连接过程的详细信息
# 包括是否在等待密码、密钥认证等
```

**3. 检查命令执行状态**
```bash
# 在脚本中检查返回值
ssh root@129.211.180.183 "command"
if [ $? -ne 0 ]; then
    echo "命令执行失败，可能需要密码或网络有问题"
    exit 1
fi
```

### 编写自动化脚本时

**必须包含的检查**：

```bash
#!/bin/bash

SERVER="root@129.211.180.183"

# 1. 检查 SSH 密钥是否配置
echo "检查 SSH 连接..."
if ! ssh -o BatchMode=yes -o ConnectTimeout=5 $SERVER "echo 2>&1" &>/dev/null; then
    echo "❌ SSH 密钥未配置或连接失败"
    echo "请运行: ssh-copy-id $SERVER"
    echo "或检查网络连接"
    exit 1
fi
echo "✓ SSH 连接正常"

# 2. 检查服务器可达性
echo "检查服务器可达性..."
if ! ping -c 2 129.211.180.183 &>/dev/null; then
    echo "❌ 服务器无法访问"
    echo "请检查网络连接"
    exit 1
fi
echo "✓ 服务器可达"

# 3. 执行实际操作
echo "执行操作..."
ssh $SERVER "your command here"
```

---

## 常见场景处理

### 场景 1: 部署脚本卡住

**症状**：
```bash
./deploy/deploy-to-server.sh
# 执行后无响应
```

**排查**：
```bash
# 1. 检查脚本内容
cat deploy/deploy-to-server.sh | grep -E "ssh|scp"

# 2. 手动测试 SSH 连接
ssh root@129.211.180.183 "echo test"

# 3. 如果提示密码，配置密钥
ssh-copy-id root@129.211.180.183
```

### 场景 2: SCP 传输无响应

**症状**：
```bash
scp file.txt root@129.211.180.183:/opt/voicebox/
# 长时间无输出
```

**排查**：
```bash
# 1. 使用 verbose 模式
scp -v file.txt root@129.211.180.183:/opt/voicebox/

# 2. 检查是否等待密码
# 如果看到 "Authenticating to 129.211.180.183:22 as 'root'"
# 然后卡住，说明在等待密码

# 3. 配置密钥或使用 sshpass
ssh-copy-id root@129.211.180.183
```

### 场景 3: MySQL 命令无响应

**症状**：
```bash
mysql -u root -p
# 光标闪烁，无提示
```

**排查**：
```bash
# 1. 检查 MySQL 是否运行
systemctl status mysqld
# 或
ps aux | grep mysql

# 2. 检查是否在等待密码
# 如果看到 "Enter password:" 提示，输入密码

# 3. 使用非交互模式
mysql -u root -pYourPassword -e "SHOW DATABASES;"
```

### 场景 4: 远程命令执行无响应

**症状**：
```bash
ssh root@129.211.180.183 "cd /opt/voicebox && ./start-all.sh"
# 长时间无输出
```

**排查**：
```bash
# 1. 检查命令是否需要交互
# start-all.sh 可能在等待用户输入

# 2. 使用 nohup 后台执行
ssh root@129.211.180.183 "cd /opt/voicebox && nohup ./start-all.sh > /tmp/start.log 2>&1 &"

# 3. 查看日志
ssh root@129.211.180.183 "tail -f /tmp/start.log"
```

---

## 预防措施

### 1. 配置 SSH 密钥（必须）

```bash
# 一次性配置，永久有效
ssh-keygen -t rsa -b 4096
ssh-copy-id root@129.211.180.183
```

### 2. 创建 SSH 配置文件

```bash
# 编辑 ~/.ssh/config
cat >> ~/.ssh/config << 'EOF'
Host voicebox-server
    HostName 129.211.180.183
    User root
    Port 22
    IdentityFile ~/.ssh/id_rsa
    ServerAliveInterval 60
    ServerAliveCountMax 3
    ConnectTimeout 10
EOF

# 使用别名连接
ssh voicebox-server
scp file.txt voicebox-server:/path/
```

### 3. 在脚本中添加超时和错误处理

```bash
#!/bin/bash
set -e  # 遇到错误立即退出

# 设置超时
TIMEOUT=30

# 执行命令
timeout $TIMEOUT ssh root@129.211.180.183 "command" || {
    echo "命令执行失败或超时"
    echo "可能原因："
    echo "1. 需要输入密码（请配置 SSH 密钥）"
    echo "2. 网络连接问题"
    echo "3. 服务器无响应"
    exit 1
}
```

---

## 快速诊断命令

```bash
# 1. 测试 SSH 连接（5秒超时）
ssh -o ConnectTimeout=5 -o BatchMode=yes root@129.211.180.183 "echo OK" 2>&1

# 返回 "OK" - 连接正常，密钥已配置
# 返回 "Permission denied" - 密钥未配置或权限问题
# 返回 "Connection timed out" - 网络问题
# 无响应 - 可能在等待密码

# 2. 检查 SSH 密钥
ls -la ~/.ssh/id_rsa*
ssh-add -l

# 3. 检查服务器密钥
ssh-keygen -F 129.211.180.183

# 4. 测试网络
ping -c 2 129.211.180.183
nc -zv 129.211.180.183 22
```

---

## 故障排查流程图

```
命令无响应
    ↓
等待 10 秒
    ↓
是否有密码提示？
    ├─ 是 → 输入密码 或 配置 SSH 密钥
    └─ 否 → 继续
        ↓
    按 Ctrl+C 中断
        ↓
    测试网络连接
        ├─ ping 服务器
        └─ 测试 SSH 端口
            ↓
        网络正常？
            ├─ 是 → 检查 SSH 密钥配置
            └─ 否 → 检查网络/防火墙
```

---

## 记住这些信号

### 🔴 需要密码的信号
- 命令执行后 5-10 秒无响应
- 没有任何输出或错误信息
- 光标一直闪烁
- 使用 `-v` 参数后看到 "Authenticating" 然后卡住

### 🟡 网络问题的信号
- 命令立即返回 "Connection timed out"
- ping 服务器失败
- 端口测试失败

### 🟢 正常的信号
- 命令快速返回结果
- 有明确的输出或错误信息
- 进度条或状态更新

---

## 最佳实践

1. **始终配置 SSH 密钥** - 这是最根本的解决方案
2. **使用超时参数** - 避免无限等待
3. **添加详细日志** - 便于排查问题
4. **测试后再自动化** - 先手动测试命令，确认无需交互
5. **使用 SSH 配置文件** - 简化连接参数
6. **定期检查连接** - 确保密钥和网络正常

---

## 紧急处理

如果正在执行的命令卡住：

```bash
# 1. 按 Ctrl+C 中断命令

# 2. 立即测试连接
ssh -v root@129.211.180.183 "echo test"

# 3. 如果提示密码，立即配置密钥
ssh-copy-id root@129.211.180.183

# 4. 重新执行命令
```
