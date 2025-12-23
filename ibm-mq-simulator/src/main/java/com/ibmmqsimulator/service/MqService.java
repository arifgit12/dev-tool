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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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

    /**
     * Sends multiple messages using multiple threads for fast delivery
     * @param queueName Target queue name
     * @param messageContent XML message content
     * @param messageCount Number of messages to send (1-20000)
     * @param threadCount Number of threads to use (1-100)
     * @param progressCallback Callback for progress updates (sent, total)
     */
    public void sendMessagesMultiThreaded(String queueName, String messageContent, 
                                          int messageCount, int threadCount,
                                          ProgressCallback progressCallback) throws JMSException, InterruptedException {
        if (!isConnected) {
            throw new JMSException("Not connected to IBM MQ");
        }

        // Validate input and make final for lambda usage
        final int finalMessageCount = Math.max(1, Math.min(20000, messageCount));
        final int finalThreadCount = Math.max(1, Math.min(100, threadCount));
        
        ExecutorService executorService = Executors.newFixedThreadPool(finalThreadCount);
        AtomicInteger sentCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(finalMessageCount);
        
        // History sample interval constant
        final int HISTORY_SAMPLE_INTERVAL = 10;
        
        log.info("Starting to send {} messages using {} threads to queue: {}", 
                 finalMessageCount, finalThreadCount, queueName);
        
        for (int i = 0; i < finalMessageCount; i++) {
            final int messageNum = i + 1;
            executorService.submit(() -> {
                Session threadSession = null;
                MessageProducer producer = null;
                try {
                    // Synchronized session creation for thread safety
                    synchronized (connection) {
                        threadSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    }
                    Queue queue = threadSession.createQueue(queueName);
                    producer = threadSession.createProducer(queue);
                    
                    TextMessage message = threadSession.createTextMessage(messageContent);
                    message.setIntProperty("messageNumber", messageNum);
                    producer.send(message);
                    
                    int sent = sentCount.incrementAndGet();
                    
                    // Add to history (sample every HISTORY_SAMPLE_INTERVAL messages to avoid overwhelming the UI)
                    if (messageNum % HISTORY_SAMPLE_INTERVAL == 0 || messageNum == finalMessageCount) {
                        MqMessage mqMessage = MqMessage.builder()
                                .messageId(message.getJMSMessageID())
                                .content(String.format("[Batch %d/%d] %s", messageNum, finalMessageCount, 
                                        messageContent.substring(0, Math.min(50, messageContent.length())) + "..."))
                                .queue(queueName)
                                .timestamp(LocalDateTime.now())
                                .type(MqMessage.MessageType.SENT)
                                .build();
                        messageHistory.add(mqMessage);
                    }
                    
                    // Report progress
                    if (progressCallback != null) {
                        progressCallback.onProgress(sent, finalMessageCount);
                    }
                    
                } catch (JMSException e) {
                    log.error("Failed to send message {}: {}", messageNum, e.getMessage());
                    errorCount.incrementAndGet();
                } finally {
                    // Clean up resources
                    try {
                        if (producer != null) producer.close();
                    } catch (JMSException e) {
                        log.warn("Error closing producer: {}", e.getMessage());
                    }
                    try {
                        if (threadSession != null) threadSession.close();
                    } catch (JMSException e) {
                        log.warn("Error closing session: {}", e.getMessage());
                    }
                    latch.countDown();
                }
            });
        }
        
        // Wait for all messages to be sent - calculate timeout based on message count
        latch.await();
        executorService.shutdown();
        
        // Timeout: 1 minute base + 2 seconds per 1000 messages
        long timeoutSeconds = 60 + (finalMessageCount / 1000) * 2;
        boolean terminated = executorService.awaitTermination(timeoutSeconds, TimeUnit.SECONDS);
        
        if (!terminated) {
            log.warn("Executor service did not terminate within {} seconds", timeoutSeconds);
            executorService.shutdownNow();
        }
        
        log.info("Completed sending messages. Success: {}, Errors: {}", 
                 sentCount.get(), errorCount.get());
        
        if (errorCount.get() > 0) {
            throw new JMSException(String.format("Failed to send %d messages out of %d", 
                                                 errorCount.get(), finalMessageCount));
        }
    }

    /**
     * Callback interface for progress updates
     */
    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(int sent, int total);
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
