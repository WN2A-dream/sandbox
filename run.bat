@echo off
cd /d "%~dp0"

REM ============================================================
REM  Build and start Mini Shop. Press Ctrl+C to stop.
REM  Before the first run: start PostgreSQL and run setup-db.bat.
REM ============================================================

call "%~dp0build.bat"
if errorlevel 1 exit /b 1

REM "lib\*" puts every jar in the lib folder on the classpath
java -Dfile.encoding=UTF-8 -cp "out\classes;lib\*" shop.Main
