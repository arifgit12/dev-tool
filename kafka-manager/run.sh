#!/bin/bash

echo "Starting Kafka Manager..."
echo

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed or not in PATH"
    echo "Please install Maven and try again"
    exit 1
fi

# Build the application
echo "Building application..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

echo
echo "Starting Kafka Manager UI..."
echo
java -jar target/kafka-manager-1.0.0.jar &

echo "Kafka Manager started successfully!"
sleep 2
