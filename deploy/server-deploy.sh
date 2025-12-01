#!/bin/bash
# VoiceBox 规范化部署脚本
# 目录结构：
# /opt/voicebox          - 应用代码
# /var/log/voicebox      - 应用日志
# /var/lib/voicebox      - 应用数据
# /etc/systemd/system    - 系统服务
# /etc/nginx/conf.d      - Nginx 配置

set -e

APP_DIR="/opt/voicebox"
LOG_DIR="/var/log/voicebox"
DATA_DIR="/var/lib/voicebox"

echo "=========================================="
echo "VoiceBox 规范化部署"
echo "=========================================="

# 1. 创建规范目录结构
echo "1. 创建目录结构..."
mkdir -p $LOG_DIR
mkdir -p $DATA_DIR
mkdir -p $DATA_DIR/uploads
cd $APP_DIR

# 2. 清理临时文件
echo "2. 清理临时文件..."
find . -name "._*" -type f -delete 2>/dev/null || true
rm -f /tmp/voicebox-deploy.tar.gz

# 3. 配置环境
echo "3. 配置应用环境..."
if [ ! -f "config.properties" ]; then
  if [ -f "env-example.properties" ]; then
    cp env-example.properties config.properties
    echo "已创建 config.properties，使用默认配置"
  fi
fi

# 4. 构建后端
echo "4. 构建后端项目（这可能需要几分钟）..."
mvn clean package -DskipTests -q
echo "✓ 后端构建完成"

# 5. 构建前端
echo "5. 构建前端项目..."
cd $APP_DIR/app-web

# 设置 npm 镜像加速（使用淘宝镜像）
npm config set registry https://registry.npmmirror.com

# 安装依赖
echo "   安装前端依赖..."
npm install --silent

# 构建
echo "   构建前端..."
npm run build

echo "✓ 前端构建完成"

# 6. 安装 Nginx（如果未安装）
echo "6. 配置 Web 服务器..."
if ! command -v nginx &> /dev/null; then
    echo "   安装 Nginx..."
    yum install -y nginx -q
    systemctl enable nginx
fi

# 7. 配置 Nginx
echo "7. 配置 Nginx 反向代理..."
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

    # 后端 API 代理
    location /api/ {
        proxy_pass http://localhost:10088/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # WebSocket 支持
    location /ws/ {
        proxy_pass http://localhost:10088/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }
}
EOF

# 测试 Nginx 配置
nginx -t
echo "✓ Nginx 配置验证通过"

# 8. 创建系统服务
echo "8. 创建 systemd 服务..."
cat > /etc/systemd/system/voicebox-backend.service << 'EOF'
[Unit]
Description=VoiceBox Backend Service
After=network.target
Documentation=https://github.com/your-repo/voicebox

[Service]
Type=simple
User=root
WorkingDirectory=/opt/voicebox
ExecStart=/usr/bin/java -Xmx1024m -Xms512m -jar /opt/voicebox/app-device/target/app-device-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10
StandardOutput=append:/var/log/voicebox/backend.log
StandardError=append:/var/log/voicebox/backend-error.log

# 环境变量
Environment="JAVA_HOME=/usr/lib/jvm/java-11-openjdk"
Environment="LOG_PATH=/var/log/voicebox"
Environment="DATA_PATH=/var/lib/voicebox"

[Install]
WantedBy=multi-user.target
EOF

echo "✓ 系统服务配置完成"

# 9. 启动服务
echo "9. 启动服务..."
systemctl daemon-reload
systemctl enable voicebox-backend
systemctl start voicebox-backend
systemctl restart nginx

# 等待服务启动
echo "   等待服务启动..."
sleep 3

echo ""
echo "=========================================="
echo "✓ 部署完成！"
echo "=========================================="
echo ""
echo "📁 目录结构："
echo "   应用代码: /opt/voicebox"
echo "   应用日志: /var/log/voicebox"
echo "   应用数据: /var/lib/voicebox"
echo ""
echo "🚀 服务状态："
echo "   后端服务: $(systemctl is-active voicebox-backend)"
echo "   Web服务器: $(systemctl is-active nginx)"
echo ""
echo "🌐 访问地址："
echo "   http://129.211.180.183"
echo ""
echo "📊 管理命令："
echo "   查看后端日志: tail -f /var/log/voicebox/backend.log"
echo "   查看错误日志: tail -f /var/log/voicebox/backend-error.log"
echo "   重启后端: systemctl restart voicebox-backend"
echo "   重启Nginx: systemctl restart nginx"
echo "   查看状态: systemctl status voicebox-backend"
echo ""
echo "=========================================="
