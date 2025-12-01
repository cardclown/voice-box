#!/usr/bin/env bash
set -euo pipefail

echo "[dev-watch] 启动 Java 8 + Maven 3.6 开发循环..."

cd /workspace

APP_MODULE="app-device"
MAIN_CLASS="com.example.voicebox.app.device.DeviceApp"
JAR_PATH="${APP_MODULE}/target/app-device-0.0.1-SNAPSHOT.jar"
APP_PID=0

WATCH_PATHS=(
  "./app-device/src"
  "./cloud-provider-http/src"
  "./cloud-provider-sample/src"
  "./cloud-api/src"
  "./core-domain/src"
  "./hardware-api/src"
  "./hardware-mock/src"
  "./hardware-raspi/src"
)

# 捕获退出信号，清理子进程
trap 'kill_app; exit 0' SIGINT SIGTERM

kill_app() {
  if [ "$APP_PID" -gt 0 ]; then
    if kill -0 "$APP_PID" 2>/dev/null; then
      echo "[dev-watch] 停止旧应用进程 (PID: $APP_PID)..."
      kill "$APP_PID" || true
      wait "$APP_PID" 2>/dev/null || true
    fi
    APP_PID=0
  fi
}

build_app() {
  echo "[dev-watch] 开始构建 ${APP_MODULE}（会自动构建依赖模块）..."
  # 使用 install 确保 jar 安装到本地仓库，供 dependency:build-classpath 使用
  if mvn -q -pl "${APP_MODULE}" -am install -DskipTests; then
    echo "[dev-watch] 构建完成：${JAR_PATH}"
    return 0
  else
    echo "[dev-watch] 构建失败。"
    return 1
  fi
}

start_app() {
  echo "[dev-watch] 计算 classpath..."
  # 使用 dependency:build-classpath 代替 exec，更标准
  # -Dmdep.outputFile 避免解析 stdout
  mvn -q -pl "${APP_MODULE}" dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt
  CP=$(cat /tmp/cp.txt)
  
  echo "[dev-watch] 启动应用：${MAIN_CLASS}"
  # 加上本模块的 jar
  FULL_CP="${JAR_PATH}:${CP}"
  
  java -cp "${FULL_CP}" "${MAIN_CLASS}" &
  APP_PID=$!
  echo "[dev-watch] 应用已在后台启动 (PID: $APP_PID)"
}

watch_sources() {
  echo "[dev-watch] 监听源码变更..."
  # 使用 inotifywait 阻塞等待变更
  # -qq: 安静模式
  # -r: 递归
  # -e: 关注的事件
  inotifywait -qq -r -e modify,create,delete,move "${WATCH_PATHS[@]}"
}

# 初始构建与启动
if build_app; then
  start_app
else
  echo "[dev-watch] 初始构建失败，等待修复..."
fi

# 循环监听
while true; do
  watch_sources

  echo "[dev-watch] 检测到代码变更，准备重启..."
  
  # 给一点缓冲时间，避免短时间内多次触发（例如 IDE 同时保存多个文件）
  sleep 1

  kill_app

  if build_app; then
    start_app
  else
    echo "[dev-watch] 构建失败，等待下一次修改..."
  fi
done
