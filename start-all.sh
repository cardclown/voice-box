#!/bin/bash

# VoiceBox 一键启动脚本
# 功能：按正确顺序启动后端和前端服务
# 用法：./start-all.sh [dev|prod]

# 注意：不使用 set -e，因为我们需要手动处理错误

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_ROOT"

# 环境参数（默认为开发环境）
ENV=${1:-dev}

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   VoiceBox 项目启动脚本${NC}"
echo -e "${BLUE}   环境: ${ENV}${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# 根据环境选择配置文件
if [ "$ENV" = "prod" ]; then
    CONFIG_FILE="config-prod.properties"
elif [ "$ENV" = "dev" ]; then
    CONFIG_FILE="config-dev.properties"
else
    echo -e "${RED}❌ 错误: 未知环境 '$ENV'${NC}"
    echo -e "${YELLOW}用法: ./start-all.sh [dev|prod]${NC}"
    exit 1
fi

# 检查配置文件是否存在
if [ ! -f "$CONFIG_FILE" ]; then
    echo -e "${RED}❌ 错误: 配置文件 $CONFIG_FILE 不存在${NC}"
    echo -e "${YELLOW}请从模板创建配置文件:${NC}"
    echo -e "  cp env-example.properties $CONFIG_FILE"
    echo -e "  然后编辑 $CONFIG_FILE 填入正确的配置"
    exit 1
fi

echo -e "${GREEN}✓ 使用配置文件: $CONFIG_FILE${NC}"
echo ""

# 检查 Maven（优先使用指定路径的 Maven）
MAVEN_CMD="/Users/jd/mydesk/maven/bin/mvn"
if [ -f "$MAVEN_CMD" ]; then
    echo -e "${GREEN}✓ 使用 Maven: $MAVEN_CMD${NC}"
elif command -v mvn &> /dev/null; then
    MAVEN_CMD="mvn"
    echo -e "${GREEN}✓ 使用系统 Maven${NC}"
else
    echo -e "${RED}❌ 错误: 未找到 Maven${NC}"
    echo -e "${YELLOW}请设置 Maven 路径或安装 Maven${NC}"
    exit 1
fi

# 检查 Node.js
if ! command -v node &> /dev/null; then
    echo -e "${RED}❌ 错误: 未找到 Node.js${NC}"
    echo -e "${YELLOW}请安装 Node.js${NC}"
    exit 1
fi

# 检查 npm
if ! command -v npm &> /dev/null; then
    echo -e "${RED}❌ 错误: 未找到 npm${NC}"
    echo -e "${YELLOW}请安装 npm${NC}"
    exit 1
fi

echo ""

# ==================== 步骤 1: 编译后端 ====================
echo -e "${YELLOW}[1/4] 正在编译后端项目...${NC}"

# 创建临时文件存储编译输出
COMPILE_LOG=$(mktemp)
$MAVEN_CMD clean install -DskipTests > "$COMPILE_LOG" 2>&1 &
COMPILE_PID=$!

# 显示进度
while kill -0 $COMPILE_PID 2>/dev/null; do
    echo -n "."
    sleep 1
done

# 等待编译完成并获取退出状态
wait $COMPILE_PID
COMPILE_STATUS=$?

if [ $COMPILE_STATUS -ne 0 ]; then
    echo -e "\n${RED}❌ 后端编译失败${NC}"
    echo -e "${YELLOW}错误信息:${NC}"
    tail -n 20 "$COMPILE_LOG"
    echo ""
    echo -e "${YELLOW}完整日志已保存到: $COMPILE_LOG${NC}"
    echo -e "${YELLOW}或运行以下命令查看详细错误:${NC}"
    echo -e "  mvn clean install -DskipTests"
    exit 1
fi

echo -e "\n${GREEN}✓ 后端编译成功${NC}"
rm -f "$COMPILE_LOG"
echo ""

# ==================== 步骤 2: 安装前端依赖 ====================
echo -e "${YELLOW}[2/4] 正在检查前端依赖...${NC}"
cd "$PROJECT_ROOT/app-web"

if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}首次运行，正在安装前端依赖...${NC}"
    npm install
    echo -e "${GREEN}✓ 前端依赖安装完成${NC}"
else
    echo -e "${GREEN}✓ 前端依赖已存在${NC}"
fi
echo ""

# ==================== 步骤 3: 启动后端服务 ====================
echo -e "${YELLOW}[3/4] 正在启动后端服务...${NC}"
cd "$PROJECT_ROOT"

# 检查端口 10088 是否被占用
if lsof -Pi :10088 -sTCP:LISTEN -t >/dev/null 2>&1 ; then
    echo -e "${YELLOW}⚠️  端口 10088 已被占用，正在尝试关闭旧进程...${NC}"
    lsof -ti:10088 | xargs kill -9 2>/dev/null || true
    sleep 2
fi

