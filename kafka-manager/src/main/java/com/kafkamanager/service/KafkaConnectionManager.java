package com.kafkamanager.service;

import com.kafkamanager.model.KafkaConnection;
import com.kafkamanager.model.TopicInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KafkaConnectionManager {

    private final Map<String, AdminClient> adminClients = new ConcurrentHashMap<>();
    private final Map<String, KafkaConnection> connections = new ConcurrentHashMap<>();

    public void addConnection(KafkaConnection connection) {
        connections.put(connection.getId(), connection);
    }

    public void removeConnection(String connectionId) {
        closeConnection(connectionId);
        connections.remove(connectionId);
    }

    public boolean testConnection(KafkaConnection connection) {
        try {
            Map<String, Object> props = connection.toPropertiesMap();
            props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000");
            props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "5000");

            try (AdminClient adminClient = AdminClient.create(props)) {
                adminClient.describeCluster().clusterId().get();
                return true;
            }
        } catch (Exception e) {
            log.error("Connection test failed for {}: {}", connection.getName(), e.getMessage());
            return false;
        }
    }

    public void connect(String connectionId) throws Exception {
        KafkaConnection connection = connections.get(connectionId);
        if (connection == null) {
            throw new IllegalArgumentException("Connection not found: " + connectionId);
        }

        Map<String, Object> props = connection.toPropertiesMap();
        AdminClient adminClient = AdminClient.create(props);

        adminClient.describeCluster().clusterId().get();

        adminClients.put(connectionId, adminClient);
        connection.setConnected(true);
        log.info("Connected to Kafka cluster: {}", connection.getName());
    }

    public void closeConnection(String connectionId) {
        AdminClient adminClient = adminClients.remove(connectionId);
        if (adminClient != null) {
            adminClient.close();
        }
        KafkaConnection connection = connections.get(connectionId);
        if (connection != null) {
            connection.setConnected(false);
        }
    }

    public List<TopicInfo> listTopics(String connectionId) throws ExecutionException, InterruptedException {
        AdminClient adminClient = adminClients.get(connectionId);
        if (adminClient == null) {
            throw new IllegalStateException("Not connected to cluster");
        }

        ListTopicsResult topicsResult = adminClient.listTopics(new ListTopicsOptions().listInternal(true));
        Set<String> topicNames = topicsResult.names().get();

        DescribeTopicsResult describeResult = adminClient.describeTopics(topicNames);
        Map<String, TopicDescription> topicDescriptions = describeResult.all().get();

        ConfigResource.Type type = ConfigResource.Type.TOPIC;
        Collection<ConfigResource> resources = topicNames.stream()
                .map(name -> new ConfigResource(type, name))
                .collect(Collectors.toList());

        DescribeConfigsResult configsResult = adminClient.describeConfigs(resources);
        Map<ConfigResource, Config> configs = configsResult.all().get();

        List<TopicInfo> topicInfoList = new ArrayList<>();
        for (Map.Entry<String, TopicDescription> entry : topicDescriptions.entrySet()) {
            String topicName = entry.getKey();
            TopicDescription description = entry.getValue();

            ConfigResource resource = new ConfigResource(type, topicName);
            Config config = configs.get(resource);

            Map<String, String> configMap = new HashMap<>();
            if (config != null) {
                config.entries().forEach(e -> configMap.put(e.name(), e.value()));
            }

            TopicInfo info = TopicInfo.builder()
                    .name(topicName)
                    .partitionCount(description.partitions().size())
                    .replicationFactor(description.partitions().isEmpty() ? 0 :
                            description.partitions().get(0).replicas().size())
                    .configs(configMap)
                    .internal(description.isInternal())
                    .build();

            topicInfoList.add(info);
        }

        return topicInfoList;
    }

    public Map<String, Object> getTopicStatistics(String connectionId, String topicName)
            throws ExecutionException, InterruptedException {
        AdminClient adminClient = adminClients.get(connectionId);
        KafkaConnection connection = connections.get(connectionId);

        if (adminClient == null || connection == null) {
            throw new IllegalStateException("Not connected to cluster");
        }

        Map<String, Object> props = connection.toPropertiesMap();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-manager-stats-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        Map<String, Object> stats = new HashMap<>();

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            TopicDescription description = adminClient.describeTopics(Collections.singleton(topicName))
                    .all().get().get(topicName);

            List<TopicPartition> partitions = description.partitions().stream()
                    .map(p -> new TopicPartition(topicName, p.partition()))
                    .collect(Collectors.toList());

            consumer.assign(partitions);

            Map<TopicPartition, Long> beginningOffsets = consumer.beginningOffsets(partitions);
            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);

            long totalMessages = 0;
            for (TopicPartition partition : partitions) {
                long start = beginningOffsets.getOrDefault(partition, 0L);
                long end = endOffsets.getOrDefault(partition, 0L);
                totalMessages += (end - start);
            }

            stats.put("totalMessages", totalMessages);
            stats.put("partitions", partitions.size());
            stats.put("replicationFactor", description.partitions().isEmpty() ? 0 :
                    description.partitions().get(0).replicas().size());
            stats.put("partitionDetails", partitions.stream()
                    .collect(Collectors.toMap(
                            TopicPartition::partition,
                            p -> Map.of(
                                    "startOffset", beginningOffsets.getOrDefault(p, 0L),
                                    "endOffset", endOffsets.getOrDefault(p, 0L),
                                    "messageCount", endOffsets.getOrDefault(p, 0L) - beginningOffsets.getOrDefault(p, 0L)
                            )
                    )));
        }

        return stats;
    }

    public List<String> listConsumerGroups(String connectionId) throws ExecutionException, InterruptedException {
        AdminClient adminClient = adminClients.get(connectionId);
        if (adminClient == null) {
            throw new IllegalStateException("Not connected to cluster");
        }

        return adminClient.listConsumerGroups().all().get().stream()
                .map(ConsumerGroupListing::groupId)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getConsumerGroupInfo(String connectionId, String groupId)
            throws ExecutionException, InterruptedException {
        AdminClient adminClient = adminClients.get(connectionId);
        if (adminClient == null) {
            throw new IllegalStateException("Not connected to cluster");
        }

        DescribeConsumerGroupsResult result = adminClient.describeConsumerGroups(Collections.singleton(groupId));
        ConsumerGroupDescription description = result.all().get().get(groupId);

        Map<String, Object> info = new HashMap<>();
        info.put("groupId", description.groupId());
        info.put("state", description.state().toString());
        info.put("members", description.members().size());
        info.put("coordinator", description.coordinator().toString());
        info.put("partitionAssignor", description.partitionAssignor());

        return info;
    }

    public Collection<KafkaConnection> getAllConnections() {
        return connections.values();
    }

    public KafkaConnection getConnection(String connectionId) {
        return connections.get(connectionId);
    }

    public boolean isConnected(String connectionId) {
        KafkaConnection connection = connections.get(connectionId);
        return connection != null && connection.isConnected();
    }
}
