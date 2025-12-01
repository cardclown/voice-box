#!/bin/bash

echo "=========================================="
echo "Voice 和 Emotion 页面快速测试"
echo "=========================================="
echo ""

# 测试后端连接
echo "1. 测试后端连接..."
BACKEND_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:10088/api/emotional-voice/profile/1)
if [ "$BACKEND_STATUS" = "200" ]; then
    echo "   ✓ 后端连接正常 (HTTP $BACKEND_STATUS)"
else
    echo "   ✗ 后端连接异常 (HTTP $BACKEND_STATUS)"
fi

# 测试前端连接
echo ""
echo "2. 测试前端连接..."
FRONTEND_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:5173/)
if [ "$FRONTEND_STATUS" = "200" ]; then
    echo "   ✓ 前端连接正常 (HTTP $FRONTEND_STATUS)"
else
    echo "   ✗ 前端连接异常 (HTTP $FRONTEND_STATUS)"
fi

# 测试情感语音 API
echo ""
echo "3. 测试情感语音 API..."
EMOTION_RESPONSE=$(curl -s http://localhost:10088/api/emotional-voice/profile/1)
echo "   响应: $EMOTION_RESPONSE"

# 测试数据库
echo ""
echo "4. 测试数据库数据..."
ssh root@129.211.180.183 "mysql -u voicebox -pvoicebox123 voicebox_db -e 'SELECT COUNT(*) as count FROM user_emotional_profiles;' 2>/dev/null" | tail -1

echo ""
echo "=========================================="
echo "测试完成！"
echo "=========================================="
echo ""
echo "请在浏览器中访问: http://localhost:5173"
echo "然后点击 Voice 和 Emotion 按钮测试页面"
echo ""
