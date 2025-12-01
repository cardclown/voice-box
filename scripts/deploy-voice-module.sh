#!/bin/bash

set -e

SERVER="root@129.211.180.183"
REMOTE_DIR="/opt/voicebox"

echo "=========================================="
echo "部署语音交互模块"
echo "=========================================="

# 1. 编译前端
echo "编译前端..."
cd app-web
npm run build
cd ..

# 2. 上传前端文件
echo "上传前端文件..."
scp app-web/src/views/VoiceInteraction.vue $SERVER:$REMOTE_DIR/app-web/src/views/
scp app-web/src/App.vue $SERVER:$REMOTE_DIR/app-web/src/
scp app-web/src/components/layout/ModuleNav.vue $SERVER:$REMOTE_DIR/app-web/src/components/layout/
scp app-web/src/services/emotionalVoiceService.js $SERVER:$REMOTE_DIR/app-web/src/services/

# 3. 上传构建文件
echo "上传构建文件..."
scp -r app-web/dist/* $SERVER:$REMOTE_DIR/app-web/dist/

# 4. 重启前端服务
echo "重启前端服务..."
ssh $SERVER << 'ENDSSH'
cd /opt/voicebox
# 重新构建前端
cd app-web
npm run build
cd ..

# 重启服务
./restart-all.sh

echo "等待服务启动..."
sleep 5

# 检查服务状态
./status.sh
ENDSSH

echo "=========================================="
echo "语音交互模块部署完成！"
echo "=========================================="
echo ""
echo "访问地址："
echo "  http://129.211.180.183:10087/"
echo ""
echo "新增模块："
echo "  Voice - 语音交互模块"
echo ""
echo "功能包括："
echo "  - 语音输入和识别"
echo "  - 语音播放和控制"
echo "  - 流式语音播放"
echo "  - 语音设置和配置"
echo "  - 实时监控和指标"
