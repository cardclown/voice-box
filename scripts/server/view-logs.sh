#!/bin/bash
# 查看服务器日志

echo "查看 VoiceBox 后端日志..."
echo "按 Ctrl+C 退出"
echo ""

ssh voicebox-server "tail -f /var/log/voicebox/backend.log"
