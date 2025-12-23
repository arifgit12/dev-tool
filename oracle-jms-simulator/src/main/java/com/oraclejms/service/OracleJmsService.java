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

    public void connect() throws JMSException {
        try {
            // Set up JNDI context
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
            env.put(Context.PROVIDER_URL, config.getProviderUrl());
            
            if (config.getUser() != null && !config.getUser().isEmpty()) {
                env.put(Context.SECURITY_PRINCIPAL, config.getUser());
                env.put(Context.SECURITY_CREDENTIALS, config.getPassword());
            }

            InitialContext ctx = new InitialContext(env);
            
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

        try {
            // Look up the queue
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
            env.put(Context.PROVIDER_URL, config.getProviderUrl());
            if (config.getUser() != null && !config.getUser().isEmpty()) {
                env.put(Context.SECURITY_PRINCIPAL, config.getUser());
                env.put(Context.SECURITY_CREDENTIALS, config.getPassword());
            }

            InitialContext ctx = new InitialContext(env);
            Queue queue = (Queue) ctx.lookup(queueName);
            
            MessageProducer producer = session.createProducer(queue);
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
            
            producer.close();
            ctx.close();
            
            log.info("Sent message to queue: {}", queueName);
        } catch (NamingException e) {
            log.error("Failed to send message", e);
            throw new JMSException("Failed to send message: " + e.getMessage());
        }
    }

    public List<JmsMessage> receiveMessages(String queueName, int maxMessages) throws JMSException {
        if (!connected) {
            throw new JMSException("Not connected to Oracle JMS");
        }

        List<JmsMessage> messages = new ArrayList<>();
        
        try {
            // Look up the queue
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
            env.put(Context.PROVIDER_URL, config.getProviderUrl());
            if (config.getUser() != null && !config.getUser().isEmpty()) {
                env.put(Context.SECURITY_PRINCIPAL, config.getUser());
                env.put(Context.SECURITY_CREDENTIALS, config.getPassword());
            }

            InitialContext ctx = new InitialContext(env);
            Queue queue = (Queue) ctx.lookup(queueName);
            
            MessageConsumer consumer = session.createConsumer(queue);
            
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
            
            consumer.close();
            ctx.close();
            
            log.info("Received {} messages from queue: {}", messages.size(), queueName);
        } catch (NamingException e) {
            log.error("Failed to receive messages", e);
            throw new JMSException("Failed to receive messages: " + e.getMessage());
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
