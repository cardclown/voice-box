#!/bin/bash

echo "=========================================="
echo "语音服务诊断工具"
echo "=========================================="
echo ""

# 1. 检查配置
echo "1. 检查配置文件..."
if [ -f "config.properties" ]; then
    echo "✓ config.properties 存在"
    echo ""
    echo "豆包语音配置:"
    grep "voicebox.doubao.voice" config.properties | while read line; do
        key=$(echo "$line" | cut -d'=' -f1)
        value=$(echo "$line" | cut -d'=' -f2)
        if [[ "$key" == *"token"* ]] || [[ "$key" == *"secret"* ]]; then
            echo "  $key=***隐藏***"
        else
            echo "  $line"
        fi
    done
else
    echo "✗ config.properties 不存在"
fi

echo ""
echo "=========================================="

# 2. 检查后端服务
echo "2. 检查后端服务状态..."
if lsof -ti:10088 > /dev/null 2>&1; then
    echo "✓ 后端服务正在运行 (端口 10088)"
    PID=$(lsof -ti:10088)
    echo "  进程ID: $PID"
else
    echo "✗ 后端服务未运行"
fi

echo ""
echo "=========================================="

# 3. 测试网络连接
echo "3. 测试豆包API网络连接..."
echo "  测试 openspeech.bytedance.com..."
if ping -c 2 openspeech.bytedance.com > /dev/null 2>&1; then
    echo "  ✓ 网络连接正常"
else
    echo "  ✗ 无法连接到豆包服务器"
    echo "  可能原因:"
    echo "    - 网络问题"
    echo "    - 防火墙阻止"
    echo "    - DNS解析失败"
fi

echo ""
echo "=========================================="

# 4. 检查最近的错误日志
echo "4. 检查最近的错误日志..."
if [ -f "app-device/target/spring-boot.log" ]; then
    echo "  最近的ERROR日志:"
    tail -100 app-device/target/spring-boot.log | grep "ERROR" | tail -5
elif [ -f "backend-dev.log" ]; then
    echo "  最近的ERROR日志:"
    tail -100 backend-dev.log | grep "ERROR" | tail -5
else
    echo "  未找到日志文件"
fi

echo ""
echo "=========================================="

# 5. 建议
echo "5. 问题排查建议:"
echo ""
echo "常见问题:"
echo "  1. 豆包API凭证错误"
echo "     - 检查 appid, token, secret 是否正确"
echo "     - 确认凭证未过期"
echo ""
echo "  2. 网络连接问题"
echo "     - 检查是否能访问 openspeech.bytedance.com"
echo "     - 检查防火墙设置"
echo "     - 尝试使用VPN"
echo ""
echo "  3. 音频格式问题"
echo "     - 确认音频格式为 WAV/PCM"
echo "     - 检查采样率 (推荐 16000Hz)"
echo "     - 检查音频时长 (不要太短)"
echo ""
echo "  4. WebSocket连接问题"
echo "     - 检查是否支持 WSS 协议"
echo "     - 查看详细错误日志"
echo ""
echo "解决方案:"
echo "  1. 启用Mock服务进行测试:"
echo "     在 config.properties 中添加:"
echo "     voicebox.voice.use.mock=true"
echo ""
echo "  2. 查看详细日志:"
echo "     tail -f app-device/target/spring-boot.log | grep -E '(ERROR|WARN|DoubaoVoiceService)'"
echo ""
echo "  3. 测试API连接:"
echo "     curl -v https://openspeech.bytedance.com"
echo ""
echo "=========================================="
