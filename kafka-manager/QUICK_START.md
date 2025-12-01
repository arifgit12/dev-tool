# Quick Start Guide

Get started with Kafka Manager in 5 minutes!

## Prerequisites

Ensure you have installed:
- Java 17 or higher (`java -version`)
- Maven 3.6+ (`mvn -version`)
- Access to a Kafka cluster

## Installation

### Option 1: Quick Run (Recommended for first-time users)

**Windows:**
```cmd
run.bat
```

**Linux/Mac:**
```bash
chmod +x run.sh
./run.sh
```

### Option 2: Manual Build and Run

```bash
# Build the project
mvn clean package

# Run the application
java -jar target/kafka-manager-1.0.0.jar
```

## First Steps

### 1. Add Your First Kafka Connection

When the application opens:

1. Go to the **Connections** tab
2. Click **Add Connection**
3. Fill in the basic details:
   ```
   Connection Name: My Local Kafka
   Bootstrap Servers: localhost:9092
   Security Protocol: PLAINTEXT
   ```
4. Click **Save**

### 2. Test the Connection

1. Select your connection in the table
2. Click **Test Connection**
3. You should see "Connection test successful!"

### 3. Connect and Explore

1. Select your connection from the dropdown at the top
2. Click **Connect**
3. Navigate to the **Topics** tab to see your topics
4. Click on any topic to view statistics

## Common Connection Examples

### Local Kafka (No Security)
```
Bootstrap Servers: localhost:9092
Security Protocol: PLAINTEXT
```

### Kafka with SASL/PLAIN Authentication
```
Bootstrap Servers: kafka-broker:9092
Security Protocol: SASL_PLAINTEXT
SASL Mechanism: PLAIN
Username: your-username
Password: your-password
```

### Kafka with SSL
```
Bootstrap Servers: kafka-broker:9093
Security Protocol: SSL
SSL Truststore Location: /path/to/truststore.jks
SSL Truststore Password: truststore-password
```

### Confluent Cloud
```
Bootstrap Servers: pkc-xxxxx.us-east-1.aws.confluent.cloud:9092
Security Protocol: SASL_SSL
SASL Mechanism: PLAIN
Username: <API_KEY>
Password: <API_SECRET>
```

## Browsing Messages

1. Navigate to **Messages** tab
2. Enter topic name (e.g., `my-topic`)
3. Set partition (usually start with 0)
4. Click **Fetch Latest** to see recent messages
5. Or enter a specific offset and click **Fetch Messages**

## Tips

- **Multiple Connections**: Add all your Kafka clusters and switch between them easily
- **Saved Connections**: Your connections are automatically saved to `~/.kafka-manager/connections.json`
- **Refresh Data**: Use the Refresh button to update topic lists and statistics
- **Partition View**: Click on topics to see message distribution across partitions

## Troubleshooting

**Can't connect?**
- Verify your Kafka cluster is running
- Check bootstrap servers address
- Ensure firewall allows connection
- Verify credentials if using authentication

**UI looks weird?**
- Update to Java 17 or higher
- Try adjusting UI scale: `java -Dsun.java2d.uiScale=1.5 -jar target/kafka-manager-1.0.0.jar`

**Application won't start?**
- Check Java version: `java -version` (must be 17+)
- Check Maven installation: `mvn -version`
- Review logs in the console for error messages

## Next Steps

- Explore the **Consumer Groups** tab to monitor your consumers
- Use the statistics charts to understand topic data distribution
- Browse messages to debug data issues
- Set up connections for all your environments (dev, staging, prod)

## Need Help?

Check the full [README.md](README.md) for detailed documentation and advanced features.
