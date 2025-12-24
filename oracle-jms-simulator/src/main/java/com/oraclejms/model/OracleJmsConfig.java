package com.oraclejms.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oracle.jms")
public class OracleJmsConfig {
    private String providerUrl;
    private String connectionFactory;
    private String user;
    private String password;
    private int receiveTimeout;
    private Queue queue;

    @Data
    public static class Queue {
        private String in;
        private String out;
    }
}
