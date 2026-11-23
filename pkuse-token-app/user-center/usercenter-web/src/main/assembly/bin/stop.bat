@echo off
rem 切换控制台代码页为 UTF-8（脚本为 UTF-8 编码）
chcp 65001 >nul
rem ===================================================================
rem 用户中心后端 - 停止脚本（Windows）
rem 通过 jps 按主类查找进程并强制结束（需要 JDK 而非 JRE）
rem ===================================================================
setlocal

set APP_MAIN=com.bdsoft.usercenter.UserCenterServerApplication

set FOUND=0
for /f "tokens=1" %%i in ('jps -l ^| findstr "%APP_MAIN%"') do (
    echo 正在停止进程 %%i ...
    taskkill /F /PID %%i
    set FOUND=1
)
if "%FOUND%"=="0" echo 未发现运行中的用户中心进程。

endlocal
