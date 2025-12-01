#!/bin/bash
# VoiceBox 清理并重新规范部署脚本

set -e

echo "=========================================="
echo "清理不规范的安装"
echo "=========================================="

# 1. 停止所有相关服务
echo "1. 停止服务..."
systemctl stop voicebox-backend 2>/dev/null || true
systemctl stop mysqld 2>/dev/null || true
systemctl stop nginx 2>/dev/null || true

# 2. 卸载 MySQL 8.0（如果已安装）
echo "2. 检查并卸载 MySQL 8.0..."
if rpm -qa | grep -q mysql-community-server; then
    echo "   卸载 MySQL 8.0..."
    yum remove -y mysql-community-server mysql-community-client mysql-community-common mysql-community-libs
    yum remove -y mysql80-community-release
    rm -f /etc/yum.repos.d/mysql-community*.repo
    echo "✓ MySQL 8.0 已卸载"
fi

# 3. 清理 MySQL 数据（可选，保留数据则注释掉）
echo "3. 清理 MySQL 数据..."
read -p "是否清理 MySQL 数据？(y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    rm -rf /var/lib/mysql
    rm -f /var/log/mysqld.log
    echo "✓ MySQL 数据已清理"
else
    echo "✓ 保留 MySQL 数据"
fi

# 4. 清理临时文件
echo "4. 清理临时文件..."
rm -f /tmp/voicebox*.tar.gz
rm -f /tmp/mysql*.rpm
rm -f mysql*.rpm

echo ""
echo "=========================================="
echo "✓ 清理完成！"
echo "=========================================="
echo ""
echo "现在可以执行规范部署："
echo "bash /opt/voicebox/deploy/complete-deploy.sh"
echo ""
