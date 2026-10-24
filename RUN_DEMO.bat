@echo off
setlocal
cd /d "%~dp0"
title ResolveX Launcher
cls

echo ================================================================
echo                   ResolveX Platform Launcher
echo ================================================================
echo.

echo [1/3] Compiling Java sources ...
if not exist bin mkdir bin

javac -encoding UTF-8 -d bin src\com\resolvex\*.java src\com\resolvex\model\*.java src\com\resolvex\service\*.java src\com\resolvex\util\*.java src\com\resolvex\web\*.java
if errorlevel 1 (
    echo.
    echo [ERROR] Compilation failed. Please ensure JDK 17 or newer is installed.
    pause
    exit /b 1
)
echo [OK] Compilation successful.
echo.

echo [2/3] Starting ResolveX Servers ...
start "ResolveX Admin Server" /min cmd /c "java -cp bin com.resolvex.AdminMain 8080"
ping 127.0.0.1 -n 3 >nul

start "ResolveX Student Portal" /min cmd /c "java -cp bin com.resolvex.UserMain 8081"
ping 127.0.0.1 -n 3 >nul
echo [OK] Background servers running on ports 8080 and 8081.
echo.

echo [3/3] Opening Browser Windows ...
start "" "http://localhost:8080/login"
start "" "http://localhost:8081/login"

cls
echo ================================================================
echo                     ResolveX is LIVE!
echo ================================================================
echo.
echo   ADMIN DASHBOARD : http://localhost:8080/login
echo   Credentials     : admin  /  admin123
echo.
echo   STUDENT PORTAL  : http://localhost:8081/login
echo   Credentials     : cse1   /  pass123
echo                     (or aids1 / pass123, aiml1 / pass123)
echo.
echo ================================================================
echo   Servers are active in the background.
echo   To stop servers anytime, double-click STOP.bat
echo ================================================================
ping 127.0.0.1 -n 4 >nul
exit
