@echo off
rem 切换控制台代码页为 UTF-8（脚本与日志均为 UTF-8 编码）
chcp 65001 >nul
rem ===================================================================
rem 用户中心后端 - 启动脚本（Windows）
rem
rem 部署结构：bin(脚本) / conf(外置配置) / lib(依赖)，应用瘦包位于根目录
rem 说明：conf 下的配置优先于 jar 内配置，改配置后重启即生效；
rem       可追加启动参数：start.bat --server.port=9001
rem ===================================================================
setlocal

set BASE_DIR=%~dp0..
set APP_MAIN=com.bdsoft.usercenter.UserCenterServerApplication

rem 应用 jar：位于部署包根目录
set APP_JAR=
for /f "delims=" %%f in ('dir /b "%BASE_DIR%\usercenter-web-*.jar" 2^>nul') do set APP_JAR=%BASE_DIR%\%%f
if "%APP_JAR%"=="" (
    echo 错误：%BASE_DIR% 下未找到应用 jar（usercenter-web-*.jar）
    exit /b 1
)

if not exist "%BASE_DIR%\logs" mkdir "%BASE_DIR%\logs"

rem JVM 参数：堆 / 元空间 / OOM 转储 / GC 日志（JDK17 统一日志，滚动 5 个文件、单个 20M）
set JAVA_OPTS=-Xms512m -Xmx1024m -XX:MaxMetaspaceSize=256m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=logs -Xlog:gc*:file=logs/gc-%%t.log:time,uptime,level,tags:filecount=5,filesize=20M

cd /d "%BASE_DIR%"
echo 启动用户中心：%APP_JAR%
java -Dfile.encoding=UTF-8 %JAVA_OPTS% -cp "conf;%APP_JAR%;lib\*" %APP_MAIN% %*

endlocal
