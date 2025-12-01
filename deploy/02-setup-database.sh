#!/bin/bash
# 数据库安装和配置脚本

set -e

echo "=========================================="
echo "安装和配置 MySQL 数据库"
echo "=========================================="

# 安装 MySQL 8.0
echo "1. 安装 MySQL 8.0..."
yum install -y https://dev.mysql.com/get/mysql80-community-release-el7-7.noarch.rpm
yum install -y mysql-community-server

# 启动 MySQL
echo "2. 启动 MySQL 服务..."
systemctl start mysqld
systemctl enable mysqld

# 获取临时密码
echo "3. 获取 MySQL 临时密码..."
TEMP_PASSWORD=$(grep 'temporary password' /var/log/mysqld.log | awk '{print $NF}')
echo "临时密码: $TEMP_PASSWORD"

# 创建密码重置脚本
cat > /tmp/mysql_secure.sql << 'EOF'
ALTER USER 'root'@'localhost' IDENTIFIED BY 'VoiceBox@2024';
CREATE DATABASE IF NOT EXISTS voicebox CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'voicebox'@'%' IDENTIFIED BY 'VoiceBox@2024';
GRANT ALL PRIVILEGES ON voicebox.* TO 'voicebox'@'%';
FLUSH PRIVILEGES;
EOF

echo "4. 配置 MySQL..."
mysql -uroot -p"$TEMP_PASSWORD" --connect-expired-password < /tmp/mysql_secure.sql

echo "=========================================="
echo "MySQL 安装完成！"
echo "=========================================="
echo "数据库名: voicebox"
echo "用户名: voicebox"
echo "密码: VoiceBox@2024"
echo "=========================================="
