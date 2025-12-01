package com.kafkamanager.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicInfo {
    private String name;
    private int partitionCount;
    private int replicationFactor;
    private Map<String, String> configs;
    private long totalMessages;
    private long totalSize;
    private boolean internal;
}
