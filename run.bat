@echo off
chcp 65001 >nul 2>&1
if not defined JAVA_HOME for /f "delims=" %%I in ('where javac 2^>nul') do if not defined JAVA_HOME set "JAVA_HOME=%%~dpI.."
if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo [LOI] Hay dat JAVA_HOME den thu muc JDK 17+ hoac them javac vao PATH.
    pause
    exit /b 1
)
cd /d "%~dp0"

echo ════════════════════════════════════════════════════════════
echo   🏦  HỆ THỐNG NGÂN HÀNG — DESIGN PATTERNS DEMO
echo ════════════════════════════════════════════════════════════
echo   1. Giao diện đồ họa Desktop (JavaFX GUI)  [Khuyên dùng]
echo   2. Giao diện Console Terminal (Menu CLI)
echo ════════════════════════════════════════════════════════════
set /p choice="Chọn chế độ (1 hoặc 2, mặc định 1): "

if "%choice%"=="2" (
    echo.
    echo Đang khởi chạy Console Menu...
    call mvnw.cmd -q compile exec:java -D"exec.mainClass=com.banking.Main"
) else (
    echo.
    echo Đang khởi chạy JavaFX Modern GUI...
    call mvnw.cmd javafx:run
)

pause
