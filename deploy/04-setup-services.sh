#!/bin/bash
# 系统服务配置脚本

set -e

echo "=========================================="
echo "配置系统服务"
echo "=========================================="

# 创建后端服务
echo "1. 创建后端服务..."
cat > /etc/systemd/system/voicebox-backend.service << 'EOF'
[Unit]
Description=VoiceBox Backend Service
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/voicebox
ExecStart=/usr/bin/java -jar /opt/voicebox/app-device/target/app-device-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

# 配置 Nginx
echo "2. 配置 Nginx..."
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
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket 支持
    location /ws/ {
        proxy_pass http://localhost:8080/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }
}
EOF

# 启动服务
echo "3. 启动服务..."
systemctl daemon-reload
systemctl enable voicebox-backend
systemctl start voicebox-backend
systemctl restart nginx

echo "=========================================="
echo "服务配置完成！"
echo "=========================================="
echo "后端服务状态: $(systemctl is-active voicebox-backend)"
echo "Nginx 状态: $(systemctl is-active nginx)"
echo "=========================================="
