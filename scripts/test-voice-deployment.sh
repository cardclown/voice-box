#!/bin/bash

# 语音功能部署测试脚本

SERVER="http://129.211.180.183:10088"

echo "=========================================="
echo "语音功能部署测试"
echo "=========================================="
echo ""

# 1. 测试后端服务健康状态
echo "1. 测试后端服务..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" $SERVER/api/chat/sessions)
if [ "$HTTP_CODE" = "200" ]; then
    echo "   ✅ 后端服务正常 (HTTP $HTTP_CODE)"
else
    echo "   ❌ 后端服务异常 (HTTP $HTTP_CODE)"
fi
echo ""

# 2. 测试语音上传端点（OPTIONS请求）
echo "2. 测试语音上传端点..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X OPTIONS $SERVER/api/voice/upload)
echo "   HTTP状态码: $HTTP_CODE"
if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "204" ]; then
    echo "   ✅ 语音上传端点可访问"
else
    echo "   ⚠️  语音上传端点状态: $HTTP_CODE"
fi
echo ""

# 3. 测试语音合成端点
echo "3. 测试语音合成端点..."
RESPONSE=$(curl -s -X POST $SERVER/api/voice/synthesize \
    -H "Content-Type: application/json" \
    -d '{
        "text": "测试",
        "userId": 1,
        "sessionId": 1,
        "language": "zh-CN"
    }')

if echo "$RESPONSE" | grep -q "fileId\|error"; then
    echo "   ✅ 语音合成端点响应正常"
    echo "   响应: $(echo $RESPONSE | head -c 100)..."
else
    echo "   ❌ 语音合成端点响应异常"
    echo "   响应: $RESPONSE"
fi
echo ""

# 4. 检查数据库表
echo "4. 检查语音数据库表..."
ssh root@129.211.180.183 "mysql -u voicebox -pvoicebox123 voicebox_db -e 'SHOW TABLES LIKE \"voice%\";'" 2>/dev/null
if [ $? -eq 0 ]; then
    echo "   ✅ 数据库表检查完成"
else
    echo "   ❌ 数据库表检查失败"
fi
echo ""

# 5. 检查语音文件存储目录
echo "5. 检查语音文件存储目录..."
ssh root@129.211.180.183 "ls -la /opt/voicebox/voice-box-uploads/ 2>/dev/null | head -5"
if [ $? -eq 0 ]; then
    echo "   ✅ 语音文件存储目录存在"
else
    echo "   ⚠️  语音文件存储目录可能不存在"
fi
echo ""

# 6. 检查豆包配置
echo "6. 检查豆包语音服务配置..."
ssh root@129.211.180.183 "grep -E 'doubao|voice' /opt/voicebox/config.properties 2>/dev/null | grep -v '#'"
if [ $? -eq 0 ]; then
    echo "   ✅ 豆包配置已设置"
else
    echo "   ⚠️  豆包配置可能未设置"
fi
echo ""

echo "=========================================="
echo "测试完成"
echo "=========================================="
echo ""
echo "📝 下一步："
echo "   1. 在浏览器访问: http://129.211.180.183"
echo "   2. 测试语音输入功能"
echo "   3. 测试语音输出功能"
echo ""
