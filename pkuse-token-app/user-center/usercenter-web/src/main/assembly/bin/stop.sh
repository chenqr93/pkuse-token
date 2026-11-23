#!/bin/sh
BASE_DIR=$(cd "$(dirname "$0")/.." && pwd)
PID_FILE="$BASE_DIR/app.pid"
if [ ! -f "$PID_FILE" ]; then echo "用户中心未运行"; exit 0; fi
PID=$(cat "$PID_FILE")
if ! kill -0 "$PID" 2>/dev/null; then echo "用户中心进程不存在，清理 pid 文件"; rm -f "$PID_FILE"; exit 0; fi
kill "$PID" 2>/dev/null
for i in $(seq 1 20); do kill -0 "$PID" 2>/dev/null || break; sleep 1; done
if kill -0 "$PID" 2>/dev/null; then kill -9 "$PID" 2>/dev/null; fi
rm -f "$PID_FILE"
echo "用户中心已停止"
