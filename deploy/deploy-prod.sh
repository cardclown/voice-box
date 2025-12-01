#!/bin/bash

# 生产环境部署脚本
# 用法: ./deploy/deploy-prod.sh

set -e

SERVER="root@129.211.180.183"
REMOTE_DIR="/opt/voicebox"
BRANCH="main"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}==========================================${NC}"
echo -e "${BLUE}   部署到生产环境${NC}"
echo -e "${BLUE}==========================================${NC}"
echo ""

# 1. 检查当前分支
echo -e "${YELLOW}[1/8] 检查 Git 分支...${NC}"
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "$BRANCH" ]; then
    echo -e "${RED}❌ 错误: 必须在 $BRANCH 分支上部署${NC}"
    echo -e "${YELLOW}当前分支: $CURRENT_BRANCH${NC}"
    exit 1
fi
echo -e "${GREEN}✓ 当前在 $BRANCH 分支${NC}"
echo ""

# 2. 检查是否有未提交的变更
echo -e "${YELLOW}[2/8] 检查未提交的变更...${NC}"
if [ -n "$(git status --porcelain)" ]; then
    echo -e "${RED}❌ 错误: 有未提交的变更${NC}"
    git status
    exit 1
fi
echo -e "${GREEN}✓ 没有未提交的变更${NC}"
echo ""

# 3. 确认部署
echo -e "${YELLOW}[3/8] 确认部署...${NC}"
read -p "确认部署到生产环境? (yes/no): " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
    echo -e "${YELLOW}取消部署${NC}"
    exit 0
fi
echo ""

# 4. 打包代码
echo -e "${YELLOW}[4/8] 打包代码...${NC}"
tar -czf voicebox-prod.tar.gz \
    --exclude=node_modules \
    --exclude=target \
    --exclude=.git \
    --exclude=*.log \
    --exclude=*.pid \
    --exclude=config-dev.properties \
    --exclude=config.properties \
    .
echo -e "${GREEN}✓ 代码打包完成${NC}"
echo ""

# 5. 上传到服务器
echo -e "${YELLOW}[5/8] 上传到服务器...${NC}"
scp voicebox-prod.tar.gz $SERVER:/tmp/
echo -e "${GREEN}✓ 上传完成${NC}"
echo ""

# 6. 在服务器上部署
echo -e "${YELLOW}[6/8] 在服务器上部署...${NC}"
ssh $SERVER << 'ENDSSH'
set -e

cd /opt/voicebox

echo "停止服务..."
./stop-all.sh || true

echo "备份当前版本..."
BACKUP_DIR="/opt/voicebox-backup/$(date +%Y%m%d_%H%M%S)"
mkdir -p $BACKUP_DIR
cp -r /opt/voicebox/* $BACKUP_DIR/ || true
echo "备份位置: $BACKUP_DIR"

echo "解压新版本..."
tar -xzf /tmp/voicebox-prod.tar.gz

echo "检查生产配置..."
if [ ! -f config-prod.properties ]; then
    echo "❌ 错误: config-prod.properties 不存在"
    echo "请先创建生产配置文件"
    exit 1
fi

echo "编译后端..."
mvn clean install -DskipTests

echo "构建前端..."
cd app-web
npm install
npm run build
cd ..

echo "启动服务..."
./start-all.sh prod

echo "等待服务启动..."
sleep 10

echo "检查服务状态..."
./status.sh

echo "✅ 部署完成！"
echo "备份位置: $BACKUP_DIR"
ENDSSH

echo -e "${GREEN}✓ 服务器部署完成${NC}"
echo ""

# 7. 清理本地临时文件
echo -e "${YELLOW}[7/8] 清理临时文件...${NC}"
rm voicebox-prod.tar.gz
echo -e "${GREEN}✓ 清理完成${NC}"
echo ""

# 8. 验证部署
echo -e "${YELLOW}[8/8] 验证部署...${NC}"
if curl -s http://129.211.180.183:10088/api/chat/sessions > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 后端服务正常${NC}"
else
    echo -e "${RED}❌ 后端服务异常${NC}"
    echo -e "${YELLOW}请检查服务器日志${NC}"
fi
echo ""

echo -e "${GREEN}==========================================${NC}"
echo -e "${GREEN}   🎉 生产环境部署完成！${NC}"
echo -e "${GREEN}==========================================${NC}"
echo ""
echo -e "${BLUE}服务地址:${NC}"
echo -e "  • 后端: ${GREEN}http://129.211.180.183:10088${NC}"
echo ""
echo -e "${BLUE}查看日志:${NC}"
echo -e "  ssh $SERVER 'tail -f /opt/voicebox/backend-prod.log'"
echo ""
