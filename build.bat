@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

REM ============================================================
REM  Build script. Maven is NOT required; only a JDK (javac).
REM    1. Download the PostgreSQL JDBC driver into lib\ if missing
REM    2. Compile src\main\java into out\classes
REM    3. Copy src\main\resources (schema.sql) into out\classes
REM  NOTE: this file is ASCII only on purpose. cmd.exe can misparse
REM  UTF-8 Japanese text in .bat files. See README.md for explanations.
REM ============================================================

set "DRIVER=postgresql-42.7.4.jar"
set "DRIVER_URL=https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.4/%DRIVER%"

where javac >nul 2>&1
if errorlevel 1 (
    echo [ERROR] javac not found. Install JDK 21 or later and add it to PATH.
    exit /b 1
)

REM --- 1. Driver: download only the first time ---
if not exist "lib\%DRIVER%" (
    echo Downloading PostgreSQL JDBC driver...
    if not exist lib mkdir lib
    powershell -NoProfile -Command "[Net.ServicePointManager]::SecurityProtocol='Tls12'; Invoke-WebRequest -Uri '%DRIVER_URL%' -OutFile 'lib\%DRIVER%'"
    if errorlevel 1 (
        echo [ERROR] Driver download failed. Check your network connection.
        if exist "lib\%DRIVER%" del "lib\%DRIVER%"
        exit /b 1
    )
)

REM --- 2. Compile ---
if exist out rmdir /s /q out
mkdir out\classes

REM Build the list of source files. Inside a javac argument file the backslash
REM is an escape character, so convert it to a forward slash.
if exist out\sources.txt del out\sources.txt
for /r "src\main\java" %%f in (*.java) do (
    set "p=%%f"
    echo "!p:\=/!">>out\sources.txt
)

javac -encoding UTF-8 -d out\classes -cp "lib\%DRIVER%" @out\sources.txt
if errorlevel 1 (
    echo [ERROR] Compilation failed.
    exit /b 1
)

REM --- 3. Resources ---
xcopy /e /i /y /q "src\main\resources" "out\classes" >nul

echo Build complete: out\classes
exit /b 0
