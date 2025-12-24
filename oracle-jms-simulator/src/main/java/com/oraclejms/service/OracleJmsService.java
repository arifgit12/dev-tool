package com.oraclejms.service;

import com.oraclejms.model.JmsMessage;
import com.oraclejms.model.OracleJmsConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class OracleJmsService {

    private final OracleJmsConfig config;
    private Connection connection;
    private Session session;
    private boolean connected = false;
    private final List<JmsMessage> messageHistory = new ArrayList<>();
    
    // Current connection parameters (can be overridden)
    private String currentProviderUrl;
    private String currentConnectionFactory;
    private String currentUser;
    private String currentPassword;

    public OracleJmsService(OracleJmsConfig config) {
        this.config = config;
        // Initialize with default config values
        this.currentProviderUrl = config.getProviderUrl();
        this.currentConnectionFactory = config.getConnectionFactory();
        this.currentUser = config.getUser();
        this.currentPassword = config.getPassword();
    }
    
    /**
     * Update connection parameters without connecting
     */
    public void updateConnectionParameters(String providerUrl, String connectionFactory, 
                                           String username, String password) {
        this.currentProviderUrl = providerUrl;
        this.currentConnectionFactory = connectionFactory;
        this.currentUser = username;
        this.currentPassword = password;
    }
    
    /**
     * Get current provider URL
     */
    public String getCurrentProviderUrl() {
        return currentProviderUrl;
    }

    /**
     * Creates JNDI context with configured settings
     */
    private InitialContext createJndiContext() throws NamingException {
        // Step 2: Create JNDI context
        System.out.println("\n[2/6] Creating JNDI context...");

        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
        env.put(Context.PROVIDER_URL, currentProviderUrl);

        // WebLogic-specific timeout settings (in milliseconds)
        env.put("weblogic.jndi.connectTimeout", "60000");  // 60 seconds connection timeout
        env.put("weblogic.jndi.responseReadTimeout", "60000");  // 60 seconds read timeout

        if (currentUser != null && !currentUser.isEmpty()) {
            env.put(Context.SECURITY_PRINCIPAL, currentUser);
            env.put(Context.SECURITY_CREDENTIALS, currentPassword);
        }


        return new InitialContext(env);
    }
    
    /**
     * Test connection with current parameters
     */
    public boolean testConnection() {

        // Step 1: Verify WebLogic classes
        System.out.println("\n[1/6] Verifying WebLogic classes...");
        verifyWebLogicClasses();
        System.out.println("✓ WebLogic classes found");

        InitialContext ctx = null;
        Connection testConnection = null;
        try {
            ctx = createJndiContext();
            System.out.println("✓ JNDI context created successfully");

            // Step 3: Lookup connection factory
            System.out.println("\n[3/6] Looking up connection factory: " + config.getConnectionFactory());
            // Look up connection factory
            ConnectionFactory connectionFactory = (ConnectionFactory) ctx.lookup(currentConnectionFactory);
            System.out.println("✓ Connection factory found: " + connectionFactory.getClass().getName());

            // Step 4: Create connection
            System.out.println("\n[4/6] Creating JMS connection...");
            // Create test connection
            if (currentUser != null && !currentUser.isEmpty()) {
                testConnection = connectionFactory.createConnection(currentUser, currentPassword);
            } else {
                testConnection = connectionFactory.createConnection();
            }
            System.out.println("✓ Connection created: " + testConnection.getClass().getName());

            // Step 5: Start connection
            System.out.println("\n[5/6] Starting connection...");
            testConnection.start();
            System.out.println("✓ Connection started successfully");
            log.info("Connection test successful to {}", currentProviderUrl);
            return true;
        } catch (NamingException e) {
            if (e.getMessage() != null && e.getMessage().contains("Cannot instantiate class")) {
                log.error("WebLogic JNDI classes not available. Please ensure WebLogic client JAR is in the classpath. " +
                         "See README.md for installation instructions.", e);
            } else {
                log.error("JNDI lookup failed: {}", e.getMessage(), e);
            }
            return false;
        } catch (JMSException e) {
            log.error("JMS connection failed: {}", e.getMessage(), e);
            return false;
        } finally {
            if (testConnection != null) {
                try {
                    testConnection.close();
                } catch (JMSException e) {
                    log.warn("Error closing test connection", e);
                }
            }
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    log.warn("Error closing test context", e);
                }
            }
        }
    }

    public void connect() throws JMSException {
        try {
            InitialContext ctx = createJndiContext();
            
            // Look up connection factory
            ConnectionFactory connectionFactory = (ConnectionFactory) ctx.lookup(currentConnectionFactory);
            
            // Create connection
            if (currentUser != null && !currentUser.isEmpty()) {
                connection = connectionFactory.createConnection(currentUser, currentPassword);
            } else {
                connection = connectionFactory.createConnection();
            }
            
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            connection.start();
            connected = true;
            
            log.info("Connected to Oracle JMS at {}", currentProviderUrl);
            
            ctx.close();
        } catch (NamingException e) {
            log.error("Failed to connect to Oracle JMS", e);
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("Cannot instantiate class")) {
                throw new JMSException("WebLogic client library not found. Please install wlfullclient.jar or wlthint3client.jar. See README.md for instructions.");
            }
            throw new JMSException("Failed to connect: " + errorMsg);
        }
    }

    public void disconnect() {
        try {
            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
            }
            connected = false;
            log.info("Disconnected from Oracle JMS");
        } catch (JMSException e) {
            log.error("Error during disconnect", e);
        }
    }

    public void sendMessage(String queueName, String xmlContent) throws JMSException {
        if (!connected) {
            throw new JMSException("Not connected to Oracle JMS");
        }

        InitialContext ctx = null;
        MessageProducer producer = null;
        
        try {
            ctx = createJndiContext();
            Queue queue = (Queue) ctx.lookup(queueName);
            
            producer = session.createProducer(queue);
            TextMessage message = session.createTextMessage(xmlContent);
            producer.send(message);
            
            // Add to history
            JmsMessage jmsMessage = new JmsMessage(
                message.getJMSMessageID(),
                queueName,
                xmlContent,
                LocalDateTime.now(),
                JmsMessage.MessageType.SENT
            );
            messageHistory.add(jmsMessage);
            
            log.info("Sent message to queue: {}", queueName);
        } catch (NamingException e) {
            log.error("Failed to send message", e);
            throw new JMSException("Failed to send message: " + e.getMessage());
        } finally {
            if (producer != null) {
                try {
                    producer.close();
                } catch (JMSException e) {
                    log.warn("Error closing producer", e);
                }
            }
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    log.warn("Error closing context", e);
                }
            }
        }
    }

    public List<JmsMessage> receiveMessages(String queueName, int maxMessages) throws JMSException {
        if (!connected) {
            throw new JMSException("Not connected to Oracle JMS");
        }

        List<JmsMessage> messages = new ArrayList<>();
        InitialContext ctx = null;
        MessageConsumer consumer = null;
        
        try {
            ctx = createJndiContext();
            Queue queue = (Queue) ctx.lookup(queueName);
            
            consumer = session.createConsumer(queue);
            
            for (int i = 0; i < maxMessages; i++) {
                Message message = consumer.receive(config.getReceiveTimeout());
                if (message == null) {
                    break;
                }
                
                if (message instanceof TextMessage) {
                    TextMessage textMessage = (TextMessage) message;
                    JmsMessage jmsMessage = new JmsMessage(
                        message.getJMSMessageID(),
                        queueName,
                        textMessage.getText(),
                        LocalDateTime.now(),
                        JmsMessage.MessageType.RECEIVED
                    );
                    messages.add(jmsMessage);
                    messageHistory.add(jmsMessage);
                }
            }
            
            log.info("Received {} messages from queue: {}", messages.size(), queueName);
        } catch (NamingException e) {
            log.error("Failed to receive messages", e);
            throw new JMSException("Failed to receive messages: " + e.getMessage());
        } finally {
            if (consumer != null) {
                try {
                    consumer.close();
                } catch (JMSException e) {
                    log.warn("Error closing consumer", e);
                }
            }
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    log.warn("Error closing context", e);
                }
            }
        }
        
        return messages;
    }

    public boolean isConnected() {
        return connected;
    }

    public List<JmsMessage> getMessageHistory() {
        return new ArrayList<>(messageHistory);
    }

    public void clearHistory() {
        messageHistory.clear();
    }
    
    /**
     * Sends multiple messages with template processing in parallel using threading
     * @param queueName Target queue name
     * @param xmlTemplate XML template with placeholders
     * @param messageCount Number of messages to send (1-20000)
     * @param progressCallback Callback for progress updates (sent count, total count)
     * @throws JMSException if sending fails
     */
    public void sendMessagesAsync(String queueName, String xmlTemplate, int messageCount, 
                                   java.util.function.BiConsumer<Integer, Integer> progressCallback) throws JMSException {
        if (!connected) {
            throw new JMSException("Not connected to Oracle JMS");
        }
        
        if (messageCount < 1 || messageCount > 20000) {
            throw new JMSException("Message count must be between 1 and 20000");
        }
        
        // Use thread pool for parallel sending
        int threadCount = Math.min(10, Math.max(1, messageCount / 100)); // 1-10 threads based on load
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        java.util.concurrent.atomic.AtomicInteger sentCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger errorCount = new java.util.concurrent.atomic.AtomicInteger(0);
        
        try {
            InitialContext ctx = createJndiContext();
            Queue queue = (Queue) ctx.lookup(queueName);
            
            for (int i = 0; i < messageCount; i++) {
                final int messageNum = i + 1;
                executor.submit(() -> {
                    try {
                        // Process template for this message
                        String processedXml = com.oraclejms.util.TemplateUtil.processTemplate(xmlTemplate);
                        
                        // Create producer for this thread
                        MessageProducer producer = session.createProducer(queue);
                        TextMessage message = session.createTextMessage(processedXml);
                        producer.send(message);
                        producer.close();
                        
                        int sent = sentCount.incrementAndGet();
                        
                        // Update progress every 10 messages or on last message
                        if (sent % 10 == 0 || sent == messageCount) {
                            if (progressCallback != null) {
                                progressCallback.accept(sent, messageCount);
                            }
                        }
                        
                        log.debug("Sent message {}/{} to queue: {}", sent, messageCount, queueName);
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        log.error("Failed to send message {}/{}", messageNum, messageCount, e);
                    }
                });
            }
            
            ctx.close();
            
            // Wait for all messages to complete
            executor.shutdown();
            if (!executor.awaitTermination(300, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
                throw new JMSException("Timeout waiting for messages to send");
            }
            
            int sent = sentCount.get();
            int errors = errorCount.get();
            
            // Add summary to history
            if (sent > 0) {
                JmsMessage summaryMessage = new JmsMessage(
                    "BATCH-" + UUID.randomUUID().toString().substring(0, 8),
                    queueName,
                    String.format("Sent %d messages (%d errors)", sent, errors),
                    LocalDateTime.now(),
                    JmsMessage.MessageType.SENT
                );
                messageHistory.add(summaryMessage);
            }
            
            log.info("Batch send completed: {} messages sent, {} errors", sent, errors);
            
            if (errors > 0) {
                throw new JMSException(String.format("Sent %d messages with %d errors", sent, errors));
            }
            
        } catch (NamingException e) {
            log.error("Failed to send messages", e);
            throw new JMSException("Failed to send messages: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JMSException("Message sending interrupted: " + e.getMessage());
        }
    }

    private void verifyWebLogicClasses() {
        try {
            Class.forName("weblogic.jndi.WLInitialContextFactory");
            Class.forName("weblogic.jms.client.JMSConnectionFactory");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }
}
