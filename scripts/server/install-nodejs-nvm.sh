#!/bin/bash

# 使用 NVM 安装 Node.js 18

set -e

SERVER="root@129.211.180.183"

echo "=========================================="
echo "   使用 NVM 安装 Node.js 18"
echo "=========================================="
echo ""

ssh $SERVER << 'ENDSSH'
set -e

echo "[1/6] 安装 NVM..."
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash

# 加载 NVM
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"

echo ""
echo "[2/6] 安装 Node.js 16.20.2（兼容 CentOS 7）..."
nvm install 16.20.2
nvm use 16.20.2
nvm alias default 16.20.2

echo ""
echo "[3/6] 验证版本..."
node -v
npm -v

echo ""
echo "[4/6] 降级前端依赖以兼容 Node 16..."
cd /opt/voicebox/app-web

# 备份 package.json
cp package.json package.json.bak

# 修改 package.json 移除 Node 18 要求
cat > /tmp/fix-package.js << 'EOF'
const fs = require('fs');
const pkg = JSON.parse(fs.readFileSync('package.json', 'utf8'));
if (pkg.engines) {
  pkg.engines.node = '>=16.0.0';
}
fs.writeFileSync('package.json', JSON.stringify(pkg, null, 2));
EOF

node /tmp/fix-package.js

echo ""
echo "[5/6] 重新安装依赖并构建..."
rm -rf node_modules package-lock.json
npm install --legacy-peer-deps
npm run build

echo ""
echo "[6/6] 启动服务..."
cd /opt/voicebox
./stop-all.sh 2>/dev/null || true
nohup ./start-all.sh > /tmp/voicebox-start.log 2>&1 &
sleep 15
./status.sh || true

echo ""
echo "✅ 完成！"
ENDSSH

echo "=========================================="
echo "   🎉 安装完成！"
echo "=========================================="
echo ""
echo "服务地址:"
echo "  • 后端: http://129.211.180.183:10088"
echo ""
