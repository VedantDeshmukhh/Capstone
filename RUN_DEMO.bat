@echo off
cd /d "%~dp0"
echo Compiling ResolveX ...
if not exist bin mkdir bin
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d bin @sources.txt
if errorlevel 1 (
  echo.
  echo Compile failed. Install JDK 17 or newer and try again.
  pause
  exit /b 1
)
del sources.txt
start "ResolveX ADMIN  - http://localhost:8080" java -cp bin com.resolvex.AdminMain
timeout /t 2 >nul
start "ResolveX USER   - http://localhost:8081" java -cp bin com.resolvex.UserMain
echo.
echo Admin : http://localhost:8080   (admin / admin123)
echo User  : http://localhost:8081   (cse1 / pass123 , aids1 / pass123 , aiml1 / pass123)
echo Close the two black windows to stop.
pause
