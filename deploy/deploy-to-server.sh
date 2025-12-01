#!/bin/bash
# VoiceBox 项目部署脚本

SERVER="129.211.180.183"
USER="root"
REMOTE_DIR="/opt/voicebox"

echo "=========================================="
echo "开始部署 VoiceBox 到服务器"
echo "=========================================="

# 1. 打包项目（排除不必要的文件）
echo "1. 打包项目..."
tar -czf voicebox-deploy.tar.gz \
  --exclude='node_modules' \
  --exclude='target' \
  --exclude='.git' \
  --exclude='*.log' \
  --exclude='*.pid' \
  --exclude='voice-box-uploads' \
  --exclude='deploy' \
  --exclude='*.tar.gz' \
  .

echo "打包完成: voicebox-deploy.tar.gz"

# 2. 上传到服务器
echo "2. 上传到服务器..."
scp voicebox-deploy.tar.gz $USER@$SERVER:/tmp/

# 3. 在服务器上解压
echo "3. 解压文件..."
ssh $USER@$SERVER << 'ENDSSH'
  mkdir -p /opt/voicebox
  cd /opt/voicebox
  tar -xzf /tmp/voicebox-deploy.tar.gz
  rm /tmp/voicebox-deploy.tar.gz
  echo "文件已解压到 /opt/voicebox"
ENDSSH

# 4. 清理本地打包文件
rm voicebox-deploy.tar.gz

echo "=========================================="
echo "代码上传完成！"
echo "=========================================="
echo "下一步："
echo "1. ssh root@$SERVER"
echo "2. cd /opt/voicebox"
echo "3. 执行部署脚本"
echo "=========================================="
