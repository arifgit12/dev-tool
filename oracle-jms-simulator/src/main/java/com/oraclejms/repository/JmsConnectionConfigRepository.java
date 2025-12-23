package com.oraclejms.repository;

import com.oraclejms.model.JmsConnectionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JmsConnectionConfigRepository extends JpaRepository<JmsConnectionConfig, Long> {
    
    Optional<JmsConnectionConfig> findFirstByIsDefaultTrue();
    
    Optional<JmsConnectionConfig> findTopByOrderByUpdatedAtDesc();
}
