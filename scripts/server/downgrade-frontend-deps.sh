#!/bin/bash

# 降级前端依赖以适配 Node.js 16

set -e

SERVER="root@129.211.180.183"

echo "=========================================="
echo "   降级前端依赖并重新构建"
echo "=========================================="
echo ""

ssh $SERVER << 'ENDSSH'
set -e

echo "[1/4] 当前 Node.js 版本..."
node -v
npm -v
echo ""

echo "[2/4] 降级前端依赖..."
cd /opt/voicebox/app-web

# 备份原始 package.json
cp package.json package.json.backup

# 降级到兼容 Node 16 的版本
cat > package.json << 'EOF'
{
  "name": "app-web",
  "version": "0.0.0",
  "private": true,
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview",
    "test": "vitest"
  },
  "dependencies": {
    "axios": "^1.6.2",
    "pinia": "^2.1.7",
    "vue": "^3.3.8",
    "vue-router": "^4.2.5"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^4.5.0",
    "@vue/test-utils": "^2.4.2",
    "happy-dom": "^12.10.3",
    "vite": "^4.5.0",
    "vitest": "^0.34.6"
  },
  "engines": {
    "node": ">=16.0.0"
  }
}
EOF

echo "✓ package.json 已更新为兼容 Node 16 的版本"
echo ""

echo "[3/4] 重新安装依赖并构建..."
rm -rf node_modules package-lock.json
npm install
npm run build
echo ""

echo "[4/4] 启动服务..."
cd /opt/voicebox
./stop-all.sh 2>/dev/null || true
sleep 2
nohup ./start-all.sh > /tmp/voicebox-start.log 2>&1 &
sleep 15

echo "检查服务状态..."
./status.sh || true
echo ""

echo "✅ 完成！"
echo ""
echo "查看启动日志:"
echo "  tail -f /tmp/voicebox-start.log"
echo ""
ENDSSH

echo "=========================================="
echo "   🎉 部署完成！"
echo "=========================================="
echo ""
echo "服务地址:"
echo "  • 后端: http://129.211.180.183:10088"
echo ""
