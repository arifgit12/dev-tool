package com.ibmmqsimulator.service;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import com.ibmmqsimulator.model.MqMessage;
import com.ibmmqsimulator.util.TemplateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.jms.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service for dynamic IBM MQ connections with user-provided configuration
 */
@Slf4j
@Service
public class DynamicMqService {

    private Connection connection;
    private Session session;
    private final List<MqMessage> messageHistory;
    private boolean isConnected = false;
    
    // Dynamic configuration
    private String queueManager;
    private String channel;
    private String connName;
    private String user;
    private String password;

    public DynamicMqService() {
        this.messageHistory = new CopyOnWriteArrayList<>();
    }

    /**
     * Connect to IBM MQ with dynamic configuration
     */
    public void connect(String queueManager, String channel, String connName, String user, String password) throws JMSException {
        if (isConnected) {
            log.warn("Already connected to IBM MQ. Disconnecting first.");
            disconnect();
        }

        try {
            this.queueManager = queueManager;
            this.channel = channel;
            this.connName = connName;
            this.user = user;
            this.password = password;

            MQConnectionFactory factory = new MQConnectionFactory();
            factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
            factory.setQueueManager(queueManager);
            factory.setChannel(channel);
            factory.setConnectionNameList(connName);
            
            connection = factory.createConnection(user, password);
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            connection.start();
            
            isConnected = true;
            log.info("Connected to IBM MQ: {} via {}", queueManager, connName);
        } catch (JMSException e) {
            log.error("Failed to connect to IBM MQ", e);
            cleanup();
            throw e;
        }
    }

    public void disconnect() {
        cleanup();
        isConnected = false;
        log.info("Disconnected from IBM MQ");
    }

    /**
     * Test connection with provided configuration
     */
    public boolean testConnection(String queueManager, String channel, String connName, String user, String password) {
        try {
            MQConnectionFactory factory = new MQConnectionFactory();
            factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
            factory.setQueueManager(queueManager);
            factory.setChannel(channel);
            factory.setConnectionNameList(connName);
            
            Connection testConn = factory.createConnection(user, password);
            testConn.start();
            testConn.close();
            
            log.info("Connection test successful for: {}", queueManager);
            return true;
        } catch (JMSException e) {
            log.error("Connection test failed: {}", e.getMessage());
            return false;
        }
    }

    public void sendMessage(String queueName, String messageContent) throws JMSException {
        if (!isConnected) {
            throw new JMSException("Not connected to IBM MQ");
        }

        Queue queue = session.createQueue(queueName);
        MessageProducer producer = session.createProducer(queue);
        
        try {
            // Apply dynamic parameter substitution
            String processedContent = TemplateUtil.replacePlaceholders(messageContent);
            
            TextMessage message = session.createTextMessage(processedContent);
            producer.send(message);
            
            MqMessage mqMessage = MqMessage.builder()
                    .messageId(message.getJMSMessageID())
                    .content(processedContent)
                    .queue(queueName)
                    .timestamp(LocalDateTime.now())
                    .type(MqMessage.MessageType.SENT)
                    .build();
            
            messageHistory.add(mqMessage);
            log.info("Message sent to queue: {}", queueName);
        } finally {
            producer.close();
        }
    }

    public List<MqMessage> receiveMessages(String queueName, int maxMessages) throws JMSException {
        if (!isConnected) {
            throw new JMSException("Not connected to IBM MQ");
        }

        List<MqMessage> messages = new ArrayList<>();
        Queue queue = session.createQueue(queueName);
        MessageConsumer consumer = session.createConsumer(queue);

        try {
            for (int i = 0; i < maxMessages; i++) {
                Message message = consumer.receive(1000); // 1 second timeout
                if (message == null) {
                    break;
                }

                if (message instanceof TextMessage) {
                    TextMessage textMessage = (TextMessage) message;
                    MqMessage mqMessage = MqMessage.builder()
                            .messageId(textMessage.getJMSMessageID())
                            .content(textMessage.getText())
                            .queue(queueName)
                            .timestamp(LocalDateTime.now())
                            .type(MqMessage.MessageType.RECEIVED)
                            .build();
                    
                    messages.add(mqMessage);
                    messageHistory.add(mqMessage);
                }
            }
            
            log.info("Received {} messages from queue: {}", messages.size(), queueName);
            return messages;
        } finally {
            consumer.close();
        }
    }

    public List<MqMessage> getMessageHistory() {
        return new ArrayList<>(messageHistory);
    }

    public void clearHistory() {
        messageHistory.clear();
    }

    public boolean isConnected() {
        return isConnected;
    }

    public String getQueueManager() {
        return queueManager;
    }

    private void cleanup() {
        try {
            if (session != null) {
                session.close();
            }
        } catch (JMSException e) {
            log.warn("Error closing session", e);
        }

        try {
            if (connection != null) {
                connection.close();
            }
        } catch (JMSException e) {
            log.warn("Error closing connection", e);
        }

        session = null;
        connection = null;
    }
}
