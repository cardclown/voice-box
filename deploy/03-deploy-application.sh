#!/bin/bash
# 应用部署脚本

set -e

APP_DIR="/opt/voicebox"
GIT_REPO="你的Git仓库地址"  # 需要替换

echo "=========================================="
echo "部署 VoiceBox 应用"
echo "=========================================="

# 创建应用目录
echo "1. 创建应用目录..."
mkdir -p $APP_DIR
cd $APP_DIR

# 克隆代码（如果使用 Git）
# echo "2. 克隆代码..."
# git clone $GIT_REPO .

# 或者从本地上传代码
echo "2. 等待代码上传..."
echo "请在本地执行: scp -r /path/to/voice-box root@129.211.180.183:/opt/"

# 配置环境变量
echo "3. 配置环境变量..."
cat > $APP_DIR/config.properties << 'EOF'
# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/voicebox?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
spring.datasource.username=voicebox
spring.datasource.password=VoiceBox@2024

# 服务器配置
server.port=8080

# 日志配置
logging.level.root=INFO
logging.file.path=/var/log/voicebox
EOF

# 构建后端
echo "4. 构建后端应用..."
cd $APP_DIR
mvn clean package -DskipTests

# 构建前端
echo "5. 构建前端应用..."
cd $APP_DIR/app-web
npm install
npm run build

echo "=========================================="
echo "应用构建完成！"
echo "=========================================="
