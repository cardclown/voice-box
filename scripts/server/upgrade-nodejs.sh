#!/bin/bash

# 升级服务器 Node.js 到 18.x 并重新构建前端

set -e

SERVER="root@129.211.180.183"

echo "=========================================="
echo "   升级服务器 Node.js 并构建前端"
echo "=========================================="
echo ""

ssh $SERVER << 'ENDSSH'
set -e

echo "[1/5] 检查当前 Node.js 版本..."
node -v || echo "Node.js 未安装"
echo ""

echo "[2/5] 安装 Node.js 18.x..."
curl -fsSL https://rpm.nodesource.com/setup_18.x | bash -
yum install -y nodejs
echo ""

echo "[3/5] 验证新版本..."
node -v
npm -v
echo ""

echo "[4/5] 构建前端..."
cd /opt/voicebox/app-web
rm -rf node_modules package-lock.json
npm install
npm run build
echo ""

echo "[5/5] 启动服务..."
cd /opt/voicebox
./stop-all.sh 2>/dev/null || true
nohup ./start-all.sh > /tmp/voicebox-start.log 2>&1 &
sleep 10
./status.sh || true
echo ""

echo "✅ 完成！"
echo ""
echo "服务地址:"
echo "  • 后端: http://129.211.180.183:10088"
echo ""
ENDSSH

echo "=========================================="
echo "   🎉 升级完成！"
echo "=========================================="
