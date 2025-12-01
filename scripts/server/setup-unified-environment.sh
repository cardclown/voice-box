#!/bin/bash

###############################################################################
# 统一环境配置脚本
# 用途：在服务器上配置统一的Maven 3.6.3和Java 1.8环境
# 作者：VoiceBox Team
# 日期：2024-11-30
###############################################################################

set -e  # 遇到错误立即退出

echo "=========================================="
echo "统一环境配置脚本"
echo "目标：Maven 3.6.3 + Java 1.8"
echo "=========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查是否为root用户
if [ "$EUID" -ne 0 ]; then 
    echo -e "${RED}请使用root用户运行此脚本${NC}"
    echo "使用方法: sudo $0"
    exit 1
fi

# 1. 检查当前环境
echo -e "${YELLOW}[1/6] 检查当前环境...${NC}"
echo "当前Java版本："
java -version 2>&1 || echo "Java未安装"
echo ""
echo "当前Maven版本："
mvn -version 2>&1 || echo "Maven未安装"
echo ""

# 2. 备份现有配置
echo -e "${YELLOW}[2/6] 备份现有配置...${NC}"
BACKUP_DIR="/root/environment-backup-$(date +%Y%m%d_%H%M%S)"
mkdir -p $BACKUP_DIR

# 备份环境变量配置
if [ -f /etc/profile ]; then
    cp /etc/profile $BACKUP_DIR/profile.bak
fi
if [ -f ~/.bashrc ]; then
    cp ~/.bashrc $BACKUP_DIR/bashrc.bak
fi

echo "配置已备份到: $BACKUP_DIR"
echo ""

# 3. 清理旧的Java和Maven
echo -e "${YELLOW}[3/6] 清理旧的Java和Maven环境...${NC}"

# 清理Maven
if [ -d "/opt/maven" ]; then
    echo "删除旧的Maven: /opt/maven"
    rm -rf /opt/maven
fi

if [ -d "/usr/local/maven" ]; then
    echo "删除旧的Maven: /usr/local/maven"
    rm -rf /usr/local/maven
fi

# 清理环境变量中的Maven配置
sed -i '/MAVEN_HOME/d' /etc/profile
sed -i '/maven/d' /etc/profile

echo "旧环境清理完成"
echo ""

# 4. 安装Java 1.8
echo -e "${YELLOW}[4/6] 安装Java 1.8...${NC}"

# 检查是否已安装Java 1.8
if java -version 2>&1 | grep -q "1.8"; then
    echo -e "${GREEN}Java 1.8已安装${NC}"
else
    echo "安装OpenJDK 1.8..."
    yum install -y java-1.8.0-openjdk java-1.8.0-openjdk-devel
    
    # 设置Java环境变量
    JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
    echo "export JAVA_HOME=$JAVA_HOME" >> /etc/profile
    echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> /etc/profile
    
    echo -e "${GREEN}Java 1.8安装完成${NC}"
fi

java -version
echo ""

# 5. 安装Maven 3.6.3
echo -e "${YELLOW}[5/6] 安装Maven 3.6.3...${NC}"

MAVEN_VERSION="3.6.3"
MAVEN_HOME="/opt/maven"

# 下载Maven 3.6.3
cd /tmp
if [ ! -f "apache-maven-${MAVEN_VERSION}-bin.tar.gz" ]; then
    echo "下载Maven ${MAVEN_VERSION}..."
    wget https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz
fi

# 解压并安装
echo "解压Maven..."
tar -xzf apache-maven-${MAVEN_VERSION}-bin.tar.gz
mkdir -p $MAVEN_HOME
mv apache-maven-${MAVEN_VERSION}/* $MAVEN_HOME/

# 设置Maven环境变量
echo "export MAVEN_HOME=$MAVEN_HOME" >> /etc/profile
echo "export PATH=\$MAVEN_HOME/bin:\$PATH" >> /etc/profile

# 立即生效
export MAVEN_HOME=$MAVEN_HOME
export PATH=$MAVEN_HOME/bin:$PATH

echo -e "${GREEN}Maven 3.6.3安装完成${NC}"
echo ""

# 6. 验证安装
echo -e "${YELLOW}[6/6] 验证安装...${NC}"

# 重新加载环境变量
source /etc/profile

echo "Java版本："
java -version

echo ""
echo "Maven版本："
mvn -version

echo ""
echo -e "${GREEN}=========================================="
echo "环境配置完成！"
echo "==========================================${NC}"
echo ""
echo "配置信息："
echo "  Java: $(java -version 2>&1 | head -1)"
echo "  Maven: $(mvn -version | head -1)"
echo "  JAVA_HOME: $JAVA_HOME"
echo "  MAVEN_HOME: $MAVEN_HOME"
echo ""
echo "备份位置: $BACKUP_DIR"
echo ""
echo -e "${YELLOW}注意：请退出当前会话并重新登录以使环境变量生效${NC}"
echo "或者运行: source /etc/profile"
