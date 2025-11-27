#!/bin/bash

# VoiceBox 停止脚本
# 功能：停止所有运行中的服务

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_ROOT"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   VoiceBox 服务停止脚本${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

STOPPED_COUNT=0

# 停止后端服务
if [ -f "backend.pid" ]; then
    BACKEND_PID=$(cat backend.pid)
    if kill -0 "$BACKEND_PID" 2>/dev/null; then
        echo -e "${YELLOW}正在停止后端服务 (PID: $BACKEND_PID)...${NC}"
        kill "$BACKEND_PID" 2>/dev/null
        sleep 2
        # 如果还没停止，强制杀死
        if kill -0 "$BACKEND_PID" 2>/dev/null; then
            kill -9 "$BACKEND_PID" 2>/dev/null
        fi
        echo -e "${GREEN}✓ 后端服务已停止${NC}"
        STOPPED_COUNT=$((STOPPED_COUNT + 1))
    else
        echo -e "${YELLOW}后端服务未运行${NC}"
    fi
    rm -f backend.pid
else
    echo -e "${YELLOW}未找到后端 PID 文件${NC}"
fi

# 停止前端服务
if [ -f "frontend.pid" ]; then
    FRONTEND_PID=$(cat frontend.pid)
    if kill -0 "$FRONTEND_PID" 2>/dev/null; then
        echo -e "${YELLOW}正在停止前端服务 (PID: $FRONTEND_PID)...${NC}"
        kill "$FRONTEND_PID" 2>/dev/null
        sleep 2
        # 如果还没停止，强制杀死
        if kill -0 "$FRONTEND_PID" 2>/dev/null; then
            kill -9 "$FRONTEND_PID" 2>/dev/null
        fi
        echo -e "${GREEN}✓ 前端服务已停止${NC}"
        STOPPED_COUNT=$((STOPPED_COUNT + 1))
    else
        echo -e "${YELLOW}前端服务未运行${NC}"
    fi
    rm -f frontend.pid
else
    echo -e "${YELLOW}未找到前端 PID 文件${NC}"
fi

# 额外检查端口占用（清理残留进程）
echo ""
echo -e "${YELLOW}检查端口占用...${NC}"

if lsof -Pi :10088 -sTCP:LISTEN -t >/dev/null 2>&1 ; then
    echo -e "${YELLOW}发现端口 10088 仍被占用，正在清理...${NC}"
    lsof -ti:10088 | xargs kill -9 2>/dev/null || true
    echo -e "${GREEN}✓ 端口 10088 已清理${NC}"
fi

if lsof -Pi :5173 -sTCP:LISTEN -t >/dev/null 2>&1 ; then
    echo -e "${YELLOW}发现端口 5173 仍被占用，正在清理...${NC}"
    lsof -ti:5173 | xargs kill -9 2>/dev/null || true
    echo -e "${GREEN}✓ 端口 5173 已清理${NC}"
fi

echo ""
if [ $STOPPED_COUNT -gt 0 ]; then
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}   ✓ 已停止 $STOPPED_COUNT 个服务${NC}"
    echo -e "${GREEN}========================================${NC}"
else
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}   没有运行中的服务${NC}"
    echo -e "${BLUE}========================================${NC}"
fi
echo ""

