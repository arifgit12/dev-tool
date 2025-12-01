# Kafka Manager - Enterprise Edition

A professional Spring Boot Swing application for managing and monitoring Apache Kafka clusters with advanced visualization and enterprise features.

## Features

### Connection Management
- **Multiple Kafka Cluster Support**: Connect to multiple Kafka clusters simultaneously
- **Secure Connections**: Support for PLAINTEXT, SSL, SASL_PLAINTEXT, and SASL_SSL security protocols
- **SASL Authentication**: Support for PLAIN, SCRAM-SHA-256, SCRAM-SHA-512, and GSSAPI mechanisms
- **Connection Persistence**: Save and load connection configurations automatically
- **Connection Testing**: Test connections before connecting to ensure credentials are valid

### Topic Management
- **List All Topics**: View all topics including internal topics
- **Topic Details**: View partition count, replication factor, and comprehensive configuration
- **Topic Statistics**: Real-time statistics showing message counts per partition
- **Partition Visualization**: Interactive bar charts showing message distribution across partitions
- **Configuration Viewer**: Browse all topic-level configurations

### Consumer Group Monitoring
- **List Consumer Groups**: View all consumer groups in the cluster
- **Group Details**: Monitor consumer group state, member count, and coordinator information
- **Partition Assignment**: View partition assignor strategy and member details

### Message Browser
- **Browse Messages**: Fetch and view messages from any topic partition
- **Offset Navigation**: Start from any offset or fetch the latest messages
- **Message Details**: View full message content including key, value, and timestamp
- **Flexible Limits**: Control the number of messages to fetch (1-10,000)
- **Latest Messages**: Quick access to the most recent messages

### User Interface
- **Modern Dark Theme**: Sleek FlatLaf dark theme for reduced eye strain
- **Tabbed Interface**: Organized tabs for different management functions
- **Split Panes**: Efficient use of space with resizable panels
- **Real-time Updates**: Automatic refresh capabilities for all views
- **Status Bar**: Connection status and operation feedback

## Requirements

- Java 17 or higher
- Maven 3.6 or higher
- Access to Apache Kafka cluster (version 2.x or 3.x)

## Building the Application

```bash
# Clone the repository
cd kafka-manager

# Build with Maven
mvn clean package

# Run the application
mvn spring-boot:run
```

Alternatively, run the JAR file:

```bash
java -jar target/kafka-manager-1.0.0.jar
```

## Usage Guide

### 1. Adding a Kafka Connection

1. Navigate to the **Connections** tab
2. Click **Add Connection**
3. Fill in the connection details:
   - **Connection Name**: A friendly name for the connection
   - **Bootstrap Servers**: Comma-separated list of Kafka brokers (e.g., `localhost:9092`)
   - **Security Protocol**: Select the security protocol (PLAINTEXT, SSL, SASL_PLAINTEXT, SASL_SSL)
   - **SASL Mechanism**: Select if using SASL (PLAIN, SCRAM-SHA-256, SCRAM-SHA-512, GSSAPI)
   - **Username/Password**: Enter credentials if using SASL authentication
4. Click **Save**
5. Optionally click **Test Connection** to verify the configuration

### 2. Connecting to a Cluster

1. Select a connection from the dropdown in the top toolbar
2. Click the **Connect** button
3. Wait for the connection to establish
4. The status bar will show "Connected to [cluster name]"

### 3. Browsing Topics

1. Ensure you're connected to a cluster
2. Navigate to the **Topics** tab
3. The topics list will automatically load
4. Click on a topic to view:
   - Configuration details
   - Partition statistics with visual charts
   - Message count per partition

### 4. Monitoring Consumer Groups

1. Navigate to the **Consumer Groups** tab
2. Select a consumer group from the list
3. View detailed information:
   - Group state (Stable, Rebalancing, Dead, etc.)
   - Number of active members
   - Coordinator broker
   - Partition assignment strategy

### 5. Browsing Messages

1. Navigate to the **Messages** tab
2. Enter the topic name
3. Select the partition number
4. Choose either:
   - **Specific Offset**: Enter the offset and click "Fetch Messages"
   - **Latest Messages**: Click "Fetch Latest" to get the most recent messages
5. Adjust the limit to control how many messages to fetch
6. Click on a message row to view full details in the bottom panel

## Configuration

Connection configurations are automatically saved to:
- Windows: `C:\Users\[username]\.kafka-manager\connections.json`
- Linux/Mac: `~/.kafka-manager/connections.json`

## Architecture

### Technology Stack
- **Spring Boot 3.2.0**: Application framework
- **Apache Kafka Clients 3.6.0**: Kafka connectivity
- **Swing**: Desktop UI framework
- **FlatLaf**: Modern look and feel
- **JFreeChart**: Statistics visualization
- **Jackson**: JSON serialization
- **Lombok**: Code generation

### Project Structure
```
kafka-manager/
├── src/main/java/com/kafkamanager/
│   ├── KafkaManagerApplication.java    # Main application
│   ├── model/
│   │   ├── KafkaConnection.java        # Connection model
│   │   └── TopicInfo.java              # Topic information model
│   ├── service/
│   │   ├── KafkaConnectionManager.java # Connection & Kafka operations
│   │   └── ConfigurationService.java   # Configuration persistence
│   └── ui/
│       ├── MainFrame.java              # Main window
│       ├── ConnectionPanel.java        # Connection management
│       ├── TopicsPanel.java            # Topic browser
│       ├── ConsumerGroupsPanel.java    # Consumer group monitor
│       └── MessagesPanel.java          # Message browser
└── src/main/resources/
    └── application.properties          # Spring Boot configuration
```

## Security Best Practices

1. **Store Sensitive Data Securely**: The application stores connection details including passwords in plain JSON. Consider:
   - Using encrypted file systems
   - Setting proper file permissions on the configuration directory
   - Using Kafka ACLs to limit access

2. **Use SSL/TLS**: For production environments, always use SSL or SASL_SSL protocols

3. **Limit Permissions**: Create read-only Kafka users for monitoring purposes

4. **Network Security**: Ensure the application can only access authorized Kafka clusters

## Troubleshooting

### Connection Issues
- Verify bootstrap servers are reachable
- Check firewall rules
- Verify authentication credentials
- Ensure security protocol matches cluster configuration

### Performance Issues
- Reduce the message fetch limit for large partitions
- Close unused connections
- Increase JVM heap size if needed: `java -Xmx2g -jar kafka-manager-1.0.0.jar`

### UI Issues
- If the UI appears incorrectly, try updating Java to the latest version
- For high DPI displays, set: `java -Dsun.java2d.uiScale=1.5 -jar kafka-manager-1.0.0.jar`

## Future Enhancements

Planned features for future versions:
- Message publishing to topics
- Schema Registry integration
- Kafka Connect monitoring
- Topic creation and deletion
- Consumer group lag monitoring
- Export data to CSV/JSON
- Search and filter capabilities
- Message key/value deserializers (Avro, JSON, Protobuf)

## License

This project is provided as-is for educational and professional use.

## Support

For issues, questions, or contributions, please refer to the project repository.
