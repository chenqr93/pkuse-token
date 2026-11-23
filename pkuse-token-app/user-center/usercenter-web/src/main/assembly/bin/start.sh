#!/bin/sh
# ===================================================================
# 用户中心后端 - 启动脚本（Linux）
# 部署结构：bin(脚本) / config(外置配置) / lib(依赖) / 应用瘦包
# 额外启动参数可写入 config/start.args（每行一个参数，支持 # 注释）
# ===================================================================
BASE_DIR=$(cd "$(dirname "$0")/.." && pwd)
DEFAULT_JAVA_HOME=/opt/jdk-17.0.16
JAVA_HOME=${JAVA_HOME:-$DEFAULT_JAVA_HOME}
JAVA_BIN="$JAVA_HOME/bin/java"
JAVA_MAJOR=""
if [ -x "$JAVA_BIN" ]; then
    JAVA_MAJOR=$("$JAVA_BIN" -version 2>&1 | awk -F'"' '/version/ {split($2,a,"."); print (a[1]=="1"?a[2]:a[1])}' | head -n 1)
fi
case "$JAVA_MAJOR" in ''|*[!0-9]*) JAVA_MAJOR=0 ;; esac
if [ "$JAVA_MAJOR" -lt 17 ] && [ -x "$DEFAULT_JAVA_HOME/bin/java" ]; then
    JAVA_HOME="$DEFAULT_JAVA_HOME"
    JAVA_BIN="$JAVA_HOME/bin/java"
    JAVA_MAJOR=$("$JAVA_BIN" -version 2>&1 | awk -F'"' '/version/ {split($2,a,"."); print (a[1]=="1"?a[2]:a[1])}' | head -n 1)
fi
if [ ! -x "$JAVA_BIN" ] || [ "${JAVA_MAJOR:-0}" -lt 17 ]; then
    echo "错误：需要 JDK 17+，当前 JAVA_HOME=$JAVA_HOME"
    exit 1
fi
APP_MAIN=com.bdsoft.usercenter.UserCenterServerApplication
APP_JAR=$(ls "$BASE_DIR"/usercenter.jar 2>/dev/null | head -n 1)
if [ -z "$APP_JAR" ]; then
    echo "错误：$BASE_DIR 下未找到应用 jar（usercenter.jar）"
    exit 1
fi
if [ -f "$BASE_DIR/app.pid" ] && kill -0 "$(cat "$BASE_DIR/app.pid")" 2>/dev/null; then
    echo "用户中心已在运行，pid=$(cat "$BASE_DIR/app.pid")"
    exit 0
fi
mkdir -p "$BASE_DIR/logs"
JAVA_OPTS="-Xms512m -Xmx1024m -XX:MaxMetaspaceSize=256m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$BASE_DIR/logs -Xlog:gc*:file=$BASE_DIR/logs/gc-%t.log:time,uptime,level,tags:filecount=5,filesize=20M"
EXTRA_ARGS=""
if [ -f "$BASE_DIR/config/start.args" ]; then
    EXTRA_ARGS=$(grep -v '^[[:space:]]*#' "$BASE_DIR/config/start.args" | tr '\n' ' ')
fi
cd "$BASE_DIR" || exit 1
nohup "$JAVA_BIN" -Dfile.encoding=UTF-8 $JAVA_OPTS -cp "config:$APP_JAR:lib/*" "$APP_MAIN" --spring.config.additional-location=optional:file:$BASE_DIR/config/ $EXTRA_ARGS "$@" > "$BASE_DIR/logs/app.log" 2>&1 &
echo $! > "$BASE_DIR/app.pid"
echo "用户中心已启动，pid=$(cat "$BASE_DIR/app.pid")，日志：$BASE_DIR/logs/app.log"
