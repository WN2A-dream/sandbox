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
call :load config\app.default.properties
if exist config\app.properties call :load config\app.properties

REM --- Find psql: PATH first, then C:\Program Files\PostgreSQL\<version>\bin ---
set "PSQL="
where psql >nul 2>&1 && set "PSQL=psql"
if not defined PSQL (
    for /d %%d in ("%ProgramFiles%\PostgreSQL\*") do if exist "%%d\bin\psql.exe" set "PSQL=%%d\bin\psql.exe"
)
if not defined PSQL (
    echo [ERROR] psql not found. Install PostgreSQL first.
    exit /b 1
)

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
