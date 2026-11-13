@echo off
title JavaBank Core Simulator Launcher
color 0c
cd /d "%~dp0"

echo =========================================================
echo       Starting JavaBank OOP Banking Simulator
echo =========================================================
echo.

where javac >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [NOTICE] Java JDK 17 (javac) is not yet in your system PATH!
    echo.
    echo To compile and run raw Java console:
    echo   1. Install Java JDK 17 (or run setup_environment.bat)
    echo.
    echo Opening instant interactive web console simulator in your browser right now...
    start "" "demo\index.html"
    pause
    exit /b
)

where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    mvn clean compile exec:java -Dexec.mainClass="com.javabank.main.JavaBankApp"
) else (
    if not exist "bin" mkdir "bin"
    javac -d "bin" src/main/java/com/javabank/*/*.java src/main/java/com/javabank/*.java
    java -cp "bin" com.javabank.main.JavaBankApp
)
pause
