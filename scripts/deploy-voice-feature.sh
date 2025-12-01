#!/bin/bash

# 语音功能部署脚本

set -e

SERVER="root@129.211.180.183"
REMOTE_DIR="/opt/voicebox"

echo "=========================================="
echo "部署语音交互功能到服务器"
echo "=========================================="

# 1. 打包所有语音相关文件
echo "1. 打包语音相关文件..."
tar -czf voice-feature-complete.tar.gz \
  app-device/src/main/java/com/example/voicebox/app/device/controller/VoiceController.java \
  app-device/src/main/java/com/example/voicebox/app/device/domain/VoiceMessage.java \
  app-device/src/main/java/com/example/voicebox/app/device/domain/VoiceServiceLog.java \
  app-device/src/main/java/com/example/voicebox/app/device/repository/VoiceMessageRepository.java \
  app-device/src/main/java/com/example/voicebox/app/device/repository/VoiceServiceLogRepository.java \
  app-device/src/main/java/com/example/voicebox/app/device/service/voice/

echo "✓ 打包完成"

# 2. 上传到服务器
echo "2. 上传到服务器..."
scp voice-feature-complete.tar.gz $SERVER:/tmp/

echo "✓ 上传完成"

# 3. 在服务器上解压
echo "3. 在服务器上解压..."
ssh $SERVER << 'ENDSSH'
cd /opt/voicebox
tar -xzf /tmp/voice-feature-complete.tar.gz
echo "✓ 解压完成"
ENDSSH

# 4. 编译
echo "4. 编译项目..."
ssh $SERVER << 'ENDSSH'
cd /opt/voicebox
mvn clean install -DskipTests
if [ $? -eq 0 ]; then
    echo "✓ 编译成功"
else
    echo "✗ 编译失败"
    exit 1
fi
ENDSSH

# 5. 重启服务
echo "5. 重启后端服务..."
ssh $SERVER 'systemctl restart voicebox-backend'
sleep 10

# 6. 检查服务状态
echo "6. 检查服务状态..."
ssh $SERVER 'systemctl status voicebox-backend --no-pager | head -20'

# 7. 测试API
echo "7. 测试语音API..."
ssh $SERVER 'curl -s http://localhost:10088/actuator/health'
echo ""

echo "=========================================="
echo "部署完成！"
echo "=========================================="

# 清理
rm voice-feature-complete.tar.gz

echo "现在可以测试语音API了："
echo "curl -X POST http://129.211.180.183:10088/api/voice/synthesize \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{\"text\":\"你好\",\"userId\":1}'"
