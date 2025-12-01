#!/bin/bash

# 语音API测试脚本

set -e

BASE_URL="http://localhost:10088"
API_PREFIX="/api/voice"

echo "=========================================="
echo "VoiceBox 语音API测试"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试1: 检查服务是否运行
echo "测试1: 检查服务状态..."
if curl -s -f "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 服务运行正常${NC}"
else
    echo -e "${RED}✗ 服务未运行，请先启动服务${NC}"
    exit 1
fi
echo ""

# 测试2: 语音合成
echo "测试2: 语音合成..."
SYNTHESIS_RESPONSE=$(curl -s -X POST "${BASE_URL}${API_PREFIX}/synthesize" \
    -H "Content-Type: application/json" \
    -d '{
        "text": "你好，这是一个测试",
        "userId": 1,
        "language": "zh-CN",
        "voiceName": "zh_female_qingxin"
    }')

echo "响应: $SYNTHESIS_RESPONSE"

# 检查是否成功
if echo "$SYNTHESIS_RESPONSE" | grep -q '"success":true'; then
    echo -e "${GREEN}✓ 语音合成成功${NC}"
    
    # 提取fileId
    FILE_ID=$(echo "$SYNTHESIS_RESPONSE" | grep -o '"fileId":"[^"]*"' | cut -d'"' -f4)
    echo "文件ID: $FILE_ID"
    
    # 测试3: 获取音频文件
    if [ -n "$FILE_ID" ]; then
        echo ""
        echo "测试3: 获取音频文件..."
        
        OUTPUT_FILE="/tmp/test_voice_${FILE_ID}.mp3"
        
        if curl -s -f "${BASE_URL}${API_PREFIX}/audio/${FILE_ID}" -o "$OUTPUT_FILE"; then
            FILE_SIZE=$(stat -f%z "$OUTPUT_FILE" 2>/dev/null || stat -c%s "$OUTPUT_FILE" 2>/dev/null)
            
            if [ "$FILE_SIZE" -gt 0 ]; then
                echo -e "${GREEN}✓ 音频文件下载成功${NC}"
                echo "文件大小: $FILE_SIZE bytes"
                echo "文件位置: $OUTPUT_FILE"
                
                # 尝试播放（如果有播放器）
                if command -v afplay &> /dev/null; then
                    echo ""
                    echo "播放音频文件..."
                    afplay "$OUTPUT_FILE"
                elif command -v mpg123 &> /dev/null; then
                    echo ""
                    echo "播放音频文件..."
                    mpg123 "$OUTPUT_FILE"
                else
                    echo -e "${YELLOW}提示: 未找到音频播放器，请手动播放: $OUTPUT_FILE${NC}"
                fi
            else
                echo -e "${RED}✗ 音频文件为空${NC}"
            fi
        else
            echo -e "${RED}✗ 音频文件下载失败${NC}"
        fi
    fi
else
    echo -e "${RED}✗ 语音合成失败${NC}"
    echo "错误信息: $(echo "$SYNTHESIS_RESPONSE" | grep -o '"errorMessage":"[^"]*"' | cut -d'"' -f4)"
fi
echo ""

# 测试4: 语音上传（需要音频文件）
echo "测试4: 语音上传..."
if [ -f "$OUTPUT_FILE" ]; then
    UPLOAD_RESPONSE=$(curl -s -X POST "${BASE_URL}${API_PREFIX}/upload" \
        -F "file=@${OUTPUT_FILE}" \
        -F "userId=1" \
        -F "sessionId=1" \
        -F "language=zh-CN")
    
    echo "响应: $UPLOAD_RESPONSE"
    
    if echo "$UPLOAD_RESPONSE" | grep -q '"success":true'; then
        echo -e "${GREEN}✓ 语音上传成功${NC}"
        
        RECOGNIZED_TEXT=$(echo "$UPLOAD_RESPONSE" | grep -o '"recognizedText":"[^"]*"' | cut -d'"' -f4)
        echo "识别文本: $RECOGNIZED_TEXT"
    else
        echo -e "${RED}✗ 语音上传失败${NC}"
        echo "错误信息: $(echo "$UPLOAD_RESPONSE" | grep -o '"errorMessage":"[^"]*"' | cut -d'"' -f4)"
    fi
else
    echo -e "${YELLOW}⊘ 跳过（没有测试音频文件）${NC}"
fi
echo ""

echo "=========================================="
echo "测试完成"
echo "=========================================="
