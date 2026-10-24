@echo off
setlocal
title Stop ResolveX Servers
cls

echo ================================================================
echo                   Stopping ResolveX Servers...
echo ================================================================
echo.

taskkill /im javaw.exe /f >nul 2>&1
taskkill /im java.exe /f >nul 2>&1

echo [OK] All ResolveX background servers have been stopped.
echo.
timeout /t 3 >nul
exit
