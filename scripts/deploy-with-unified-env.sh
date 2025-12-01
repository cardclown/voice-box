#!/bin/bash

###############################################################################
# 统一环境部署脚本
# 用途：配置服务器环境并部署应用
# 作者：VoiceBox Team
# 日期：2024-11-30
###############################################################################

set -e

SERVER="root@129.211.180.183"
REMOTE_DIR="/opt/voicebox"

echo "=========================================="
echo "统一环境部署流程"
echo "=========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 1. 检查本地环境
echo -e "${YELLOW}[1/5] 检查本地环境...${NC}"
echo "本地Java版本："
java -version 2>&1 | head -1

echo "本地Maven版本："
mvn -version | head -1

# 验证本地环境
JAVA_VERSION=$(java -version 2>&1 | head -1 | grep -o '1.8')
MAVEN_VERSION=$(mvn -version | head -1 | grep -o '3.6.3')

if [ -z "$JAVA_VERSION" ]; then
    echo -e "${RED}错误：本地Java版本不是1.8${NC}"
    exit 1
fi

if [ -z "$MAVEN_VERSION" ]; then
    echo -e "${RED}错误：本地Maven版本不是3.6.3${NC}"
    exit 1
fi

echo -e "${GREEN}本地环境验证通过${NC}"
echo ""

# 2. 上传环境配置脚本到服务器
echo -e "${YELLOW}[2/5] 上传环境配置脚本到服务器...${NC}"
scp scripts/server/setup-unified-environment.sh $SERVER:/tmp/
echo -e "${GREEN}脚本上传完成${NC}"
echo ""

# 3. 在服务器上执行环境配置
echo -e "${YELLOW}[3/5] 在服务器上配置统一环境...${NC}"
echo "这可能需要几分钟时间..."
echo ""

ssh $SERVER << 'ENDSSH'
    chmod +x /tmp/setup-unified-environment.sh
    /tmp/setup-unified-environment.sh
    
    # 重新加载环境变量
    source /etc/profile
    
    echo ""
    echo "环境配置完成，验证结果："
    java -version 2>&1 | head -1
    mvn -version | head -1
ENDSSH

echo ""
echo -e "${GREEN}服务器环境配置完成${NC}"
echo ""

# 4. 编译项目
echo -e "${YELLOW}[4/5] 在本地编译项目...${NC}"
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo -e "${RED}编译失败！${NC}"
    exit 1
fi

echo -e "${GREEN}编译成功${NC}"
echo ""

# 5. 部署到服务器
echo -e "${YELLOW}[5/5] 部署到服务器...${NC}"

# 停止服务
echo "停止服务..."
ssh $SERVER "cd $REMOTE_DIR && ./stop-all.sh" || true

# 备份当前版本
BACKUP_DIR="/opt/voicebox-backup/$(date +%Y%m%d_%H%M%S)"
ssh $SERVER "mkdir -p $BACKUP_DIR && cp -r $REMOTE_DIR/* $BACKUP_DIR/"
echo "已备份到: $BACKUP_DIR"

# 上传新版本
echo "上传jar包..."
scp app-device/target/app-device-0.0.1-SNAPSHOT.jar $SERVER:$REMOTE_DIR/app-device/target/

# 上传前端构建
echo "上传前端..."
cd app-web
npm run build
cd ..
scp -r app-web/dist/* $SERVER:$REMOTE_DIR/app-web/dist/

# 启动服务
echo "启动服务..."
ssh $SERVER << 'ENDSSH'
    source /etc/profile
    cd /opt/voicebox
    ./start-all.sh
    
    # 等待服务启动
    sleep 10
    
    # 检查服务状态
    ./status.sh
ENDSSH

echo ""
echo -e "${GREEN}=========================================="
echo "部署完成！"
echo "==========================================${NC}"
echo ""
echo "服务器环境："
echo "  Java: 1.8"
echo "  Maven: 3.6.3"
echo ""
echo "备份位置: $BACKUP_DIR"
echo ""
echo "请访问 http://129.211.180.183 测试应用"
