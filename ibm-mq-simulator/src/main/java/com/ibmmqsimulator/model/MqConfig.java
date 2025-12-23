package com.ibmmqsimulator.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ibm.mq")
public class MqConfig {
    private String queueManager;
    private String channel;
    private String connName;
    private String user;
    private String password;
    private int receiveTimeout;
    private QueueConfig queue;

    @Data
    public static class QueueConfig {
        private String in;
        private String out;
    }
}
