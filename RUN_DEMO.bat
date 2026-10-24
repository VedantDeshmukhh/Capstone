@echo off
setlocal
cd /d "%~dp0"
title ResolveX Demo Launcher
cls

echo ================================================================
echo                   ResolveX Platform Launcher
echo ================================================================
echo.

echo [1/3] Checking environment and compiling Java sources ...
if not exist bin mkdir bin

dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d bin @sources.txt
if errorlevel 1 (
    echo.
    echo [ERROR] Compilation failed. Please ensure JDK 17 or newer is installed.
    if exist sources.txt del sources.txt
    pause
    exit /b 1
)
if exist sources.txt del sources.txt
echo [OK] Compilation successful.
echo.

echo [2/3] Starting ResolveX Servers ...
start "ResolveX Admin Server (Port 8080)" cmd /k "java -cp bin com.resolvex.AdminMain 8080"
timeout /t 2 >nul

start "ResolveX Student Portal (Port 8081)" cmd /k "java -cp bin com.resolvex.UserMain 8081"
timeout /t 2 >nul
echo [OK] Background servers running on ports 8080 and 8081.
echo.

echo [3/3] Opening Browser Windows ...
start "" "http://localhost:8080/login"
start "" "http://localhost:8081/login"

echo ================================================================
echo   ADMIN PORTAL  : http://localhost:8080/login
echo   Credentials   : admin / admin123
echo.
echo   USER PORTAL   : http://localhost:8081/login
echo   Credentials   : cse1 / pass123  (or aids1 / pass123, aiml1 / pass123)
echo ================================================================
echo.
echo Leave this window or the two server windows open to keep servers alive.
echo Press any key to stop both servers...
pause >nul

echo Stopping servers...
taskkill /fi "WINDOWTITLE eq ResolveX Admin Server*" /f >nul 2>&1
taskkill /fi "WINDOWTITLE eq ResolveX Student Portal*" /f >nul 2>&1
echo Done.
