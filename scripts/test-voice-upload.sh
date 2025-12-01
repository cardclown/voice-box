#!/bin/bash

echo "=========================================="
echo "语音上传测试脚本"
echo "=========================================="
echo ""

# 生成一个简单的WAV文件(1秒的静音)
# WAV文件头 + PCM数据
echo "1. 生成测试音频文件..."

# 创建临时目录
mkdir -p /tmp/voicebox-test

# 使用ffmpeg生成测试音频(如果有的话)
if command -v ffmpeg &> /dev/null; then
    echo "使用ffmpeg生成测试音频..."
    ffmpeg -f lavfi -i "sine=frequency=1000:duration=2" -ar 16000 -ac 1 -y /tmp/voicebox-test/test-audio.wav 2>/dev/null
    echo "✓ 测试音频已生成: /tmp/voicebox-test/test-audio.wav"
elif command -v sox &> /dev/null; then
    echo "使用sox生成测试音频..."
    sox -n -r 16000 -c 1 /tmp/voicebox-test/test-audio.wav synth 2 sine 1000 2>/dev/null
    echo "✓ 测试音频已生成: /tmp/voicebox-test/test-audio.wav"
else
    echo "⚠️  未找到ffmpeg或sox,生成简单的WAV文件..."
    # 生成一个最小的WAV文件头
    python3 << 'EOF'
import struct
import wave

# 创建一个简单的WAV文件
sample_rate = 16000
duration = 2  # 秒
num_samples = sample_rate * duration

with wave.open('/tmp/voicebox-test/test-audio.wav', 'w') as wav_file:
    wav_file.setnchannels(1)  # 单声道
    wav_file.setsampwidth(2)  # 16位
    wav_file.setframerate(sample_rate)
    
    # 生成简单的正弦波
    import math
    frequency = 1000  # 1kHz
    for i in range(num_samples):
        value = int(32767 * 0.3 * math.sin(2 * math.pi * frequency * i / sample_rate))
        wav_file.writeframes(struct.pack('<h', value))

print("✓ 测试音频已生成")
EOF
fi

echo ""
echo "2. 检查文件信息..."
if [ -f "/tmp/voicebox-test/test-audio.wav" ]; then
    FILE_SIZE=$(ls -lh /tmp/voicebox-test/test-audio.wav | awk '{print $5}')
    echo "  文件大小: $FILE_SIZE"
    
    if command -v file &> /dev/null; then
        FILE_TYPE=$(file /tmp/voicebox-test/test-audio.wav)
        echo "  文件类型: $FILE_TYPE"
    fi
else
    echo "✗ 测试音频文件生成失败"
    exit 1
fi

echo ""
echo "3. 上传到后端..."
echo "  目标: http://localhost:10088/api/voice/upload"
echo ""

# 发送请求(明确指定content-type)
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
  http://localhost:10088/api/voice/upload \
  -F "file=@/tmp/voicebox-test/test-audio.wav;type=audio/wav" \
  -F "userId=1" \
  -F "sessionId=1" \
  -F "language=zh-CN")

# 分离响应体和状态码
HTTP_BODY=$(echo "$RESPONSE" | head -n -1)
HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)

echo "HTTP状态码: $HTTP_CODE"
echo ""
echo "响应内容:"
echo "$HTTP_BODY" | python3 -m json.tool 2>/dev/null || echo "$HTTP_BODY"

echo ""
echo "=========================================="

# 检查结果
if [ "$HTTP_CODE" = "200" ]; then
    echo "✓ 测试成功!"
else
    echo "✗ 测试失败 (HTTP $HTTP_CODE)"
fi

echo ""
echo "查看后端日志以获取详细错误信息"
echo "=========================================="
