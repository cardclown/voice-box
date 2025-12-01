#!/bin/bash
# 重启服务器上的服务

echo "重启 VoiceBox 服务..."

ssh voicebox-server << 'ENDSSH'
echo "停止服务..."
systemctl stop voicebox-backend

echo "启动服务..."
systemctl start voicebox-backend

echo "等待服务启动..."
sleep 3

echo ""
echo "服务状态:"
systemctl status voicebox-backend --no-pager | head -10

echo ""
echo "最新日志:"
tail -10 /var/log/voicebox/backend.log
ENDSSH

echo ""
echo "✓ 服务重启完成"
