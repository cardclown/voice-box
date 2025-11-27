#!/bin/bash

# VoiceBox 状态检查脚本
# 功能：查看所有服务的运行状态

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_ROOT"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   VoiceBox 服务状态${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# 检查后端服务
echo -e "${BLUE}后端服务 (端口 10088):${NC}"
if [ -f "backend.pid" ]; then
    BACKEND_PID=$(cat backend.pid)
    if kill -0 "$BACKEND_PID" 2>/dev/null; then
        echo -e "  状态: ${GREEN}运行中${NC}"
        echo -e "  PID: ${YELLOW}$BACKEND_PID${NC}"
        if curl -s http://localhost:10088/api/chat/sessions > /dev/null 2>&1; then
            echo -e "  健康检查: ${GREEN}✓ 正常${NC}"
        else
            echo -e "  健康检查: ${RED}✗ 异常${NC}"
        fi
    else
        echo -e "  状态: ${RED}已停止${NC}"
        echo -e "  PID 文件存在但进程不存在"
    fi
else
    if lsof -Pi :10088 -sTCP:LISTEN -t >/dev/null 2>&1 ; then
        BACKEND_PID=$(lsof -ti:10088)
        echo -e "  状态: ${YELLOW}运行中 (未通过脚本启动)${NC}"
        echo -e "  PID: ${YELLOW}$BACKEND_PID${NC}"
    else
        echo -e "  状态: ${RED}未运行${NC}"
    fi
fi
echo ""

# 检查前端服务
echo -e "${BLUE}前端服务 (端口 5173):${NC}"
if [ -f "frontend.pid" ]; then
    FRONTEND_PID=$(cat frontend.pid)
    if kill -0 "$FRONTEND_PID" 2>/dev/null; then
        echo -e "  状态: ${GREEN}运行中${NC}"
        echo -e "  PID: ${YELLOW}$FRONTEND_PID${NC}"
        if curl -s http://localhost:5173 > /dev/null 2>&1; then
            echo -e "  健康检查: ${GREEN}✓ 正常${NC}"
        else
            echo -e "  健康检查: ${RED}✗ 异常${NC}"
        fi
    else
        echo -e "  状态: ${RED}已停止${NC}"
        echo -e "  PID 文件存在但进程不存在"
    fi
else
    if lsof -Pi :5173 -sTCP:LISTEN -t >/dev/null 2>&1 ; then
        FRONTEND_PID=$(lsof -ti:5173)
        echo -e "  状态: ${YELLOW}运行中 (未通过脚本启动)${NC}"
        echo -e "  PID: ${YELLOW}$FRONTEND_PID${NC}"
    else
        echo -e "  状态: ${RED}未运行${NC}"
    fi
fi
echo ""

# 服务地址
echo -e "${BLUE}服务地址:${NC}"
echo -e "  • 前端: ${GREEN}http://localhost:5173${NC}"
echo -e "  • 后端: ${GREEN}http://localhost:10088${NC}"
echo ""

# 日志文件
echo -e "${BLUE}日志文件:${NC}"
if [ -f "backend.log" ]; then
    BACKEND_LOG_SIZE=$(du -h backend.log | cut -f1)
    echo -e "  • backend.log (${YELLOW}$BACKEND_LOG_SIZE${NC})"
else
    echo -e "  • backend.log ${RED}(不存在)${NC}"
fi

if [ -f "frontend.log" ]; then
    FRONTEND_LOG_SIZE=$(du -h frontend.log | cut -f1)
    echo -e "  • frontend.log (${YELLOW}$FRONTEND_LOG_SIZE${NC})"
else
    echo -e "  • frontend.log ${RED}(不存在)${NC}"
fi
echo ""

echo -e "${BLUE}========================================${NC}"
echo ""

