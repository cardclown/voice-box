#!/bin/bash
# 同步单个文件到服务器

if [ -z "$1" ]; then
    echo "用法: ./sync-file.sh <文件路径>"
    echo ""
    echo "示例:"
    echo "  ./sync-file.sh config.properties"
    echo "  ./sync-file.sh app-device/src/main/java/com/example/Controller.java"
    exit 1
fi

FILE_PATH="$1"
SERVER="voicebox-server"
REMOTE_DIR="/opt/voicebox"

if [ ! -f "$FILE_PATH" ] && [ ! -d "$FILE_PATH" ]; then
    echo "❌ 文件或目录不存在: $FILE_PATH"
    exit 1
fi

echo "同步文件到服务器..."
echo "  本地: $FILE_PATH"
echo "  服务器: $SERVER:$REMOTE_DIR/$FILE_PATH"
echo ""

scp -r "$FILE_PATH" "$SERVER:$REMOTE_DIR/$FILE_PATH"

echo ""
echo "✓ 同步完成"
echo ""
echo "提示: 如果修改了 Java 代码，需要重新编译:"
echo "  ssh $SERVER 'cd /opt/voicebox && mvn clean package -DskipTests'"
echo "  ssh $SERVER 'systemctl restart voicebox-backend'"
