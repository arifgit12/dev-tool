@echo off
REM Oracle JMS Simulator - Run Script
REM This script builds and runs the Oracle JMS Simulator application

echo ==========================================
echo Oracle JMS Simulator
echo ==========================================
echo.

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Java is not installed or not in PATH
    echo Please install Java 17 or higher
    pause
    exit /b 1
)

REM Display Java version
echo Java version:
java -version
echo.

REM Check if Maven is installed
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Maven is not installed or not in PATH
    echo Please install Maven 3.6 or higher
    pause
    exit /b 1
)

REM Build if JAR doesn't exist
set JAR_FILE=target\oracle-jms-simulator-1.0.0.jar
if not exist "%JAR_FILE%" (
    echo Building application...
    call mvn clean package -DskipTests
    if %errorlevel% neq 0 (
        echo Build failed. Please check the errors above.
        pause
        exit /b 1
    )
    echo.
)

REM Run the application
echo Starting Oracle JMS Simulator...
echo.
java -jar "%JAR_FILE%"

pause
