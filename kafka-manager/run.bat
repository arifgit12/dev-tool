@echo off
echo Starting Kafka Manager...
echo.

REM Check if Maven is installed
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Error: Maven is not installed or not in PATH
    echo Please install Maven and try again
    pause
    exit /b 1
)

REM Build and run the application
echo Building application...
call mvn clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo.
echo Starting Kafka Manager UI...
echo.
start javaw -jar target\kafka-manager-1.0.0.jar

echo Kafka Manager started successfully!
timeout /t 3
