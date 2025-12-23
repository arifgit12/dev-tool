# Oracle JMS Simulator - User Guide

## Overview

The Oracle JMS Simulator is a professional desktop application built with JavaFX that provides a user-friendly interface for interacting with Oracle JMS queues. It's designed for developers and testers who need to send and receive XML messages to/from Oracle JMS.

## User Interface Layout

### Window Layout

The application window is divided into four main sections:

```
┌─────────────────────────────────────────────────────────────┐
│  CONNECTION PANEL                                           │
│  ├─ JMS Provider URL, Connection Factory, Connection details│
│  └─ Connect/Disconnect buttons                             │
├─────────────────────────────────────────────────────────────┤
│  MAIN CONTENT (Split Panel)                                │
│  ┌──────────────────────┬──────────────────────────────┐   │
│  │  SEND MESSAGES       │  RECEIVE MESSAGES            │   │
│  │  ├─ Queue selector   │  ├─ Queue selector           │   │
│  │  ├─ XML input area   │  ├─ Received messages area   │   │
│  │  ├─ Validation       │  └─ Receive/Clear buttons    │   │
│  │  ├─ Action buttons   │                              │   │
│  │  └─ Message history  │                              │   │
│  └──────────────────────┴──────────────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│  STATUS BAR                                                 │
│  └─ Connection status and operation feedback               │
└─────────────────────────────────────────────────────────────┘
```

### Visual Design

