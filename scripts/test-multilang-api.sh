#!/bin/bash

# 测试多语言支持API

SERVER="http://129.211.180.183:10088"
USER_ID=1

echo "=========================================="
echo "测试多语言支持API"
echo "=========================================="
echo ""

# 1. 测试获取支持的语言列表
echo "1. 获取支持的语言列表"
echo "GET $SERVER/api/emotional-voice/languages"
curl -s "$SERVER/api/emotional-voice/languages" | python3 -m json.tool
echo ""
echo ""

# 2. 测试获取用户语言偏好
echo "2. 获取用户语言偏好 (userId=$USER_ID)"
echo "GET $SERVER/api/emotional-voice/language/$USER_ID"
curl -s "$SERVER/api/emotional-voice/language/$USER_ID" | python3 -m json.tool
echo ""
echo ""

# 3. 测试设置用户语言为英文
echo "3. 设置用户语言为英文"
echo "POST $SERVER/api/emotional-voice/language/$USER_ID"
curl -s -X POST "$SERVER/api/emotional-voice/language/$USER_ID" \
  -H "Content-Type: application/json" \
  -d '{"language":"en-US"}' | python3 -m json.tool
echo ""
echo ""

# 4. 测试获取中文的模型配置
echo "4. 获取中文的模型配置"
echo "GET $SERVER/api/emotional-voice/language/zh-CN/model"
curl -s "$SERVER/api/emotional-voice/language/zh-CN/model" | python3 -m json.tool
echo ""
echo ""

# 5. 测试获取英文的音色配置
echo "5. 获取英文的音色配置"
echo "GET $SERVER/api/emotional-voice/language/en-US/voices"
curl -s "$SERVER/api/emotional-voice/language/en-US/voices" | python3 -m json.tool
echo ""
echo ""

# 6. 测试不支持的语言
echo "6. 测试不支持的语言 (应该返回错误)"
echo "GET $SERVER/api/emotional-voice/language/fr-FR/model"
curl -s "$SERVER/api/emotional-voice/language/fr-FR/model" | python3 -m json.tool
echo ""
echo ""

# 7. 恢复用户语言为中文
echo "7. 恢复用户语言为中文"
echo "POST $SERVER/api/emotional-voice/language/$USER_ID"
curl -s -X POST "$SERVER/api/emotional-voice/language/$USER_ID" \
  -H "Content-Type: application/json" \
  -d '{"language":"zh-CN"}' | python3 -m json.tool
echo ""

echo "=========================================="
echo "测试完成！"
echo "=========================================="
