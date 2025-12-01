#!/bin/bash

# 语音交互功能部署脚本
# 用于将语音功能部署到生产服务器

set -e

# 配置
SERVER="root@129.211.180.183"
REMOTE_DIR="/opt/voicebox"
LOCAL_DIR="."
BACKUP_DIR="/opt/voicebox-backup"

echo "=========================================="
echo "语音交互功能部署"
echo "=========================================="

# 1. 检查当前分支
CURRENT_BRANCH=$(git branch --show-current)
echo "当前分支: $CURRENT_BRANCH"

# 2. 检查是否有未提交的变更
if [ -n "$(git status --porcelain)" ]; then
    echo "警告: 有未提交的变更"
    git status --short
    read -p "是否继续部署? (yes/no): " CONFIRM
    if [ "$CONFIRM" != "yes" ]; then
        echo "取消部署"
        exit 0
    fi
fi

# 3. 编译项目
echo ""
echo "编译项目..."
cd app-device
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "❌ 编译失败"
    exit 1
fi
echo "✅ 编译成功"
cd ..

# 4. 构建前端
echo ""
echo "构建前端..."
cd app-web
npm install
npm run build
if [ $? -ne 0 ]; then
    echo "❌ 前端构建失败"
    exit 1
fi
echo "✅ 前端构建成功"
cd ..

# 5. 打包文件
echo ""
echo "打包文件..."
tar -czf voicebox-voice.tar.gz \
    --exclude=node_modules \
    --exclude=target \
    --exclude=.git \
    --exclude=*.log \
    --exclude=*.pid \
    --exclude=voice-box-uploads \
    app-device/target/*.jar \
    app-web/dist \
    config.properties \
    deploy/db/schema/04-voice-tables.sql \
    scripts/test-voice-api.sh

echo "✅ 打包完成"

# 6. 上传到服务器
echo ""
echo "上传到服务器..."
scp voicebox-voice.tar.gz $SERVER:/tmp/
if [ $? -ne 0 ]; then
    echo "❌ 上传失败"
    exit 1
fi
echo "✅ 上传成功"

# 7. 在服务器上部署
echo ""
echo "在服务器上部署..."
ssh $SERVER << 'ENDSSH'
set -e

cd /opt/voicebox

echo "停止服务..."
./stop-all.sh || true

echo "备份当前版本..."
BACKUP_DIR="/opt/voicebox-backup/$(date +%Y%m%d_%H%M%S)"
mkdir -p $BACKUP_DIR
cp -r /opt/voicebox/* $BACKUP_DIR/ || true

echo "解压新版本..."
tar -xzf /tmp/voicebox-voice.tar.gz

echo "初始化数据库..."
mysql -u voicebox -pvoicebox123 voicebox_db < deploy/db/schema/04-voice-tables.sql || true

echo "创建语音文件目录..."
mkdir -p /opt/voicebox/voice-files
chmod 755 /opt/voicebox/voice-files

echo "启动服务..."
./start-all.sh

echo "等待服务启动..."
sleep 10

echo "检查服务状态..."
./status.sh

echo "测试语音API..."
curl -s http://localhost:10088/actuator/health || echo "健康检查失败"

echo ""
echo "✅ 部署完成！"
echo "备份位置: $BACKUP_DIR"
ENDSSH

# 8. 清理
rm voicebox-voice.tar.gz

echo ""
echo "=========================================="
echo "部署成功完成！"
echo "=========================================="
echo ""
echo "下一步:"
echo "1. 配置豆包API密钥（参考 .kiro/specs/voice-interaction/DOUBAO_SETUP_GUIDE.md）"
echo "2. 测试语音功能"
echo "3. 查看日志: ssh $SERVER 'tail -f /opt/voicebox/logs/voicebox.log'"
echo ""
