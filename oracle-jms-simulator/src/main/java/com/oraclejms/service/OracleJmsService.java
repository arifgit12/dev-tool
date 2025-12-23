package com.oraclejms.service;

import com.oraclejms.model.JmsMessage;
import com.oraclejms.model.OracleJmsConfig;
import jakarta.jms.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

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
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
        env.put(Context.PROVIDER_URL, currentProviderUrl);
        
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
        InitialContext ctx = null;
        Connection testConnection = null;
        try {
            ctx = createJndiContext();
            
            // Look up connection factory
            ConnectionFactory connectionFactory = (ConnectionFactory) ctx.lookup(currentConnectionFactory);
            
            // Create test connection
            if (currentUser != null && !currentUser.isEmpty()) {
                testConnection = connectionFactory.createConnection(currentUser, currentPassword);
            } else {
                testConnection = connectionFactory.createConnection();
            }
            
            testConnection.start();
            log.info("Connection test successful to {}", currentProviderUrl);
            return true;
        } catch (NamingException | JMSException e) {
            log.error("Connection test failed", e);
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
            throw new JMSException("Failed to connect: " + e.getMessage());
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
}
