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

    public OracleJmsService(OracleJmsConfig config) {
        this.config = config;
    }

    /**
     * Creates JNDI context with configured settings
     */
    private InitialContext createJndiContext() throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
        env.put(Context.PROVIDER_URL, config.getProviderUrl());
        
        if (config.getUser() != null && !config.getUser().isEmpty()) {
            env.put(Context.SECURITY_PRINCIPAL, config.getUser());
            env.put(Context.SECURITY_CREDENTIALS, config.getPassword());
        }
        
        return new InitialContext(env);
    }

    public void connect() throws JMSException {
        try {
            InitialContext ctx = createJndiContext();
            
            // Look up connection factory
            ConnectionFactory connectionFactory = (ConnectionFactory) ctx.lookup(config.getConnectionFactory());
            
            // Create connection
            if (config.getUser() != null && !config.getUser().isEmpty()) {
                connection = connectionFactory.createConnection(config.getUser(), config.getPassword());
            } else {
                connection = connectionFactory.createConnection();
            }
            
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            connection.start();
            connected = true;
            
            log.info("Connected to Oracle JMS at {}", config.getProviderUrl());
            
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
