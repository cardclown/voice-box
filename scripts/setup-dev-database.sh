#!/bin/bash

# 在生产服务器上创建开发数据库
# 用法: ./scripts/setup-dev-database.sh

set -e

SERVER="root@129.211.180.183"

echo "=========================================="
echo "在服务器上创建开发数据库"
echo "=========================================="

ssh $SERVER << 'ENDSSH'
set -e

echo "1. 创建开发数据库和用户..."
mysql -u root -proot123 << 'EOF'
-- 创建开发数据库
CREATE DATABASE IF NOT EXISTS voicebox_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建开发用户
CREATE USER IF NOT EXISTS 'voicebox_dev'@'%' IDENTIFIED BY 'dev123';
GRANT ALL PRIVILEGES ON voicebox_dev.* TO 'voicebox_dev'@'%';
FLUSH PRIVILEGES;

-- 验证
SELECT user, host FROM mysql.user WHERE user='voicebox_dev';
SHOW DATABASES LIKE 'voicebox%';
EOF

echo "2. 初始化开发数据库结构..."
cd /opt/voicebox

# 执行所有 schema 脚本
for schema_file in deploy/db/schema/*.sql; do
    if [ -f "$schema_file" ]; then
        echo "执行: $schema_file"
        mysql -u voicebox_dev -pdev123 voicebox_dev < "$schema_file"
    fi
done

echo "3. 插入测试数据..."
if [ -f "deploy/db/test-data/sample-data.sql" ]; then
    mysql -u voicebox_dev -pdev123 voicebox_dev < deploy/db/test-data/sample-data.sql
fi

echo "4. 验证数据库..."
mysql -u voicebox_dev -pdev123 voicebox_dev -e "SHOW TABLES;"

echo "✅ 开发数据库创建完成！"
echo ""
echo "数据库信息："
echo "  主机: 129.211.180.183"
echo "  端口: 3306"
echo "  数据库: voicebox_dev"
echo "  用户名: voicebox_dev"
echo "  密码: dev123"
ENDSSH

echo "=========================================="
echo "开发数据库设置完成"
echo "=========================================="
