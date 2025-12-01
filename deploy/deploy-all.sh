#!/bin/bash
# VoiceBox 一键部署脚本

set -e

echo "=========================================="
echo "VoiceBox 一键部署脚本"
echo "=========================================="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 执行安装步骤
echo "步骤 1/4: 安装依赖环境..."
bash $SCRIPT_DIR/01-install-dependencies.sh

echo ""
echo "步骤 2/4: 安装数据库..."
bash $SCRIPT_DIR/02-setup-database.sh

echo ""
echo "步骤 3/4: 部署应用..."
bash $SCRIPT_DIR/03-deploy-application.sh

echo ""
echo "步骤 4/4: 配置服务..."
bash $SCRIPT_DIR/04-setup-services.sh

echo ""
echo "=========================================="
echo "部署完成！"
echo "=========================================="
echo "访问地址: http://129.211.180.183"
echo "后端 API: http://129.211.180.183/api"
echo "=========================================="
