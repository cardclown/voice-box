#!/bin/bash
# VoiceBox 本地到服务器部署脚本
# 使用 SSH 密钥认证，无需密码

set -e

# 配置
SERVER="voicebox-server"  # 使用 SSH 配置的别名
REMOTE_DIR="/opt/voicebox"
LOCAL_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "=========================================="
echo "VoiceBox 本地部署到服务器"
echo "=========================================="
echo ""
echo "📍 本地目录: $LOCAL_DIR"
echo "🌐 目标服务器: $SERVER"
echo "📁 远程目录: $REMOTE_DIR"
echo ""

# 1. 测试 SSH 连接
echo "1. 测试服务器连接..."
if ! ssh -o BatchMode=yes -o ConnectTimeout=5 $SERVER "echo '连接成功'" > /dev/null 2>&1; then
    echo "❌ 无法连接到服务器"
    echo "请确保 SSH 密钥已配置："
    echo "  ssh-copy-id root@129.211.180.183"
    exit 1
fi
echo "✓ 服务器连接正常"

# 2. 本地构建（可选）
read -p "是否在本地构建后端？(y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "2. 本地构建后端..."
    cd "$LOCAL_DIR"
    mvn clean package -DskipTests -q
    echo "✓ 本地构建完成"
else
    echo "2. 跳过本地构建"
fi

# 3. 打包代码
echo "3. 打包代码..."
cd "$LOCAL_DIR"
tar -czf /tmp/voicebox-deploy.tar.gz \
    --exclude='node_modules' \
    --exclude='target' \
    --exclude='.git' \
    --exclude='*.log' \
    --exclude='*.pid' \
    --exclude='.DS_Store' \
    --exclude='._*' \
    --exclude='app-web/dist' \
    .
echo "✓ 代码打包完成"

# 4. 上传到服务器
echo "4. 上传到服务器..."
scp /tmp/voicebox-deploy.tar.gz $SERVER:/tmp/
echo "✓ 上传完成"

# 5. 在服务器上部署
echo "5. 在服务器上部署..."
ssh $SERVER << 'ENDSSH'
set -e

echo "   停止服务..."
systemctl stop voicebox-backend 2>/dev/null || true

echo "   备份当前版本..."
if [ -d "/opt/voicebox" ]; then
    tar -czf /tmp/voicebox-backup-$(date +%Y%m%d_%H%M%S).tar.gz -C /opt voicebox 2>/dev/null || true
fi

echo "   解压新代码..."
mkdir -p /opt/voicebox
cd /opt/voicebox
tar -xzf /tmp/voicebox-deploy.tar.gz

echo "   清理临时文件..."
find . -name "._*" -type f -delete 2>/dev/null || true
rm -f /tmp/voicebox-deploy.tar.gz

echo "   构建后端..."
mvn clean package -DskipTests -q

echo "   构建前端..."
cd /opt/voicebox/app-web
npm config set registry https://registry.npmmirror.com
npm install --silent
npm run build

echo "   启动服务..."
systemctl start voicebox-backend
systemctl restart nginx

echo "   等待服务启动..."
sleep 3

ENDSSH

echo "✓ 服务器部署完成"

# 6. 验证部署
echo ""
echo "6. 验证部署..."
ssh $SERVER << 'ENDSSH'
echo "   后端服务: $(systemctl is-active voicebox-backend)"
echo "   Nginx: $(systemctl is-active nginx)"
echo ""
echo "   检查后端日志（最后 5 行）："
tail -5 /var/log/voicebox/backend.log 2>/dev/null || echo "   日志文件不存在"
ENDSSH

# 7. 清理本地临时文件
rm -f /tmp/voicebox-deploy.tar.gz

echo ""
echo "=========================================="
echo "✅ 部署完成！"
echo "=========================================="
echo ""
echo "🌐 访问地址: http://129.211.180.183"
echo ""
echo "📊 查看日志:"
echo "   ssh $SERVER 'tail -f /var/log/voicebox/backend.log'"
echo ""
echo "🔄 重启服务:"
echo "   ssh $SERVER 'systemctl restart voicebox-backend'"
echo ""
echo "=========================================="
