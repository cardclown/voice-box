#!/bin/bash

echo "=========================================="
echo "测试情感语音模块 API"
echo "=========================================="

# 测试用户画像查询
echo ""
echo "1. 测试用户画像查询 API..."
PROFILE_RESPONSE=$(curl -s http://localhost:10088/api/emotional-voice/profile/1)
echo "响应: $PROFILE_RESPONSE"

# 测试健康检查
echo ""
echo "2. 测试健康检查..."
HEALTH_RESPONSE=$(curl -s http://localhost:10088/api/voice/monitoring/health)
echo "响应: $HEALTH_RESPONSE"

# 测试监控报告
echo ""
echo "3. 测试监控报告..."
REPORT_RESPONSE=$(curl -s http://localhost:10088/api/voice/monitoring/report)
echo "响应: $REPORT_RESPONSE"

echo ""
echo "=========================================="
echo "测试完成"
echo "=========================================="
