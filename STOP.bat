@echo off
setlocal
title Stop ResolveX Servers
cls

echo Stopping ResolveX background servers...
taskkill /fi "WINDOWTITLE eq ResolveX Admin Server*" /f >nul 2>&1
taskkill /fi "WINDOWTITLE eq ResolveX Student Portal*" /f >nul 2>&1
taskkill /im javaw.exe /f >nul 2>&1
taskkill /im java.exe /f >nul 2>&1

echo [OK] All ResolveX servers have been stopped.
ping 127.0.0.1 -n 3 >nul
exit
