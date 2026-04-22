@echo off
title Optimization Algorithm Suite
color 0B

echo ========================================================
echo       BUILD PIPELINE: ALGORITHM COMPARISON SUITE
echo ========================================================
echo.

echo [1/4] Cleaning stale data and old executables...
mingw32-make clean
echo.

echo [2/4] Compiling C Backend Engines (MinGW)...
mingw32-make
if %ERRORLEVEL% NEQ 0 (
    color 0C
    echo.
    echo [FATAL ERROR] C Compilation failed! Check the terminal output above.
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo [3/4] Compiling Java Frontend UI...
javac *.java
if %ERRORLEVEL% NEQ 0 (
    color 0C
    echo.
    echo [FATAL ERROR] Java Compilation failed! Check the terminal output above.
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo [4/4] Pipeline Complete. Launching Dashboard...
echo ========================================================
echo.
java MainDashboard

:: If the user closes the Java window, pause so they can read any terminal output
pause