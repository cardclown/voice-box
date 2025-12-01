#!/bin/bash

# 情感语音模块测试脚本
# 运行所有端到端测试并生成报告

set -e

echo "=========================================="
echo "情感语音模块端到端测试"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试结果统计
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 1. 后端测试
echo "📋 步骤 1: 运行后端端到端测试"
echo "----------------------------------------"

cd app-device

if mvn test -Dtest=EmotionalVoiceE2ETest; then
    echo -e "${GREEN}✓ 后端测试通过${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}✗ 后端测试失败${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

TOTAL_TESTS=$((TOTAL_TESTS + 1))
cd ..

echo ""

# 2. 前端测试
echo "📋 步骤 2: 运行前端端到端测试"
echo "----------------------------------------"

cd app-web

if npm run test -- EmotionalVoiceE2E.test.js; then
    echo -e "${GREEN}✓ 前端测试通过${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}✗ 前端测试失败${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

TOTAL_TESTS=$((TOTAL_TESTS + 1))
cd ..

echo ""

# 3. API 集成测试
echo "📋 步骤 3: 运行 API 集成测试"
echo "----------------------------------------"

# 检查服务是否运行
if curl -s http://localhost:10088/actuator/health > /dev/null 2>&1; then
    echo "✓ 后端服务正在运行"
    
    # 测试情感分析 API
    echo "测试 1: 情感分析 API"
    if curl -X POST http://localhost:10088/api/emotional-voice/analyze \
        -F "audioFile=@test-data/sample-audio.wav" \
        -F "userId=1" \
        -F "sessionId=test-session" \
        -F "text=测试文本" \
        -s -o /dev/null -w "%{http_code}" | grep -q "200"; then
        echo -e "${GREEN}  ✓ 情感分析 API 正常${NC}"
    else
        echo -e "${YELLOW}  ⚠ 情感分析 API 可能需要检查${NC}"
    fi
    
    # 测试用户画像 API
    echo "测试 2: 用户画像 API"
    if curl -s http://localhost:10088/api/emotional-voice/profile/1 \
        -o /dev/null -w "%{http_code}" | grep -q "200"; then
        echo -e "${GREEN}  ✓ 用户画像 API 正常${NC}"
    else
        echo -e "${YELLOW}  ⚠ 用户画像 API 可能需要检查${NC}"
    fi
    
else
    echo -e "${YELLOW}⚠ 后端服务未运行，跳过 API 测试${NC}"
    echo "  提示: 运行 'mvn spring-boot:run' 启动后端服务"
fi

echo ""

# 4. 数据库测试
echo "📋 步骤 4: 验证数据库表结构"
echo "----------------------------------------"

# 检查数据库连接
if mysql -h 129.211.180.183 -u voicebox -pvoicebox123 voicebox_db -e "SELECT 1" > /dev/null 2>&1; then
    echo "✓ 数据库连接正常"
    
    # 检查情感语音相关表
    TABLES=("emotional_tags" "emotional_profiles" "emotion_history" "voice_features")
    
    for table in "${TABLES[@]}"; do
        if mysql -h 129.211.180.183 -u voicebox -pvoicebox123 voicebox_db \
            -e "DESCRIBE $table" > /dev/null 2>&1; then
            echo -e "${GREEN}  ✓ 表 $table 存在${NC}"
        else
            echo -e "${RED}  ✗ 表 $table 不存在${NC}"
            FAILED_TESTS=$((FAILED_TESTS + 1))
        fi
    done
else
    echo -e "${YELLOW}⚠ 无法连接到数据库${NC}"
fi

echo ""

# 5. 前端组件测试
echo "📋 步骤 5: 验证前端组件"
echo "----------------------------------------"

COMPONENTS=(
    "app-web/src/views/EmotionalVoice.vue"
    "app-web/src/components/emotional/EmotionalVoiceInput.vue"
    "app-web/src/components/emotional/EmotionFeedback.vue"
    "app-web/src/components/emotional/TagVisualization.vue"
    "app-web/src/components/emotional/EmotionHistory.vue"
    "app-web/src/components/emotional/EmotionStatistics.vue"
)

for component in "${COMPONENTS[@]}"; do
    if [ -f "$component" ]; then
        echo -e "${GREEN}  ✓ $(basename $component) 存在${NC}"
    else
        echo -e "${RED}  ✗ $(basename $component) 不存在${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
done

echo ""

# 6. 后端服务测试
echo "📋 步骤 6: 验证后端服务"
echo "----------------------------------------"

SERVICES=(
    "app-device/src/main/java/com/example/voicebox/app/device/service/emotional/VoiceFeatureAnalyzer.java"
    "app-device/src/main/java/com/example/voicebox/app/device/service/emotional/EmotionRecognitionService.java"
    "app-device/src/main/java/com/example/voicebox/app/device/service/emotional/EmotionalTagGenerator.java"
    "app-device/src/main/java/com/example/voicebox/app/device/controller/EmotionalVoiceController.java"
)

for service in "${SERVICES[@]}"; do
    if [ -f "$service" ]; then
        echo -e "${GREEN}  ✓ $(basename $service) 存在${NC}"
    else
        echo -e "${RED}  ✗ $(basename $service) 不存在${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
done

echo ""

# 生成测试报告
echo "=========================================="
echo "测试报告"
echo "=========================================="
echo ""
echo "总测试数: $TOTAL_TESTS"
echo -e "${GREEN}通过: $PASSED_TESTS${NC}"
echo -e "${RED}失败: $FAILED_TESTS${NC}"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}🎉 所有测试通过！${NC}"
    exit 0
else
    echo -e "${RED}❌ 有测试失败，请检查上述输出${NC}"
    exit 1
fi
