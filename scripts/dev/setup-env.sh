#!/bin/bash
# VoiceBox 项目环境配置脚本

# 使用 mydesk 下的 Maven 3.6.3（兼容 Java 8）
export PATH="$HOME/mydesk/maven/bin:$PATH"

echo "✅ 环境配置完成"
echo "Maven 版本："
mvn -version
echo ""
echo "使用方法："
echo "  source setup-env.sh  # 在当前 shell 中加载环境变量"
echo "  mvn clean install    # 然后就可以正常使用 Maven 了"
