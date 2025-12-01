#!/bin/bash

# 自动部署脚本（无需确认）
set -e

SERVER="root@129.211.180.183"
REMOTE_DIR="/opt/voicebox"

echo "=========================================="
echo "   自动部署到生产环境"
echo "=========================================="
echo ""

# 1. 打包代码
echo "[1/4] 打包代码..."
tar -czf voicebox-prod.tar.gz \
    --exclude=node_modules \
    --exclude=target \
    --exclude=.git \
    --exclude=*.log \
    --exclude=*.pid \
    --exclude=config-dev.properties \
    --exclude=voice-box-uploads \
    --exclude=.DS_Store \
    .
echo "✓ 代码打包完成"
echo ""

# 2. 上传到服务器
echo "[2/4] 上传到服务器..."
scp voicebox-prod.tar.gz $SERVER:/tmp/
echo "✓ 上传完成"
echo ""

# 3. 在服务器上部署
echo "[3/4] 在服务器上部署..."
ssh $SERVER << 'ENDSSH'
set -e

cd /opt/voicebox

echo "停止服务..."
./stop-all.sh 2>/dev/null || true

echo "备份当前版本..."
BACKUP_DIR="/opt/voicebox-backup/$(date +%Y%m%d_%H%M%S)"
mkdir -p $BACKUP_DIR
cp -r /opt/voicebox/* $BACKUP_DIR/ 2>/dev/null || true
echo "备份位置: $BACKUP_DIR"

echo "解压新版本..."
tar -xzf /tmp/voicebox-prod.tar.gz

echo "编译后端..."
mvn clean install -DskipTests

echo "构建前端..."
cd app-web
npm install
npm run build
cd ..

echo "启动服务..."
nohup ./start-all.sh > /tmp/start-all.log 2>&1 &

echo "等待服务启动..."
sleep 15

echo "检查服务状态..."
./status.sh || true

echo "✅ 部署完成！"
echo "备份位置: $BACKUP_DIR"
ENDSSH

echo "✓ 服务器部署完成"
echo ""

# 4. 清理本地临时文件
echo "[4/4] 清理临时文件..."
rm voicebox-prod.tar.gz
echo "✓ 清理完成"
echo ""

echo "=========================================="
echo "   🎉 部署完成！"
echo "=========================================="
echo ""
echo "服务地址:"
echo "  • 后端: http://129.211.180.183:10088"
echo "  • 前端: http://129.211.180.183 (需要配置 Nginx)"
echo ""
