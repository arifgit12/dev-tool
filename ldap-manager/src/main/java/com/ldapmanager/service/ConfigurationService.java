package com.ldapmanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ldapmanager.model.LdapConnection;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
public class ConfigurationService {
    private static final String CONFIG_FILE = System.getProperty("user.home") +
        File.separator + ".ldap-manager" + File.separator + "connections.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<LdapConnection> loadConnections() {
        try {
            File configFile = new File(CONFIG_FILE);
            if (!configFile.exists()) {
                return new ArrayList<>();
            }
            LdapConnection[] connections = objectMapper.readValue(configFile, LdapConnection[].class);
            return new ArrayList<>(Arrays.asList(connections));
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void saveConnections(List<LdapConnection> connections) {
        try {
            File configFile = new File(CONFIG_FILE);
            configFile.getParentFile().mkdirs();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(configFile, connections);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveConnection(LdapConnection connection) {
        List<LdapConnection> connections = loadConnections();

        if (connection.getId() == null) {
            connection.setId(UUID.randomUUID().toString());
        }

        connections.removeIf(c -> c.getId().equals(connection.getId()));
        connections.add(connection);
        saveConnections(connections);
    }

    public void deleteConnection(String id) {
        List<LdapConnection> connections = loadConnections();
        connections.removeIf(c -> c.getId().equals(id));
        saveConnections(connections);
    }

    public LdapConnection getConnection(String id) {
        return loadConnections().stream()
            .filter(c -> c.getId().equals(id))
            .findFirst()
            .orElse(null);
    }
}
