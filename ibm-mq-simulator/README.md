# IBM MQ Simulator

A professional IBM MQ Simulator built with Spring Boot and JavaFX for testing and development purposes.

## Features

- **Professional JavaFX UI** with dark theme
- **IBM MQ Connection Management** - Connect to IBM MQ queue managers
- **XML Message Handling**:
  - Auto-beautification of XML content
  - Real-time XML validation
  - Send button automatically disabled for invalid XML
- **High-Performance Message Sending**:
  - Configurable message count (1-20,000 messages)
  - Multi-threaded delivery (1-100 threads)
  - Real-time progress tracking with progress bar
  - Fast batch message sending
- **Send Messages** to configurable queues
- **Receive Messages** from queues
- **Message History** tracking for sent and received messages
- **Queue Selection** - Choose input/output queues dynamically

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- IBM MQ Server (for actual connection testing)

## Configuration

Configure IBM MQ connection details in `src/main/resources/application.properties`:

```properties
# IBM MQ Connection
ibm.mq.queue-manager=QM1
ibm.mq.channel=DEV.APP.SVRCONN
ibm.mq.conn-name=localhost(1414)
ibm.mq.user=app
ibm.mq.password=passw0rd
ibm.mq.receive-timeout=5000

# Queues
ibm.mq.queue.in=DEV.QUEUE.1
ibm.mq.queue.out=DEV.QUEUE.2
```

## Building

```bash
cd ibm-mq-simulator
mvn clean package
```

## Running

### Using Maven

```bash
mvn javafx:run
```

### Using the JAR file

```bash
java -jar target/ibm-mq-simulator-1.0.0.jar
```

## Usage

1. **Connect to IBM MQ**
   - Verify connection details in the top panel
   - Click the "Connect" button to establish connection
   - Status bar will show connection status

2. **Send Messages**
   - Enter or paste XML content in the left panel
   - Click "Beautify XML" to format the XML with proper indentation
   - The Send button will be automatically disabled if XML is invalid
   - Select target queue from dropdown
   - **Configure message count** (1-20000): Set how many copies of the message to send
   - **Configure thread count** (1-100): Set number of concurrent threads for fast delivery
   - Click "Send Message" to send to IBM MQ
   - Progress bar shows real-time sending progress for batch operations

3. **Receive Messages**
   - Select the queue to receive from
   - Click "Receive Messages" to pull messages from the queue
   - Messages will be displayed in the right panel with timestamps

4. **Message History**
   - View all sent and received messages in the history list
   - Click "Clear History" to reset the history

## Technology Stack

- **Spring Boot 3.2.0** - Application framework
- **JavaFX 21** - UI framework
- **IBM MQ 9.3.4.1** - MQ client libraries
- **Lombok** - Code simplification
- **Maven** - Build tool

## UI Features

- Dark professional theme
- Real-time XML validation with visual feedback
- Separate panels for sending and receiving messages
- Color-coded status indicators
- Message history with sent/received indicators
- Responsive button states based on connection and validation status

## License

See LICENSE file for details.
