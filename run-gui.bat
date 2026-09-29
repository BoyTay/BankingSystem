@echo off
setlocal
cd /d "%~dp0"

if not defined JAVA_HOME (
    for /f "delims=" %%I in ('where javac 2^>nul') do (
        if not defined JAVA_HOME set "JAVA_HOME=%%~dpI.."
    )
)

if not exist "%JAVA_HOME%\bin\javac.exe" (
    for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk*") do (
        if exist "%%D\bin\javac.exe" set "JAVA_HOME=%%D"
    )
)
if not exist "%JAVA_HOME%\bin\javac.exe" (
    for /d %%D in ("C:\Program Files\Java\jdk*") do (
        if exist "%%D\bin\javac.exe" set "JAVA_HOME=%%D"
    )
)

if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo [LOI] Hay dat JAVA_HOME den thu muc JDK 17+ hoac them javac vao PATH.
    pause
    exit /b 1
)

echo ========================================================
echo   NOVABANK - HE THONG NGAN HANG (9 DESIGN PATTERNS)
echo   Dang khoi chay giao dien Desktop NovaBank...
echo ========================================================
echo.

call "%~dp0mvnw.cmd" javafx:run

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [LOI] Khong the khoi chay JavaFX. Vui long kiem tra log tren.
    pause
)
endlocal
