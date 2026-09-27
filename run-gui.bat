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
echo   🏦  VIETBANK — HỆ THỐNG NGÂN HÀNG (9 DESIGN PATTERNS)
echo   Khởi chạy Giao diện Đồ họa Desktop (JavaFX Modern UI)...
echo ════════════════════════════════════════════════════════════
echo.

call mvnw.cmd javafx:run

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [LỖI] Không thể khởi chạy JavaFX. Vui lòng kiểm tra JAVA_HOME hoặc log trên.
    pause
)
