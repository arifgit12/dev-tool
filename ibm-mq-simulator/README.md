# IBM MQ Simulator

A professional IBM MQ Simulator built with Spring Boot and JavaFX for testing and development purposes.

## Features

- **Professional JavaFX UI** with dark theme
- **IBM MQ Connection Management** - Connect to IBM MQ queue managers
- **XML Message Handling**:
  - Auto-beautification of XML content
  - Real-time XML validation
  - Send button automatically disabled for invalid XML
  - **Dynamic template parameters** - Use `${PLACEHOLDER}` for generating unique values
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
   - **Use dynamic templates**: Add `${PLACEHOLDER}` in XML to generate unique values per message
   - Click "Send Message" to send to IBM MQ
   - Progress bar shows real-time sending progress for batch operations

3. **Receive Messages**
   - Select the queue to receive from
   - Click "Receive Messages" to pull messages from the queue
   - Messages will be displayed in the right panel with timestamps

4. **Message History**
   - View all sent and received messages in the history list
   - Click "Clear History" to reset the history

## Dynamic Template Parameters

Use `${PLACEHOLDER}` syntax in your XML to generate unique values for each message. Perfect for load testing with realistic data.

### Supported Placeholders

| Placeholder | Description | Example Output |
|------------|-------------|----------------|
| `${ID}` | Numeric ID | `12345678` |
| `${UUID}` | UUID string | `550e8400-e29b-41d4-a716-446655440000` |
| `${NUMBER}` | Random number (1-999999) | `42873` |
| `${AMOUNT}` or `${PRICE}` | Decimal amount (0.01-9999.99) | `1234.56` |
| `${NAME}` | Full name | `John Smith` |
| `${FIRSTNAME}` | First name only | `Mary` |
| `${LASTNAME}` | Last name only | `Johnson` |
| `${EMAIL}` | Email address | `john.smith@example.com` |
| `${PHONE}` | Phone number | `555-123-4567` |
| `${DATE}` | Current date | `2023-12-23` |
| `${TIMESTAMP}` | Current date/time | `2023-12-23T12:30:45.123` |
| `${TIME}` | Current time | `12:30:45.123` |
| `${RANDOM}` | Random 8-char string | `a3f7b2c9` |

### Custom Ranges

| Placeholder | Description | Example |
|------------|-------------|---------|
| `${NUMBER:1-100}` | Number in range | `${NUMBER:1000-9999}` → `5432` |
| `${AMOUNT:10-1000}` | Amount in range | `${AMOUNT:50-500}` → `234.56` |
| `${STRING:5}` | Random string of length N | `${STRING:10}` → `aB3xY7kL9m` |

### Example Template

```xml
<?xml version="1.0" encoding="UTF-8"?>
<order>
    <orderId>${ID}</orderId>
    <orderNumber>ORD-${NUMBER}</orderNumber>
    <customer>
        <customerId>${UUID}</customerId>
        <name>${NAME}</name>
        <email>${EMAIL}</email>
        <phone>${PHONE}</phone>
    </customer>
    <items>
        <item>
            <productId>${NUMBER:1000-9999}</productId>
            <quantity>${NUMBER:1-10}</quantity>
            <price>${AMOUNT:10-1000}</price>
        </item>
    </items>
    <totalAmount>${AMOUNT}</totalAmount>
    <orderDate>${DATE}</orderDate>
    <timestamp>${TIMESTAMP}</timestamp>
    <reference>${RANDOM}</reference>
</order>
```

When sending 1000 messages with this template, each message will have unique IDs, names, amounts, timestamps, etc. If no `${}` placeholders are present, the XML is sent as-is.

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
