#!/bin/bash
# VoiceBox 服务器环境安装脚本 - CentOS 7

set -e

echo "=========================================="
echo "开始安装 VoiceBox 运行环境"
echo "=========================================="

# 更新系统
echo "1. 更新系统包..."
yum update -y

# 安装基础工具
echo "2. 安装基础工具..."
yum install -y wget curl git vim unzip

# 安装 Java 17
echo "3. 安装 Java 17..."
yum install -y java-17-openjdk java-17-openjdk-devel
java -version

# 安装 Maven
echo "4. 安装 Maven..."
cd /opt
wget https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz
tar -xzf apache-maven-3.9.6-bin.tar.gz
ln -sf /opt/apache-maven-3.9.6/bin/mvn /usr/bin/mvn
mvn -version

# 安装 Node.js 18
echo "5. 安装 Node.js 18..."
curl -fsSL https://rpm.nodesource.com/setup_18.x | bash -
yum install -y nodejs
node -v
npm -v

# 安装 Docker
echo "6. 安装 Docker..."
yum install -y yum-utils
yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
yum install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
systemctl start docker
systemctl enable docker
docker -v

# 安装 Docker Compose
echo "7. 安装 Docker Compose..."
curl -L "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
docker-compose --version

# 安装 Nginx
echo "8. 安装 Nginx..."
yum install -y nginx
systemctl enable nginx

# 配置防火墙
echo "9. 配置防火墙..."
systemctl start firewalld
systemctl enable firewalld
firewall-cmd --permanent --add-service=http
firewall-cmd --permanent --add-service=https
firewall-cmd --permanent --add-port=8080/tcp
firewall-cmd --permanent --add-port=3000/tcp
firewall-cmd --reload

echo "=========================================="
echo "环境安装完成！"
echo "=========================================="
echo "Java: $(java -version 2>&1 | head -n 1)"
echo "Maven: $(mvn -version | head -n 1)"
echo "Node: $(node -v)"
echo "Docker: $(docker -v)"
echo "=========================================="
