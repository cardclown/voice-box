#!/bin/bash

# 测试语音监控数据收集
# 通过模拟语音请求来生成监控数据

API_BASE="http://localhost:10088"

echo "=========================================="
echo "测试语音监控数据收集"
echo "=========================================="

# 测试 TTS 请求（不需要音频文件）
echo ""
echo "1. 测试 TTS 请求..."
for i in {1..5}; do
    echo "  发送 TTS 请求 $i/5..."
    curl -s -X POST "$API_BASE/api/voice/synthesize" \
        -H "Content-Type: application/json" \
        -d "{
            \"text\": \"这是第 $i 次测试\",
            \"userId\": 1,
            \"language\": \"zh-CN\",
            \"voiceType\": \"female\"
        }" > /dev/null
    
    if [ $? -eq 0 ]; then
        echo "  ✓ TTS 请求 $i 成功"
    else
        echo "  ✗ TTS 请求 $i 失败"
    fi
    
    sleep 0.5
done

echo ""
echo "2. 测试不同语言的 TTS 请求..."
languages=("zh-CN" "en-US" "ja-JP" "ko-KR")
for lang in "${languages[@]}"; do
    echo "  发送 $lang TTS 请求..."
    curl -s -X POST "$API_BASE/api/voice/synthesize" \
        -H "Content-Type: application/json" \
        -d "{
            \"text\": \"Hello World\",
            \"userId\": 2,
            \"language\": \"$lang\",
            \"voiceType\": \"female\"
        }" > /dev/null
    
    if [ $? -eq 0 ]; then
        echo "  ✓ $lang TTS 请求成功"
    else
        echo "  ✗ $lang TTS 请求失败"
    fi
    
    sleep 0.5
done

echo ""
echo "3. 测试多用户请求..."
for userId in {1..3}; do
    echo "  用户 $userId 发送请求..."
    curl -s -X POST "$API_BASE/api/voice/synthesize" \
        -H "Content-Type: application/json" \
        -d "{
            \"text\": \"用户 $userId 的测试\",
            \"userId\": $userId,
            \"language\": \"zh-CN\",
            \"voiceType\": \"female\"
        }" > /dev/null
    
    if [ $? -eq 0 ]; then
        echo "  ✓ 用户 $userId 请求成功"
    else
        echo "  ✗ 用户 $userId 请求失败"
    fi
    
    sleep 0.5
done

echo ""
echo "=========================================="
echo "4. 查看监控数据"
echo "=========================================="

echo ""
echo "健康状态:"
curl -s "$API_BASE/api/voice/monitoring/health" | python3 -m json.tool

echo ""
echo "整体指标:"
curl -s "$API_BASE/api/voice/monitoring/metrics/overall" | python3 -m json.tool

echo ""
echo "TTS 指标:"
curl -s "$API_BASE/api/voice/monitoring/metrics/tts" | python3 -m json.tool

echo ""
echo "用户指标:"
curl -s "$API_BASE/api/voice/monitoring/metrics/users" | python3 -m json.tool

echo ""
echo "语言分布:"
curl -s "$API_BASE/api/voice/monitoring/report" | python3 -c "import sys, json; data=json.load(sys.stdin); print(json.dumps(data['languages'], indent=2))"

echo ""
echo "=========================================="
echo "测试完成！"
echo "=========================================="
echo ""
echo "提示："
echo "- 访问 http://localhost:5173/voice-interaction 查看前端监控数据"
echo "- 访问 test-monitoring-api.html 查看详细的 API 测试"
echo "- 监控数据会随着使用逐渐累积"
