#!/bin/bash

# Oracle JMS Simulator - Run Script
# This script builds and runs the Oracle JMS Simulator application

echo "=========================================="
echo "Oracle JMS Simulator"
echo "=========================================="
echo ""

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH"
    echo "Please install Java 17 or higher"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo "Java version: $JAVA_VERSION"
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed or not in PATH"
    echo "Please install Maven 3.6 or higher"
    exit 1
fi

# Build if JAR doesn't exist
JAR_FILE="target/oracle-jms-simulator-1.0.0.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "Building application..."
    mvn clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo "Build failed. Please check the errors above."
        exit 1
    fi
    echo ""
fi

# Run the application
echo "Starting Oracle JMS Simulator..."
echo ""
java -jar "$JAR_FILE"
