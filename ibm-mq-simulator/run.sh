#!/bin/bash

# IBM MQ Simulator - Quick Start Script
# This script helps you build and run the IBM MQ Simulator

echo "================================"
echo "IBM MQ Simulator - Quick Start"
echo "================================"
echo ""

# Navigate to project directory
cd "$(dirname "$0")"

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven is not installed. Please install Maven 3.6+ first."
    exit 1
fi

# Check Java version
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed. Please install Java 17+ first."
    exit 1
fi

echo "✓ Maven found: $(mvn --version | head -1)"
echo "✓ Java found: $(java -version 2>&1 | head -1)"
echo ""

# Build the project
echo "Building the project..."
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo ""
    echo "✓ Build successful!"
    echo ""
    echo "Starting the application..."
    echo ""
    mvn javafx:run
else
    echo ""
    echo "✗ Build failed. Please check the error messages above."
    exit 1
fi
