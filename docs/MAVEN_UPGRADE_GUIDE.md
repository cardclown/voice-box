# Maven 升级指南

## 概述

本文档说明如何将云服务器的 Maven 从 3.0.5 升级到 3.9.9，并同步更新相关配置文档。

---

## 当前状态

### 服务器环境
- **服务器地址**: 129.211.180.183
- **当前 Maven 版本**: 3.0.5 (Red Hat 3.0.5-17)
- **安装方式**: yum 安装
- **Maven Home**: /usr/share/maven

### 目标版本
- **目标 Maven 版本**: 3.9.9
- **安装方式**: 手动安装到 /opt/maven
- **原因**: 
  - 3.0.5 版本过旧，不支持新特性
  - 与本地开发环境保持一致
  - 更好的性能和安全性

---

## 升级步骤

### 方式一：使用自动化脚本（推荐）

#### 1. 上传升级脚本到服务器

```bash
# 在本地执行
scp scripts/server/upgrade-maven.sh root@129.211.180.183:/tmp/
```

#### 2. 连接服务器并执行脚本

```bash
# 连接服务器
ssh root@129.211.180.183

# 执行升级脚本
cd /tmp
chmod +x upgrade-maven.sh
./upgrade-maven.sh
```

#### 3. 验证升级结果

```bash
# 使环境变量生效
source /etc/profile.d/maven.sh

# 验证版本
mvn -version

# 应该看到：
# Apache Maven 3.9.9
# Maven home: /opt/maven
# Java version: 11.0.23, vendor: Red Hat, Inc.
```

#### 4. 重启后端服务

```bash
cd /opt/voicebox
systemctl stop voicebox-backend
mvn clean install -DskipTests
systemctl start voicebox-backend
systemctl status voicebox-backend
```

---

### 方式二：手动升级

#### 1. 下载 Maven 3.9.9

```bash
# 连接服务器
ssh root@129.211.180.183

# 下载 Maven
cd /tmp
wget https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz
```

#### 2. 备份旧版本

```bash
# 备份旧的 Maven（如果存在）
if [ -d "/opt/maven" ]; then
    mv /opt/maven /opt/maven.backup.$(date +%Y%m%d_%H%M%S)
fi
```

#### 3. 安装新版本

```bash
# 解压到 /opt
tar -xzf apache-maven-3.9.9-bin.tar.gz
mv apache-maven-3.9.9 /opt/maven
```

#### 4. 配置环境变量

```bash
# 创建环境变量配置文件
cat > /etc/profile.d/maven.sh << 'EOF'
# Maven 环境变量
export MAVEN_HOME=/opt/maven
export PATH=${MAVEN_HOME}/bin:${PATH}
EOF

# 设置执行权限
chmod +x /etc/profile.d/maven.sh

# 使环境变量生效
source /etc/profile.d/maven.sh
```

#### 5. 移除旧版本

```bash
# 移除 yum 安装的旧版本
yum remove -y maven

# 或者只是禁用，不删除
# rpm -qa | grep maven
```

#### 6. 验证安装

```bash
# 验证版本
mvn -version

# 验证路径
which mvn
# 应该显示: /opt/maven/bin/mvn

# 测试编译
cd /opt/voicebox
mvn clean compile
```

---

## 升级后配置

### 1. 更新 systemd 服务文件

如果后端服务使用 systemd 管理，需要更新环境变量：

```bash
# 编辑服务文件
vim /etc/systemd/system/voicebox-backend.service

# 确保包含 Maven 路径
[Service]
Environment="MAVEN_HOME=/opt/maven"
Environment="PATH=/opt/maven/bin:/usr/local/bin:/usr/bin:/bin"

# 重新加载配置
systemctl daemon-reload
systemctl restart voicebox-backend
```

### 2. 更新部署脚本

确保部署脚本使用新的 Maven：

```bash
# 编辑 /opt/voicebox/start-all.sh
# 在脚本开头添加
export MAVEN_HOME=/opt/maven
export PATH=${MAVEN_HOME}/bin:${PATH}
```

### 3. 配置 Maven 本地仓库（可选）

```bash
# 创建 Maven 配置目录
mkdir -p ~/.m2

# 配置 settings.xml
cat > ~/.m2/settings.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">
    
    <!-- 本地仓库路径 -->
    <localRepository>/opt/maven-repo</localRepository>
    
    <!-- 使用阿里云镜像加速（可选） -->
    <mirrors>
        <mirror>
            <id>aliyun</id>
            <mirrorOf>central</mirrorOf>
            <name>Aliyun Maven Mirror</name>
            <url>https://maven.aliyun.com/repository/public</url>
        </mirror>
    </mirrors>
</settings>
EOF

# 创建本地仓库目录
mkdir -p /opt/maven-repo
```

---

## 验证清单

升级完成后，请验证以下内容：

### ✅ Maven 版本验证

