#!/bin/bash

# 部署多语言支持功能
# 只部署新增的多语言相关文件

set -e

SERVER="root@129.211.180.183"
REMOTE_DIR="/opt/voicebox"

echo "=========================================="
echo "部署多语言支持功能"
echo "=========================================="

# 1. 上传新增的Java文件
echo "上传多语言服务文件..."
scp app-device/src/main/java/com/example/voicebox/app/device/service/emotional/MultiLanguageService.java \
    $SERVER:$REMOTE_DIR/app-device/src/main/java/com/example/voicebox/app/device/service/emotional/

scp app-device/src/main/java/com/example/voicebox/app/device/service/emotional/UserLanguagePreferenceService.java \
    $SERVER:$REMOTE_DIR/app-device/src/main/java/com/example/voicebox/app/device/service/emotional/

# 2. 上传更新的文件
echo "上传更新的文件..."
scp app-device/src/main/java/com/example/voicebox/app/device/controller/EmotionalVoiceController.java \
    $SERVER:$REMOTE_DIR/app-device/src/main/java/com/example/voicebox/app/device/controller/

scp app-device/src/main/java/com/example/voicebox/app/device/domain/UserEmotionalProfile.java \
    $SERVER:$REMOTE_DIR/app-device/src/main/java/com/example/voicebox/app/device/domain/

# 3. 上传前端文件
echo "上传前端文件..."
scp app-web/src/components/emotional/LanguageSelector.vue \
    $SERVER:$REMOTE_DIR/app-web/src/components/emotional/

scp app-web/src/i18n/emotional-voice.js \
    $SERVER:$REMOTE_DIR/app-web/src/i18n/

scp app-web/src/services/emotionalVoiceService.js \
    $SERVER:$REMOTE_DIR/app-web/src/services/

scp app-web/src/views/EmotionalVoice.vue \
    $SERVER:$REMOTE_DIR/app-web/src/views/

# 4. 在服务器上重新编译
echo "在服务器上重新编译后端..."
ssh $SERVER << 'ENDSSH'
cd /opt/voicebox/app-device
mvn clean compile -DskipTests
ENDSSH

# 5. 重启服务
echo "重启后端服务..."
ssh $SERVER << 'ENDSSH'
cd /opt/voicebox
./stop-all.sh
sleep 3
./start-all.sh
ENDSSH

echo "=========================================="
echo "多语言支持功能部署完成！"
echo "=========================================="
echo ""
echo "新增API端点："
echo "  GET  /api/emotional-voice/languages"
echo "  GET  /api/emotional-voice/language/{userId}"
echo "  POST /api/emotional-voice/language/{userId}"
echo "  GET  /api/emotional-voice/language/{language}/model"
echo "  GET  /api/emotional-voice/language/{language}/voices"
echo ""
echo "测试命令："
echo "  curl http://129.211.180.183:10088/api/emotional-voice/languages"
