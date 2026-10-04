@echo off
setlocal
cd /d "%~dp0"

REM ============================================================
REM  Create the empty database (does nothing if it already exists).
REM  Host, port, DB name, user and password are read from
REM  config\app.default.properties, overridden by config\app.properties.
REM  Tables and sample products are created by the app on first start.
REM ============================================================

REM --- Load settings: defaults first, then local overrides ---
set "psql.path="
call :load config\app.default.properties
if exist config\app.properties call :load config\app.properties

REM --- Find psql. Order of search:
REM       1. psql.path in the config files
REM       2. PATH
REM       3. PostgreSQL install info in the registry (works for any install folder)
REM       4. C:\Program Files\PostgreSQL\<version>\bin
set "PSQL="
if defined psql.path (
    if exist "%psql.path%" (
        set "PSQL=%psql.path%"
    ) else (
        echo [ERROR] psql.path in the config file does not exist: %psql.path%
        exit /b 1
    )
)
if not defined PSQL where psql >nul 2>&1 && set "PSQL=psql"
if not defined PSQL (
    for /f "tokens=3*" %%a in ('reg query "HKLM\SOFTWARE\PostgreSQL\Installations" /s /v "Base Directory" 2^>nul ^| findstr /c:"REG_SZ"') do (
        if exist "%%b\bin\psql.exe" set "PSQL=%%b\bin\psql.exe"
    )
)
if not defined PSQL (
    for /d %%d in ("%ProgramFiles%\PostgreSQL\*") do if exist "%%d\bin\psql.exe" set "PSQL=%%d\bin\psql.exe"
)
if not defined PSQL (
    echo [ERROR] psql not found.
    echo         Checked: psql.path in config, PATH, the registry, and %ProgramFiles%\PostgreSQL\
    echo         - If PostgreSQL is installed, find psql.exe in its "bin" folder and set it in config\app.properties, for example:
    echo               psql.path=D:\PostgreSQL\18\bin\psql.exe
    echo         - If psql.exe does not exist, re-run the PostgreSQL installer and select "Command Line Tools".
    exit /b 1
)
echo Using psql: %PSQL%

REM Pass the password by environment variable so psql does not prompt for it
set "PGPASSWORD=%db.password%"
set "PGCLIENTENCODING=UTF8"

REM --- Does the database already exist? ---
set "TMPFILE=%TEMP%\mini_shop_dbcheck.txt"
"%PSQL%" -h %db.host% -p %db.port% -U %db.user% -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='%db.name%'" > "%TMPFILE%"
if errorlevel 1 (
    echo [ERROR] Cannot connect to PostgreSQL. Check that it is running and that the user and password are correct.
    echo         To change settings, copy config\app.default.properties to config\app.properties and edit it.
    exit /b 1
)
set "FOUND="
set /p FOUND=<"%TMPFILE%"
del "%TMPFILE%" >nul 2>&1

if "%FOUND%"=="1" (
    echo Database "%db.name%" already exists. Nothing to do.
    exit /b 0
)

REM --- Create it ---
"%PSQL%" -h %db.host% -p %db.port% -U %db.user% -d postgres -c "CREATE DATABASE \"%db.name%\""
if errorlevel 1 (
    echo [ERROR] Failed to create the database.
    exit /b 1
)
echo Database "%db.name%" created.
exit /b 0

REM --- Subroutine: load "key=value" lines of a properties file as variables ---
REM  Lines starting with # are comments. Example: db.host=localhost sets %db.host%
:load
for /f "usebackq eol=# tokens=1,* delims==" %%a in ("%~1") do set "%%a=%%b"
exit /b 0