```bash
# 1. 检查版本
mvn -version
# 应该显示: Apache Maven 3.9.9

# 2. 检查路径
which mvn
# 应该显示: /opt/maven/bin/mvn

# 3. 检查环境变量
echo $MAVEN_HOME
# 应该显示: /opt/maven
```

### ✅ 编译测试

```bash
# 1. 清理并编译项目
cd /opt/voicebox
mvn clean compile

# 2. 运行测试
mvn test

# 3. 打包项目
mvn clean package -DskipTests
```

### ✅ 服务运行测试

```bash
# 1. 重启后端服务
systemctl restart voicebox-backend

# 2. 检查服务状态
systemctl status voicebox-backend

# 3. 测试 API
curl -X POST http://localhost:10088/api/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"测试","userId":1}'
```

---

## 回滚方案

如果升级后出现问题，可以回滚到旧版本：

```bash
# 1. 停止服务
systemctl stop voicebox-backend

# 2. 恢复旧版本
rm -rf /opt/maven
mv /opt/maven.backup.YYYYMMDD_HHMMSS /opt/maven

# 或者重新安装 yum 版本
yum install -y maven

# 3. 重新加载环境变量
source /etc/profile.d/maven.sh

# 4. 验证版本
mvn -version

# 5. 重启服务
systemctl start voicebox-backend
```

---

## 常见问题

### Q1: 升级后找不到 mvn 命令

**原因**: 环境变量未生效

**解决方案**:
```bash
# 重新加载环境变量
source /etc/profile.d/maven.sh

# 或者重新登录
exit
ssh root@129.211.180.183

# 或者使用完整路径
/opt/maven/bin/mvn -version
```

### Q2: 编译时出现依赖下载失败

**原因**: 网络问题或仓库配置问题

**解决方案**:
```bash
# 1. 配置阿里云镜像
vim ~/.m2/settings.xml
# 添加阿里云镜像配置（见上文）

# 2. 清理本地仓库
rm -rf ~/.m2/repository

# 3. 重新下载依赖
mvn clean install -U
```

### Q3: systemd 服务启动失败

**原因**: 服务文件中的环境变量未更新

**解决方案**:
```bash
# 1. 编辑服务文件
vim /etc/systemd/system/voicebox-backend.service

# 2. 添加 Maven 环境变量
[Service]
Environment="MAVEN_HOME=/opt/maven"
Environment="PATH=/opt/maven/bin:/usr/local/bin:/usr/bin:/bin"

# 3. 重新加载并重启
systemctl daemon-reload
systemctl restart voicebox-backend
```

### Q4: 旧版本 Maven 仍然在运行

**原因**: PATH 中旧版本优先级更高

**解决方案**:
```bash
# 1. 检查所有 Maven 路径
which -a mvn

# 2. 移除 yum 安装的版本
yum remove -y maven

# 3. 或者调整 PATH 顺序
export PATH=/opt/maven/bin:$PATH
```

---

## 同步到本地开发环境

升级服务器 Maven 后，建议本地开发环境也使用相同版本：

### macOS

```bash
# 使用 Homebrew
brew install maven

# 或下载指定版本
wget https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz
tar -xzf apache-maven-3.9.9-bin.tar.gz
sudo mv apache-maven-3.9.9 /usr/local/maven

# 配置环境变量
echo 'export MAVEN_HOME=/usr/local/maven' >> ~/.zshrc
echo 'export PATH=$MAVEN_HOME/bin:$PATH' >> ~/.zshrc
source ~/.zshrc

# 验证
mvn -version
```

### Linux

```bash
# 下载并安装
wget https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz
tar -xzf apache-maven-3.9.9-bin.tar.gz
sudo mv apache-maven-3.9.9 /opt/maven

# 配置环境变量
echo 'export MAVEN_HOME=/opt/maven' >> ~/.bashrc
echo 'export PATH=$MAVEN_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc

# 验证
mvn -version
```

---

## 文档更新清单

升级完成后，需要更新以下文档：

- [x] `docs/SERVER_ENVIRONMENT.md` - 更新 Maven 版本信息
- [x] `docs/MAVEN_UPGRADE_GUIDE.md` - 本文档
- [x] `QUICK_START.md` - 更新环境要求
- [x] `deploy/README.md` - 更新部署说明
- [x] `README.md` - 更新项目要求（如果有）

---

## 升级记录

| 日期 | 操作人 | 旧版本 | 新版本 | 状态 | 备注 |
|------|--------|--------|--------|------|------|
| 2024-11-29 | Kiro AI | 3.0.5 | 3.9.9 | ✅ 已完成 | 升级成功，服务运行正常 |

---

## 参考资料

- [Apache Maven 官方网站](https://maven.apache.org/)
- [Maven 3.9.9 发布说明](https://maven.apache.org/docs/3.9.9/release-notes.html)
- [Maven 下载页面](https://maven.apache.org/download.cgi)