- **Dark Theme**: Professional dark color scheme (#2b2b2b background, #1e1e1e panels)
- **Color-Coded Elements**:
  - Green (#4CAF50): Success states, Connect button
  - Red (#f44336): Error states, Disconnect button, Invalid XML
  - Blue (#2196F3): Information, Beautify button
  - Orange (#FF9800): Warning, Receive button
  - Gray (#757575): Neutral actions, Clear buttons

## Feature Details

### 1. Connection Management

**Location**: Top panel

**Elements**:
- Connection details display (read-only):
  - JMS Provider URL
  - Connection Factory name
  - Username
- Connect button (green) - Establishes connection to Oracle JMS
- Disconnect button (red) - Closes the connection

**Behavior**:
- Connect button is enabled when disconnected
- Disconnect button is enabled when connected
- Status bar updates with connection state

### 2. Send Messages Panel

**Location**: Left side of main content

**XML Input Area**:
- Large text area for entering XML content
- Monospace font (Courier New) for better XML readability
- Dark background (#1e1e1e) with light text
- Real-time validation as you type

**Validation Indicator**:
- Shows below the input area
- Green checkmark (✓) with "Valid XML" when XML is valid
- Red cross (✗) with error message when XML is invalid
- Empty when input area is empty

**Queue Selection**:
- Dropdown to select target queue
- Pre-populated with queues from application.properties
- Default: DEV.QUEUE.2 (out queue)

**Action Buttons**:
1. **Beautify XML** (Blue):
   - Formats XML with proper indentation
   - Always enabled (even when disconnected)
   - Shows error dialog if XML is malformed

2. **Send Message** (Green):
   - Sends XML to selected queue
   - Automatically disabled when:
     - Not connected to Oracle JMS
     - XML is invalid
   - Shows success/error status

3. **Clear** (Gray):
   - Clears the XML input area
   - Always enabled

**Message History**:
- List view showing sent messages
- Format: `→ YYYY-MM-DD HH:MM:SS [QUEUE_NAME] SENT`
- Scrollable list
- Clear History button to reset

### 3. Receive Messages Panel

**Location**: Right side of main content

**Received Messages Area**:
- Large read-only text area
- Displays received messages with formatting:
  ```
  ========================================
  Time: YYYY-MM-DD HH:MM:SS
  Queue: QUEUE_NAME
  Message ID: JMS_MESSAGE_ID
  ----------------------------------------
  [XML Content]
  ========================================
  ```
- Monospace font for XML readability
- Auto-scrolling to latest message

**Queue Selection**:
- Dropdown to select source queue
- Pre-populated with queues from application.properties
- Default: DEV.QUEUE.1 (in queue)

**Action Buttons**:
1. **Receive Messages** (Orange):
   - Polls the selected queue for messages
   - Retrieves up to 10 messages per click
   - Disabled when not connected
   - Shows count of received messages

2. **Clear** (Gray):
   - Clears the received messages display
   - Always enabled

### 4. Status Bar

**Location**: Bottom of window

**Display**:
- Current connection state:
  - Green text when connected: "Connected to [JMS_PROVIDER_URL]"
  - Red text when disconnected: "Disconnected"
- Operation feedback:
  - "Message sent successfully to [QUEUE]"
  - "Received N message(s) from [QUEUE]"
  - "XML beautified successfully"
  - Error messages for failed operations

## Usage Workflow

### Typical Usage Scenario

1. **Start Application**
   ```bash
   java -jar target/oracle-jms-simulator-1.0.0.jar
   ```

2. **Connect to Oracle JMS**
   - Verify connection details in the top panel
   - Click "Connect" button
   - Wait for status bar to show "Connected to t3://localhost:7001"

3. **Send a Message**
   - Paste or type XML in the left panel input area
   - Click "Beautify XML" to format (optional)
   - Verify green checkmark appears (✓ Valid XML)
   - Select target queue (default: DEV.QUEUE.2)
   - Click "Send Message"
   - Check status bar for confirmation
   - Message appears in history list

4. **Receive Messages**
   - Select source queue in right panel (default: DEV.QUEUE.1)
   - Click "Receive Messages"
   - Messages appear in the display area with timestamps
   - Check status bar for message count

5. **Disconnect**
   - Click "Disconnect" when done
   - Status bar shows "Disconnected"

## Sample XML Messages

The `sample-messages/` directory contains example XML files:

### Order Example
```xml
<?xml version="1.0" encoding="UTF-8"?>
<order>
    <orderId>ORD-12345</orderId>
    <customer>
        <customerId>CUST-001</customerId>
        <name>John Doe</name>
    </customer>
    <!-- ... -->
</order>
```

### Notification Example
```xml
<?xml version="1.0" encoding="UTF-8"?>
<notification>
    <messageId>MSG-98765</messageId>
    <type>email</type>
    <!-- ... -->
</notification>
```

## Error Handling

### Common Errors and Solutions

1. **Connection Failed**
   - Verify Oracle JMS server is running
   - Check connection details in application.properties
   - Ensure network connectivity to JMS server
   - Verify user credentials

2. **Invalid XML**
   - Send button automatically disabled
   - Error message shows specific parsing error
   - Use "Beautify XML" to help identify issues
   - Check for missing closing tags, invalid characters

3. **No Messages Received**
   - Verify correct queue name
   - Check if queue has messages
   - Ensure messages are not locked by other consumers
   - Verify queue permissions

4. **Send Failed**
   - Check connection status
   - Verify queue exists on server
   - Check queue permissions
   - Ensure queue is not full

## Keyboard Shortcuts

The application responds to standard text editing shortcuts in text areas:
- Ctrl+A: Select all
- Ctrl+C: Copy
- Ctrl+V: Paste
- Ctrl+X: Cut
- Ctrl+Z: Undo (in input areas)

## Configuration

Edit `src/main/resources/application.properties` before building:

```properties
# Oracle JMS Connection
oracle.jms.provider-url=t3://localhost:7001         # Provider URL (e.g., for WebLogic)
oracle.jms.connection-factory=weblogic.jms.ConnectionFactory  # Connection Factory JNDI name
oracle.jms.user=weblogic                            # Username
oracle.jms.password=welcome1                        # Password
oracle.jms.receive-timeout=5000                     # Timeout in milliseconds

# Queues
oracle.jms.queue.in=DEV.QUEUE.1                    # Input queue
oracle.jms.queue.out=DEV.QUEUE.2                   # Output queue
```

After changing configuration, rebuild the application:
```bash
mvn clean package
```

## Tips and Best Practices

1. **Use Beautify XML** before sending to ensure proper formatting
2. **Test with sample messages** first to verify connectivity
3. **Monitor the message history** to track sent messages
4. **Clear displays regularly** to avoid clutter
5. **Disconnect when done** to free up JMS connections
6. **Check status bar** for operation feedback
7. **Use proper XML** - invalid XML cannot be sent

## Troubleshooting

### Application won't start
- Verify Java 17+ is installed: `java -version`
- Check JavaFX is available in your Java installation
- Review application logs for errors

### UI appears blank or frozen
- Ensure display/X11 forwarding is configured (Linux)
- Try running with: `java -Dprism.verbose=true -jar target/oracle-jms-simulator-1.0.0.jar`

### Can't connect to Oracle JMS
- Test JMS connection with Oracle tools first
- Verify firewall settings
- Check JMS configuration
- Review JMS logs for connection attempts

## Advanced Features

### Message History
- Tracks both sent and received messages
- Uses arrows to indicate direction:
  - → : Sent message
  - ← : Received message
- Includes timestamp and queue name
- Persists during session (cleared on disconnect or manual clear)

### XML Validation
- Real-time validation as you type
- Specific error messages for parsing issues
- Validates XML structure and syntax
- Ensures well-formed XML before sending

### Connection State Management
- Automatic button state updates
- Prevents operations when disconnected
- Graceful connection handling
- Proper resource cleanup on disconnect

## Support

For issues or questions:
1. Check the README.md file
2. Review application logs
3. Verify Oracle JMS server status
4. Check configuration in application.properties

## Version Information

- Application Version: 1.0.0
- Spring Boot: 3.2.0
- JavaFX: 21
- Java: 17+

- netstat -an | findstr "7001 7101"
- netstat -an | grep "7001 7101"
- mvn clean package
- mvn spring-boot:run -Dspring-boot.run.mainClass=com.oraclejms.JmsConnectionTester
- java -cp target/oracle-jms-simulator-1.0.0.jar com.oraclejms.JmsConnectionTester