#!/bin/bash

# VoiceBox 重启脚本
# 功能：停止所有服务后重新启动

# 颜色定义
BLUE='\033[0;34m'
NC='\033[0m' # No Color

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_ROOT"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   VoiceBox 服务重启${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# 停止现有服务
./stop-all.sh

echo ""
echo -e "${BLUE}等待 3 秒后重新启动...${NC}"
sleep 3
echo ""

# 启动服务
./start-all.sh

