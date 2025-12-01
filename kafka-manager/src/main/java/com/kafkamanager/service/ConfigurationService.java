package com.kafkamanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafkamanager.model.KafkaConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ConfigurationService {

    private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".kafka-manager";
    private static final String CONNECTIONS_FILE = CONFIG_DIR + File.separator + "connections.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConfigurationService() {
        File configDir = new File(CONFIG_DIR);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
    }

    public void saveConnections(List<KafkaConnection> connections) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(CONNECTIONS_FILE), connections);
            log.info("Saved {} connections to {}", connections.size(), CONNECTIONS_FILE);
        } catch (IOException e) {
            log.error("Failed to save connections", e);
        }
    }

    public List<KafkaConnection> loadConnections() {
        File file = new File(CONNECTIONS_FILE);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try {
            List<KafkaConnection> connections = objectMapper.readValue(file,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, KafkaConnection.class));
            log.info("Loaded {} connections from {}", connections.size(), CONNECTIONS_FILE);
            return connections;
        } catch (IOException e) {
            log.error("Failed to load connections", e);
            return new ArrayList<>();
        }
    }
}
