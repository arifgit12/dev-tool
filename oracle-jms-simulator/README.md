# Oracle JMS Simulator

A professional JavaFX-based desktop application for interacting with Oracle JMS (Java Message Service) queues. This tool provides a user-friendly interface for sending and receiving XML messages to/from Oracle WebLogic JMS.

## Features

- **Professional Dark-Themed UI**: Modern, easy-on-the-eyes interface
- **Oracle JMS Integration**: Connect to Oracle WebLogic JMS servers
- **Editable Connection Configuration**: Edit connection details in the UI
- **Configuration Persistence**: Save connection settings to H2 database
- **Test Connection**: Validate connection before connecting
- **Batch Message Sending**: Send 1-20000 messages with multi-threading
- **Dynamic Template Parameters**: Generate unique data for each message
- **XML Message Support**: Send and receive XML messages
- **Real-time XML Validation**: Validates XML as you type
- **XML Beautification**: Format XML with proper indentation
- **Message History**: Track sent and received messages
- **Flexible Queue Management**: Editable queue names
- **Connection Management**: Easy connect/disconnect functionality

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Access to an Oracle WebLogic JMS server
- **Oracle WebLogic Client JAR** (wlfullclient.jar or wlthint3client.jar)

## Quick Start - WebLogic Client Setup

The Oracle JMS Simulator requires the WebLogic client library. Follow these simple steps:

### Step 1: Obtain the WebLogic Client JAR

Choose one option:

**Option A: Full Client (Recommended)**
```bash
cd $WL_HOME/server/lib
java -jar wljarbuilder.jar
# This creates wlfullclient.jar
```

**Option B: Thin Client (Smaller)**
```bash
# The thin client is already available at:
# $WL_HOME/server/lib/wlthint3client.jar
```

### Step 2: Place JAR in lib Directory

Copy the WebLogic client JAR to the `lib` directory:

```bash
# For full client:
cp $WL_HOME/server/lib/wlfullclient.jar oracle-jms-simulator/lib/

# OR for thin client:
cp $WL_HOME/server/lib/wlthint3client.jar oracle-jms-simulator/lib/
```

**That's it!** The build process will automatically include the JAR from the `lib` directory.

### Step 3: Build the Application

```bash
cd oracle-jms-simulator
mvn clean package
```

The WebLogic client JAR will be automatically included in the packaged application.

## Alternative: Maven Local Repository Installation

If you prefer to install the WebLogic JAR to your local Maven repository instead:

1. Install the JAR:
   <dependency>
       <groupId>com.oracle.weblogic</groupId>
       <artifactId>wlfullclient</artifactId>
       <version>14.1.1.0</version>
   </dependency>
   ```

   **For wlthint3client.jar:**
   ```xml
   <dependency>
       <groupId>com.oracle.weblogic</groupId>
       <artifactId>wlthint3client</artifactId>
       <version>14.1.1.0</version>
   </dependency>
   ```

2. Rebuild the application:
   ```bash
   mvn clean package
   ```

## Installation

1. Clone the repository
2. Navigate to the oracle-jms-simulator directory:
   ```bash
   cd oracle-jms-simulator
   ```

3. **Install WebLogic client JAR** (see section above)

4. Configure your Oracle JMS connection in `src/main/resources/application.properties`:
   ```properties
   oracle.jms.provider-url=t3://localhost:7001
   oracle.jms.connection-factory=weblogic.jms.ConnectionFactory
   oracle.jms.user=weblogic
   oracle.jms.password=welcome1
   oracle.jms.queue.in=DEV.QUEUE.1
   oracle.jms.queue.out=DEV.QUEUE.2
   ```

5. Build the application:
   ```bash
   mvn clean package
   ```

## Running the Application

### Using Maven:
```bash
mvn javafx:run
```

### Using the JAR file:
```bash
java -jar target/oracle-jms-simulator-1.0.0.jar
```

### Using the run scripts:
- Linux/Mac: `./run.sh`
- Windows: `run.bat`

## Quick Start

1. **Start the application** using one of the methods above
2. **Configure connection** details in application.properties (if not already done)
3. **Click Connect** to establish a connection to Oracle JMS
4. **Send a message**:
   - Enter or paste XML in the left panel
   - Click "Beautify XML" to format (optional)
   - Click "Send Message"
5. **Receive messages**:
   - Click "Receive Messages" in the right panel
   - Messages will appear with timestamps and details

## Configuration

The application uses Spring Boot configuration. Edit `src/main/resources/application.properties`:

```properties
# Oracle JMS Connection
oracle.jms.provider-url=t3://localhost:7001
oracle.jms.connection-factory=weblogic.jms.ConnectionFactory
oracle.jms.user=weblogic
oracle.jms.password=welcome1
oracle.jms.receive-timeout=5000

# Queues
oracle.jms.queue.in=DEV.QUEUE.1
oracle.jms.queue.out=DEV.QUEUE.2
```

## Sample Messages

Sample XML messages are provided in the `sample-messages/` directory:
- `order.xml` - Example order message
- `notification.xml` - Example notification message

## User Guide

For detailed usage instructions, see [USER_GUIDE.md](USER_GUIDE.md)

## Technology Stack

- **JavaFX 21** - Modern Java UI framework
- **Spring Boot 3.2.0** - Application framework
- **Jakarta JMS API** - JMS standard API
- **Lombok** - Reduce boilerplate code
- **Maven** - Build and dependency management

## Project Structure

```
oracle-jms-simulator/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── oraclejms/
│       │           ├── model/          # Data models
│       │           ├── service/        # Business logic
│       │           ├── ui/             # JavaFX UI components
│       │           └── util/           # Utility classes
│       └── resources/
│           └── application.properties  # Configuration
├── sample-messages/                    # Example XML files
├── pom.xml                            # Maven configuration
├── README.md                          # This file
└── USER_GUIDE.md                      # Detailed user guide
```

## Troubleshooting

### NoInitialContextException: Cannot instantiate class: weblogic.jndi.WLInitialContextFactory

**Error Message:**
```
javax.naming.NoInitialContextException: Cannot instantiate class: weblogic.jndi.WLInitialContextFactory
```

**Cause:** The WebLogic client JAR is not in the classpath.

**Solution:**
1. Follow the "WebLogic Client Installation" section above to install the WebLogic client JAR
2. Add the dependency to `pom.xml` (see WebLogic Client Installation section)
3. Rebuild the application: `mvn clean package`

### Connection Issues
- Verify Oracle WebLogic server is running
- Check the provider URL in application.properties or in the UI
- Ensure network connectivity to the JMS server
- Verify credentials are correct
- Use the "Test Connection" button to validate settings before connecting

### Build Issues
- Ensure Java 17+ is installed: `java -version`
- Ensure Maven is installed: `mvn -version`
- Clear Maven cache: `mvn clean`
- Verify WebLogic client JAR is installed in local Maven repository

### Runtime Issues
- Check JavaFX is available in your Java installation
- Review logs in the console for error details
- Ensure WebLogic client JAR is included in the packaged JAR

## Contributing

Contributions are welcome! Please feel free to submit issues or pull requests.

## License

See the LICENSE file in the root directory for license information.

## Support

For questions or issues:
1. Check the [USER_GUIDE.md](USER_GUIDE.md)
2. Review application logs
3. Verify Oracle JMS server configuration
4. Check connection settings in application.properties
