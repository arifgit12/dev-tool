package com.ibmmqsimulator.service;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import com.ibmmqsimulator.model.MqConfig;
import com.ibmmqsimulator.model.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.jms.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class MqService {

    private final MqConfig mqConfig;
    private Connection connection;
    private Session session;
    private final List<MqMessage> messageHistory;
    private MessageConsumer consumer;
    private boolean isConnected = false;

    public MqService(MqConfig mqConfig) {
        this.mqConfig = mqConfig;
        this.messageHistory = new CopyOnWriteArrayList<>();
    }

    public void connect() throws JMSException {
        if (isConnected) {
            log.warn("Already connected to IBM MQ");
            return;
        }

        try {
            MQConnectionFactory factory = new MQConnectionFactory();
            factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
            factory.setQueueManager(mqConfig.getQueueManager());
            factory.setChannel(mqConfig.getChannel());
            factory.setConnectionNameList(mqConfig.getConnName());
            
            connection = factory.createConnection(mqConfig.getUser(), mqConfig.getPassword());
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            connection.start();
            
            isConnected = true;
            log.info("Connected to IBM MQ: {}", mqConfig.getQueueManager());
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

    public void sendMessage(String queueName, String messageContent) throws JMSException {
        if (!isConnected) {
            throw new JMSException("Not connected to IBM MQ");
        }

        Queue queue = session.createQueue(queueName);
        MessageProducer producer = session.createProducer(queue);
        
        try {
            TextMessage message = session.createTextMessage(messageContent);
            producer.send(message);
            
            MqMessage mqMessage = MqMessage.builder()
                    .messageId(message.getJMSMessageID())
                    .content(messageContent)
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
                Message message = consumer.receive(mqConfig.getReceiveTimeout());
                if (message == null) {
                    break;
                }
                
                if (message instanceof TextMessage) {
                    TextMessage textMessage = (TextMessage) message;
                    MqMessage mqMessage = MqMessage.builder()
                            .messageId(message.getJMSMessageID())
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
        } finally {
            consumer.close();
        }
        
        return messages;
    }

    public void startListening(String queueName, MessageListener listener) throws JMSException {
        if (!isConnected) {
            throw new JMSException("Not connected to IBM MQ");
        }

        stopListening();
        
        Queue queue = session.createQueue(queueName);
        consumer = session.createConsumer(queue);
        consumer.setMessageListener(listener);
        log.info("Started listening on queue: {}", queueName);
    }

    public void stopListening() throws JMSException {
        if (consumer != null) {
            consumer.close();
            consumer = null;
            log.info("Stopped listening");
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

    public String getInQueue() {
        return mqConfig.getQueue().getIn();
    }

    public String getOutQueue() {
        return mqConfig.getQueue().getOut();
    }

    private void cleanup() {
        try {
            if (consumer != null) {
                consumer.close();
                consumer = null;
            }
        } catch (Exception e) {
            log.warn("Error closing consumer", e);
        }

        try {
            if (session != null) {
                session.close();
                session = null;
            }
        } catch (Exception e) {
            log.warn("Error closing session", e);
        }

        try {
            if (connection != null) {
                connection.close();
                connection = null;
            }
        } catch (Exception e) {
            log.warn("Error closing connection", e);
        }
    }
}
