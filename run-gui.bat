@echo off
chcp 65001 >nul 2>&1
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
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
