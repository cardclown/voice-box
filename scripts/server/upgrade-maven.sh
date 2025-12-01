#!/bin/bash
# Maven 升级脚本 - 升级到 Maven 3.9.9
# 使用方法: 将此脚本上传到服务器并执行

set -e

echo "=========================================="
echo "Maven 升级脚本 - 升级到 3.9.9"
echo "=========================================="
echo ""

# 1. 检查当前版本
echo "1. 检查当前 Maven 版本："
mvn -version || true
echo ""

# 2. 下载 Maven 3.9.9
MAVEN_VERSION=3.9.9
MAVEN_HOME=/opt/maven
# 使用 Apache 归档镜像
MAVEN_DOWNLOAD_URL="https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"

echo "2. 下载 Maven ${MAVEN_VERSION}..."
cd /tmp
if [ -f "apache-maven-${MAVEN_VERSION}-bin.tar.gz" ]; then
    echo "   文件已存在，跳过下载"
else
    wget ${MAVEN_DOWNLOAD_URL} || {
        echo "   下载失败，尝试使用备用镜像..."
        wget "https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
    }
fi
echo ""

# 3. 备份旧版本（如果存在）
if [ -d "${MAVEN_HOME}" ]; then
    echo "3. 备份旧版本 Maven..."
    mv ${MAVEN_HOME} ${MAVEN_HOME}.backup.$(date +%Y%m%d_%H%M%S)
else
    echo "3. 未发现旧版本，跳过备份"
fi
echo ""

# 4. 解压新版本
echo "4. 安装 Maven ${MAVEN_VERSION}..."
tar -xzf apache-maven-${MAVEN_VERSION}-bin.tar.gz
mv apache-maven-${MAVEN_VERSION} ${MAVEN_HOME}
echo ""

# 5. 配置环境变量
echo "5. 配置环境变量..."
cat > /etc/profile.d/maven.sh << 'PROFILE'
# Maven 环境变量
export MAVEN_HOME=/opt/maven
export PATH=${MAVEN_HOME}/bin:${PATH}
PROFILE

chmod +x /etc/profile.d/maven.sh
source /etc/profile.d/maven.sh
echo ""

# 6. 清理旧的 Maven 链接（如果存在）
echo "6. 清理系统中的旧 Maven..."
# 移除 yum 安装的 Maven（如果存在）
if rpm -qa | grep -q maven; then
    echo "   发现 yum 安装的 Maven，正在移除..."
    yum remove -y maven || true
fi
echo ""

# 7. 验证新版本
echo "7. 验证新版本："
${MAVEN_HOME}/bin/mvn -version
echo ""

# 8. 清理下载文件
echo "8. 清理临时文件..."
rm -f /tmp/apache-maven-${MAVEN_VERSION}-bin.tar.gz
echo ""

echo "=========================================="
echo "✅ Maven 升级完成！"
echo "=========================================="
echo ""
echo "新版本信息："
${MAVEN_HOME}/bin/mvn -version
echo ""
echo "注意事项："
echo "1. 请执行: source /etc/profile.d/maven.sh"
echo "2. 或重新登录以使环境变量生效"
echo "3. 验证命令: mvn -version"
echo ""
