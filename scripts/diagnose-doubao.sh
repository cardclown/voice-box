#!/bin/bash

# 豆包语音服务诊断脚本

echo "=========================================="
echo "豆包语音服务诊断"
echo "=========================================="
echo ""

# 从配置文件读取参数
APPID=$(ssh root@129.211.180.183 "grep 'voicebox.doubao.voice.appid' /opt/voicebox/config.properties | cut -d'=' -f2")
TOKEN=$(ssh root@129.211.180.183 "grep 'voicebox.doubao.voice.token' /opt/voicebox/config.properties | cut -d'=' -f2")
TTS_URL=$(ssh root@129.211.180.183 "grep 'voicebox.doubao.voice.url' /opt/voicebox/config.properties | cut -d'=' -f2")
STT_URL=$(ssh root@129.211.180.183 "grep 'voicebox.doubao.voice.stt.url' /opt/voicebox/config.properties | cut -d'=' -f2")

echo "配置信息："
echo "  AppID: $APPID"
echo "  Token: ${TOKEN:0:10}..."
echo "  TTS URL: $TTS_URL"
echo "  STT URL: $STT_URL"
echo ""

# 测试1: 检查域名解析
echo "1. 测试域名解析..."
HOST=$(echo $TTS_URL | sed 's|wss://||' | sed 's|/.*||')
echo "   域名: $HOST"
nslookup $HOST 2>&1 | grep -A 2 "Name:" || echo "   ❌ 域名解析失败"
echo ""

# 测试2: 测试端口连接
echo "2. 测试端口连接..."
nc -zv -w 5 $HOST 443 2>&1 | grep -q "succeeded" && echo "   ✅ 端口443可达" || echo "   ❌ 端口443不可达"
echo ""

# 测试3: 测试HTTP连接
echo "3. 测试HTTP连接..."
HTTP_URL=$(echo $TTS_URL | sed 's|wss://|https://|')
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -m 5 "$HTTP_URL" 2>/dev/null)
echo "   HTTP状态码: $HTTP_CODE"
if [ "$HTTP_CODE" = "404" ]; then
    echo "   ⚠️  API端点可能已变更"
elif [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
    echo "   ⚠️  认证失败，可能需要更新凭证"
elif [ "$HTTP_CODE" = "000" ]; then
    echo "   ❌ 无法连接到服务器"
else
    echo "   状态: $HTTP_CODE"
fi
echo ""

# 测试4: 检查服务器日志中的错误
echo "4. 检查最近的错误日志..."
ssh root@129.211.180.183 "tail -50 /opt/voicebox/logs/app.log | grep -i 'websocket\|doubao\|tts\|stt' | tail -10"
echo ""

echo "=========================================="
echo "诊断建议"
echo "=========================================="
echo ""

if [ "$HTTP_CODE" = "404" ]; then
    echo "🔍 问题分析："
    echo "   WebSocket端点返回404，可能的原因："
    echo "   1. API地址已过期或变更"
    echo "   2. 需要使用不同的API版本"
    echo "   3. 豆包服务已升级，需要新的接入方式"
    echo ""
    echo "💡 建议解决方案："
    echo "   1. 查阅豆包最新API文档"
    echo "   2. 联系豆包技术支持获取正确的API地址"
    echo "   3. 考虑使用豆包的HTTP API而非WebSocket"
    echo "   4. 评估其他语音服务提供商（阿里云、腾讯云、讯飞）"
fi

if [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
    echo "🔍 问题分析："
    echo "   认证失败，可能的原因："
    echo "   1. AppID、Token或Secret不正确"
    echo "   2. 签名算法有误"
    echo "   3. 时间戳问题"
    echo ""
    echo "💡 建议解决方案："
    echo "   1. 验证AppID和Token是否有效"
    echo "   2. 检查签名生成算法"
    echo "   3. 确认服务器时间是否正确"
fi

echo ""
echo "📚 参考资源："
echo "   • 豆包开放平台: https://www.volcengine.com/docs/6561/79817"
echo "   • 火山引擎语音服务: https://www.volcengine.com/product/speech"
echo ""
