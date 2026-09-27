@echo off
chcp 65001 >nul 2>&1
if not defined JAVA_HOME for /f "delims=" %%I in ('where javac 2^>nul') do if not defined JAVA_HOME set "JAVA_HOME=%%~dpI.."
if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo [LOI] Hay dat JAVA_HOME den thu muc JDK 17+ hoac them javac vao PATH.
    exit /b 1
)
cd /d "%~dp0"

echo Dang khoi chay VietBank trong terminal...
call mvnw.cmd -q compile exec:java
exit /b %ERRORLEVEL%