# 启动后端（后台运行）
# 读取配置文件并设置 JVM 参数
echo -e "${GREEN}✓ 正在加载配置文件: $CONFIG_FILE${NC}"
CONFIG_ARGS=""
while IFS='=' read -r key value || [ -n "$key" ]; do
    # 跳过注释和空行
    if [[ ! "$key" =~ ^#.* ]] && [ -n "$key" ]; then
        # 移除前后空格
        key=$(echo "$key" | xargs)
        value=$(echo "$value" | xargs)
        if [ -n "$value" ]; then
            CONFIG_ARGS="$CONFIG_ARGS -D$key=$value"
        fi
    fi
done < "$CONFIG_FILE"

# 根据环境设置日志文件
if [ "$ENV" = "prod" ]; then
    BACKEND_LOG="backend-prod.log"
else
    BACKEND_LOG="backend-dev.log"
fi

# 启动后端
nohup $MAVEN_CMD spring-boot:run -pl app-device \
  -Dspring-boot.run.main-class=com.example.voicebox.app.device.DeviceApp \
  -Dspring-boot.run.jvmArguments="$CONFIG_ARGS" > "$BACKEND_LOG" 2>&1 &
BACKEND_PID=$!
echo "$BACKEND_PID" > backend.pid

echo -e "${BLUE}后端 PID: $BACKEND_PID${NC}"
echo -e "${YELLOW}等待后端启动...${NC}"

# 等待后端启动（最多等待60秒）
BACKEND_READY=false
for i in {1..60}; do
    if curl -s http://localhost:10088/api/chat/sessions > /dev/null 2>&1; then
        BACKEND_READY=true
        break
    fi
    echo -n "."
    sleep 1
done

if [ "$BACKEND_READY" = true ]; then
    echo -e "\n${GREEN}✓ 后端服务启动成功 (http://localhost:10088)${NC}"
else
    echo -e "\n${RED}❌ 后端启动超时${NC}"
    echo -e "${YELLOW}请查看日志文件: $BACKEND_LOG${NC}"
    exit 1
fi
echo ""

# ==================== 步骤 4: 启动前端服务 ====================
echo -e "${YELLOW}[4/4] 正在启动前端服务...${NC}"
cd "$PROJECT_ROOT/app-web"

# 检查端口 5173 是否被占用
if lsof -Pi :5173 -sTCP:LISTEN -t >/dev/null 2>&1 ; then
    echo -e "${YELLOW}⚠️  端口 5173 已被占用，正在尝试关闭旧进程...${NC}"
    lsof -ti:5173 | xargs kill -9 2>/dev/null || true
    sleep 2
fi

# 根据环境设置前端日志文件
if [ "$ENV" = "prod" ]; then
    FRONTEND_LOG="../frontend-prod.log"
    # 生产环境构建前端
    echo -e "${YELLOW}生产环境：构建前端...${NC}"
    npm run build
    echo -e "${GREEN}✓ 前端构建完成${NC}"
    echo -e "${YELLOW}请使用 Nginx 或其他 Web 服务器提供静态文件${NC}"
else
    FRONTEND_LOG="../frontend-dev.log"
    # 开发环境启动开发服务器
    nohup npm run dev > "$FRONTEND_LOG" 2>&1 &
    FRONTEND_PID=$!
    echo "$FRONTEND_PID" > ../frontend.pid

    echo -e "${BLUE}前端 PID: $FRONTEND_PID${NC}"
    echo -e "${YELLOW}等待前端启动...${NC}"

    # 等待前端启动（最多等待30秒）
    FRONTEND_READY=false
    for i in {1..30}; do
        if curl -s http://localhost:5173 > /dev/null 2>&1; then
            FRONTEND_READY=true
            break
        fi
        echo -n "."
        sleep 1
    done

    if [ "$FRONTEND_READY" = true ]; then
        echo -e "\n${GREEN}✓ 前端服务启动成功 (http://localhost:5173)${NC}"
    else
        echo -e "\n${YELLOW}⚠️  前端启动可能需要更多时间${NC}"
        echo -e "${YELLOW}请查看日志文件: $FRONTEND_LOG${NC}"
    fi
fi
echo ""

# ==================== 启动完成 ====================
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}   🎉 所有服务启动完成！${NC}"
echo -e "${GREEN}   环境: ${ENV}${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${BLUE}服务地址:${NC}"
if [ "$ENV" = "prod" ]; then
    echo -e "  • 前端: ${YELLOW}请配置 Nginx 或其他 Web 服务器${NC}"
else
    echo -e "  • 前端: ${GREEN}http://localhost:5173${NC}"
fi
echo -e "  • 后端: ${GREEN}http://localhost:10088${NC}"
echo ""
echo -e "${BLUE}进程信息:${NC}"
echo -e "  • 后端 PID: ${YELLOW}$BACKEND_PID${NC}"
if [ "$ENV" = "dev" ] && [ -n "$FRONTEND_PID" ]; then
    echo -e "  • 前端 PID: ${YELLOW}$FRONTEND_PID${NC}"
fi
echo ""
echo -e "${BLUE}配置文件:${NC}"
echo -e "  • ${YELLOW}$CONFIG_FILE${NC}"
echo ""
echo -e "${BLUE}日志文件:${NC}"
echo -e "  • 后端: ${YELLOW}$BACKEND_LOG${NC}"
if [ "$ENV" = "dev" ]; then
    echo -e "  • 前端: ${YELLOW}$FRONTEND_LOG${NC}"
fi
echo ""
echo -e "${BLUE}停止服务:${NC}"
echo -e "  运行: ${YELLOW}./stop-all.sh${NC}"
echo ""
echo -e "${YELLOW}提示: 按 Ctrl+C 不会停止后台服务，请使用 stop-all.sh 脚本${NC}"
echo ""

