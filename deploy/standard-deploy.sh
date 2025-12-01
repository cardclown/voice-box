#!/bin/bash
# VoiceBox 标准规范部署脚本
# 使用 MySQL 5.7，不使用 Redis

set -e

APP_DIR="/opt/voicebox"
LOG_DIR="/var/log/voicebox"
DATA_DIR="/var/lib/voicebox"

echo "=========================================="
echo "VoiceBox 标准部署"
echo "=========================================="

# 1. 创建规范目录结构
echo "1. 创建目录结构..."
mkdir -p $LOG_DIR
mkdir -p $DATA_DIR/uploads
cd $APP_DIR

# 2. 清理临时文件
echo "2. 清理临时文件..."
find . -name "._*" -type f -delete 2>/dev/null || true
rm -f /tmp/voicebox*.tar.gz

# 3. 安装 MySQL 5.7
echo "3. 配置 MySQL 5.7..."
if ! command -v mysql &> /dev/null; then
    echo "   安装 MySQL 5.7..."
    wget -q https://dev.mysql.com/get/mysql57-community-release-el7-11.noarch.rpm
    rpm -ivh mysql57-community-release-el7-11.noarch.rpm
    yum install -y mysql-community-server
    
    systemctl start mysqld
    systemctl enable mysqld
    
    # 获取临时密码并配置
    TEMP_PASS=$(grep 'temporary password' /var/log/mysqld.log | tail -1 | awk '{print $NF}')
    
    # 创建配置脚本
    cat > /tmp/mysql_init.sql << 'SQLEOF'
SET GLOBAL validate_password_policy=LOW;
SET GLOBAL validate_password_length=6;
ALTER USER 'root'@'localhost' IDENTIFIED BY 'root123';
CREATE DATABASE IF NOT EXISTS voicebox_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'voicebox'@'localhost' IDENTIFIED BY 'voicebox123';
GRANT ALL PRIVILEGES ON voicebox_db.* TO 'voicebox'@'localhost';
FLUSH PRIVILEGES;
SQLEOF
    
    mysql -uroot -p"$TEMP_PASS" --connect-expired-password < /tmp/mysql_init.sql
    rm -f /tmp/mysql_init.sql
    echo "✓ MySQL 5.7 安装完成"
else
    echo "   MySQL 已安装，确保数据库存在..."
    mysql -uroot -proot123 -e "CREATE DATABASE IF NOT EXISTS voicebox_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null || true
    mysql -uroot -proot123 -e "CREATE USER IF NOT EXISTS 'voicebox'@'localhost' IDENTIFIED BY 'voicebox123';" 2>/dev/null || true
    mysql -uroot -proot123 -e "GRANT ALL PRIVILEGES ON voicebox_db.* TO 'voicebox'@'localhost'; FLUSH PRIVILEGES;" 2>/dev/null || true
    echo "✓ 数据库配置完成"
fi

# 4. 配置应用
echo "4. 配置应用..."
if [ -f "$APP_DIR/deploy/server-config.properties" ]; then
    cp $APP_DIR/deploy/server-config.properties $APP_DIR/config.properties
    echo "✓ 配置文件已更新"
fi

# 5. 构建后端
echo "5. 构建后端..."
mvn clean package -DskipTests -Dmaven.test.skip=true -q
echo "✓ 后端构建完成"

# 6. 构建前端
echo "6. 构建前端..."
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

cat > /etc/nginx/conf.d/voicebox.conf << 'NGINXEOF'
server {
    listen 80;
    server_name _;

    location / {
        root /opt/voicebox/app-web/dist;
        try_files $uri $uri/ /index.html;
        index index.html;
    }

    location /api/ {
        proxy_pass http://localhost:10088/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    location /ws/ {
        proxy_pass http://localhost:10088/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }
}
NGINXEOF

nginx -t
echo "✓ Nginx 配置完成"

# 8. 创建系统服务
echo "8. 创建系统服务..."
cat > /etc/systemd/system/voicebox-backend.service << 'SERVICEEOF'
[Unit]
Description=VoiceBox Backend Service
After=network.target mysqld.service

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

[Install]
WantedBy=multi-user.target
SERVICEEOF

echo "✓ 系统服务配置完成"

# 9. 启动服务
echo "9. 启动服务..."
systemctl daemon-reload
systemctl enable voicebox-backend
systemctl restart voicebox-backend
systemctl restart nginx

sleep 3

echo ""
echo "=========================================="
echo "✅ 部署完成！"
echo "=========================================="
echo ""
echo "📁 规范目录结构："
echo "   /opt/voicebox          - 应用代码"
echo "   /var/log/voicebox      - 应用日志"
echo "   /var/lib/voicebox      - 应用数据"
echo ""
echo "🗄️  MySQL 5.7 配置："
echo "   数据库: voicebox_db"
echo "   用户: voicebox / voicebox123"
echo "   Root: root / root123"
echo ""
echo "🚀 服务状态："
echo "   MySQL: $(systemctl is-active mysqld)"
echo "   后端: $(systemctl is-active voicebox-backend)"
echo "   Nginx: $(systemctl is-active nginx)"
echo ""
echo "🌐 访问: http://129.211.180.183"
echo ""
echo "📊 管理命令："
echo "   tail -f /var/log/voicebox/backend.log"
echo "   systemctl restart voicebox-backend"
echo "   mysql -uvoicebox -pvoicebox123 voicebox_db"
echo ""
echo "=========================================="
