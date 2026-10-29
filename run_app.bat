@echo off
cd /d "%~dp0"
set "PATH=C:\Program Files\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin;%PATH%"
echo Starting Campus Club Management System...
call mvn javafx:run
pause
