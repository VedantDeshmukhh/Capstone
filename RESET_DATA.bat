@echo off
cd /d "%~dp0"
echo Stop both ResolveX windows first.
del /q data\accounts.txt data\complaints.txt data\groups.txt data\categories.txt 2>nul
echo Old data removed. Run RUN_DEMO.bat - fresh data is created from data\import\*.csv
pause
