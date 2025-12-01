#!/bin/bash
# VoiceBox 前端部署脚本（后端代码有编译错误，先部署前端）

set -e

APP_DIR="/opt/voicebox"
LOG_DIR="/var/log/voicebox"

echo "=========================================="
echo "VoiceBox 前端部署"
echo "=========================================="

# 1. 创建目录
echo "1. 创建目录结构..."
mkdir -p $LOG_DIR
cd $APP_DIR

# 2. 清理临时文件
echo "2. 清理临时文件..."
find . -name "._*" -type f -delete 2>/dev/null || true
rm -f /tmp/voicebox-deploy.tar.gz

# 3. 构建前端
echo "3. 构建前端项目..."
cd $APP_DIR/app-web

# 设置 npm 镜像
npm config set registry https://registry.npmmirror.com

# 安装依赖
echo "   安装依赖..."
npm install --silent

# 构建
echo "   构建中..."
npm run build

echo "✓ 前端构建完成"

# 4. 安装 Nginx
echo "4. 配置 Web 服务器..."
if ! command -v nginx &> /dev/null; then
    echo "   安装 Nginx..."
    yum install -y nginx
    systemctl enable nginx
fi

# 5. 配置 Nginx（仅静态文件）
echo "5. 配置 Nginx..."
cat > /etc/nginx/conf.d/voicebox.conf << 'EOF'
server {
    listen 80;
    server_name _;

    # 前端静态文件
    location / {
        root /opt/voicebox/app-web/dist;
        try_files $uri $uri/ /index.html;
        index index.html;
    }

    # 后端 API 代理（暂时返回维护页面）
    location /api/ {
        return 503 '{"error": "Backend service is under maintenance"}';
        add_header Content-Type application/json;
    }
}
EOF

# 测试配置
nginx -t
echo "✓ Nginx 配置完成"

# 6. 启动 Nginx
echo "6. 启动服务..."
systemctl restart nginx

echo ""
echo "=========================================="
echo "✓ 前端部署完成！"
echo "=========================================="
echo ""
echo "📁 目录："
echo "   前端代码: /opt/voicebox/app-web/dist"
echo ""
echo "🌐 访问地址："
echo "   http://129.211.180.183"
echo ""
echo "⚠️  注意："
echo "   后端服务因代码编译错误暂未部署"
echo "   需要修复以下问题后才能部署后端："
echo "   - UserProfile 类缺少方法: getPersonalityType(), needsUpdate(), isConfident()"
echo "   - ConversationFeature 类缺少 Pageable 相关方法"
echo "   - 缺少 Spring Data JPA 依赖"
echo ""
echo "=========================================="
