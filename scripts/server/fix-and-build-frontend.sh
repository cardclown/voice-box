#!/bin/bash

# 修复依赖并重新构建前端

set -e

SERVER="root@129.211.180.183"

echo "=========================================="
echo "   修复依赖并重新构建前端"
echo "=========================================="
echo ""

ssh $SERVER << 'ENDSSH'
set -e

cd /opt/voicebox/app-web

echo "[1/3] 添加缺失的依赖..."
npm install highlight.js marked --save
echo ""

echo "[2/3] 重新构建..."
npm run build
echo ""

echo "[3/3] 启动服务..."
cd /opt/voicebox
./stop-all.sh 2>/dev/null || true
sleep 2
nohup ./start-all.sh > /tmp/voicebox-start.log 2>&1 &
sleep 15

echo "检查服务状态..."
./status.sh || true
echo ""

echo "✅ 完成！"
ENDSSH

echo "=========================================="
echo "   🎉 部署完成！"
echo "=========================================="
echo ""
echo "服务地址:"
echo "  • 后端: http://129.211.180.183:10088"
echo ""
