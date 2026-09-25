@echo off
chcp 65001 >nul 2>&1
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
cd /d "%~dp0"
call mvnw.cmd -q compile exec:java -D"exec.mainClass=com.banking.Main"
pause
