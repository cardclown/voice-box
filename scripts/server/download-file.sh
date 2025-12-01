#!/bin/bash
# 从服务器下载文件到本地

if [ -z "$1" ]; then
    echo "用法: ./download-file.sh <服务器文件路径>"
    echo ""
    echo "示例:"
    echo "  ./download-file.sh config.properties"
    echo "  ./download-file.sh app-device/src/main/java/com/example/Controller.java"
    exit 1
fi

FILE_PATH="$1"
SERVER="voicebox-server"
REMOTE_DIR="/opt/voicebox"

echo "从服务器下载文件..."
echo "  服务器: $SERVER:$REMOTE_DIR/$FILE_PATH"
echo "  本地: $FILE_PATH"
echo ""

# 创建本地目录（如果需要）
mkdir -p "$(dirname "$FILE_PATH")"

scp -r "$SERVER:$REMOTE_DIR/$FILE_PATH" "$FILE_PATH"

echo ""
echo "✓ 下载完成"
echo ""
echo "提示: 记得提交到 Git:"
echo "  git add $FILE_PATH"
echo "  git commit -m '同步服务器变更: $FILE_PATH'"
