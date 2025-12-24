package com.oraclejms.service;

import com.oraclejms.model.JmsConnectionConfig;
import com.oraclejms.repository.JmsConnectionConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
public class ConnectionConfigService {
    
    private final JmsConnectionConfigRepository repository;
    
    public ConnectionConfigService(JmsConnectionConfigRepository repository) {
        this.repository = repository;
    }
    
    /**
     * Save a connection configuration and mark it as default
     */
    @Transactional
    public JmsConnectionConfig saveConfiguration(String providerUrl, String connectionFactory, 
                                                   String username, String password) {
        // Clear existing default flag
        repository.findFirstByIsDefaultTrue().ifPresent(config -> {
            config.setDefault(false);
            repository.save(config);
        });
        
        // Create new configuration
        JmsConnectionConfig config = new JmsConnectionConfig();
        config.setProviderUrl(providerUrl);
        config.setConnectionFactory(connectionFactory);
        config.setUsername(username);
        config.setPassword(password);
        config.setDefault(true);
        
        JmsConnectionConfig saved = repository.save(config);
        log.info("Saved connection configuration: {}", saved.getId());
        return saved;
    }
    
    /**
     * Get the default (most recent) connection configuration
     */
    public Optional<JmsConnectionConfig> getDefaultConfiguration() {
        // First try to get explicitly marked default
        Optional<JmsConnectionConfig> defaultConfig = repository.findFirstByIsDefaultTrue();
        if (defaultConfig.isPresent()) {
            return defaultConfig;
        }
        
        // Otherwise get the most recently updated one
        return repository.findTopByOrderByUpdatedAtDesc();
    }
}
