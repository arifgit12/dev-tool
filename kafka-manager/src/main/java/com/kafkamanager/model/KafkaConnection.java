package com.kafkamanager.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaConnection implements Serializable {
    private String id;
    private String name;
    private String bootstrapServers;
    private String securityProtocol;
    private String saslMechanism;
    private String saslUsername;
    private String saslPassword;
    private String sslTruststoreLocation;
    private String sslTruststorePassword;
    private String sslKeystoreLocation;
    private String sslKeystorePassword;
    private Map<String, String> additionalProperties;
    private boolean connected;

    public Map<String, Object> toPropertiesMap() {
        Map<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", bootstrapServers);

        if (securityProtocol != null && !securityProtocol.isEmpty()) {
            props.put("security.protocol", securityProtocol);
        }

        if (saslMechanism != null && !saslMechanism.isEmpty()) {
            props.put("sasl.mechanism", saslMechanism);
            if (saslUsername != null && saslPassword != null) {
                String jaasConfig = String.format(
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                    saslUsername, saslPassword
                );
                props.put("sasl.jaas.config", jaasConfig);
            }
        }

        if (sslTruststoreLocation != null && !sslTruststoreLocation.isEmpty()) {
            props.put("ssl.truststore.location", sslTruststoreLocation);
            props.put("ssl.truststore.password", sslTruststorePassword);
        }

        if (sslKeystoreLocation != null && !sslKeystoreLocation.isEmpty()) {
            props.put("ssl.keystore.location", sslKeystoreLocation);
            props.put("ssl.keystore.password", sslKeystorePassword);
        }

        if (additionalProperties != null) {
            props.putAll(additionalProperties);
        }

        return props;
    }
}
