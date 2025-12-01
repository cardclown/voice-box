#!/bin/bash
# VoiceBox 完整部署脚本（包含 MySQL 配置）

set -e

APP_DIR="/opt/voicebox"
LOG_DIR="/var/log/voicebox"
DATA_DIR="/var/lib/voicebox"

echo "=========================================="
echo "VoiceBox 完整部署"
echo "=========================================="

# 1. 创建目录结构
echo "1. 创建目录结构..."
mkdir -p $LOG_DIR
mkdir -p $DATA_DIR
mkdir -p $DATA_DIR/uploads
cd $APP_DIR

# 2. 清理临时文件
echo "2. 清理临时文件..."
find . -name "._*" -type f -delete 2>/dev/null || true
rm -f /tmp/voicebox-deploy.tar.gz

# 3. 安装 MySQL 5.7（如果未安装）
echo "3. 检查并安装 MySQL 5.7..."
if ! command -v mysql &> /dev/null; then
    echo "   安装 MySQL 5.7..."
    # 添加 MySQL 5.7 仓库
    wget https://dev.mysql.com/get/mysql57-community-release-el7-11.noarch.rpm
    rpm -ivh mysql57-community-release-el7-11.noarch.rpm
    yum install -y mysql-community-server
    
    # 启动 MySQL
    systemctl start mysqld
    systemctl enable mysqld
    
    # 获取临时密码
    TEMP_PASSWORD=$(grep 'temporary password' /var/log/mysqld.log | tail -1 | awk '{print $NF}')
    echo "   MySQL 临时密码: $TEMP_PASSWORD"
    
    # 修改密码策略并创建数据库（MySQL 5.7）
    mysql -uroot -p"$TEMP_PASSWORD" --connect-expired-password << 'EOF'
SET GLOBAL validate_password_policy=LOW;
SET GLOBAL validate_password_length=6;
ALTER USER 'root'@'localhost' IDENTIFIED BY 'root123';
CREATE DATABASE IF NOT EXISTS voicebox_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'voicebox'@'localhost' IDENTIFIED BY 'voicebox123';
GRANT ALL PRIVILEGES ON voicebox_db.* TO 'voicebox'@'localhost';
FLUSH PRIVILEGES;
EOF
    echo "✓ MySQL 5.7 安装并配置完成"
else
    echo "   MySQL 已安装"
    # 确保数据库和用户存在
    mysql -uroot -proot123 << 'EOF' 2>/dev/null || mysql -uroot << 'EOF' 2>/dev/null || true
CREATE DATABASE IF NOT EXISTS voicebox_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'voicebox'@'localhost' IDENTIFIED BY 'voicebox123';
GRANT ALL PRIVILEGES ON voicebox_db.* TO 'voicebox'@'localhost';
FLUSH PRIVILEGES;
EOF
    echo "✓ 数据库检查完成"
fi

# 4. 配置应用
echo "4. 配置应用..."
if [ -f "$APP_DIR/server-config.properties" ]; then
    cp $APP_DIR/server-config.properties $APP_DIR/config.properties
    echo "✓ 使用服务器配置"
else
    echo "⚠️  未找到 server-config.properties，使用默认配置"
fi

# 5. 构建后端
echo "5. 构建后端项目..."
mvn clean package -DskipTests -Dmaven.test.skip=true -q
echo "✓ 后端构建完成"

# 6. 构建前端
echo "6. 构建前端项目..."
cd $APP_DIR/app-web
npm config set registry https://registry.npmmirror.com
npm install --silent
npm run build
echo "✓ 前端构建完成"

# 7. 配置 Nginx
echo "7. 配置 Nginx..."
if ! command -v nginx &> /dev/null; then
    yum install -y nginx
    systemctl enable nginx
fi

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

nginx -t
echo "✓ Nginx 配置完成"

# 8. 创建系统服务
echo "8. 创建系统服务..."
cat > /etc/systemd/system/voicebox-backend.service << 'EOF'
[Unit]
Description=VoiceBox Backend Service
After=network.target mysqld.service
Documentation=https://github.com/your-repo/voicebox

[Service]
Type=simple
User=root
WorkingDirectory=/opt/voicebox
ExecStart=/usr/bin/java -Xmx1024m -Xms512m -jar /opt/voicebox/app-device/target/app-device-0.0.1-SNAPSHOT.jar --spring.config.location=/opt/voicebox/config.properties
Restart=always
RestartSec=10
StandardOutput=append:/var/log/voicebox/backend.log
StandardError=append:/var/log/voicebox/backend-error.log

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
systemctl restart voicebox-backend
systemctl restart nginx

# 等待服务启动
echo "   等待后端服务启动..."
sleep 5

echo ""
echo "=========================================="
echo "✅ 部署完成！"
echo "=========================================="
echo ""
echo "📁 目录结构："
echo "   应用代码: /opt/voicebox"
echo "   应用日志: /var/log/voicebox"
echo "   应用数据: /var/lib/voicebox"
echo ""
echo "🗄️  数据库信息（MySQL 5.7）："
echo "   数据库: voicebox_db"
echo "   用户: voicebox"
echo "   密码: voicebox123"
echo "   Root密码: root123"
echo "   连接: localhost:3306"
echo ""
echo "🚀 服务状态："
echo "   MySQL: $(systemctl is-active mysqld)"
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
echo "   连接数据库: mysql -uvoicebox -pvoicebox123 voicebox_db"
echo ""
echo "=========================================="
