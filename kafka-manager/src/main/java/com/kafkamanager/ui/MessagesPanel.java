package com.kafkamanager.ui;

import com.kafkamanager.model.KafkaConnection;
import com.kafkamanager.service.KafkaConnectionManager;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class MessagesPanel extends JPanel {

    private final KafkaConnectionManager connectionManager;
    private JTextField topicField;
    private JSpinner partitionSpinner;
    private JSpinner offsetSpinner;
    private JSpinner limitSpinner;
    private JTable messagesTable;
    private DefaultTableModel tableModel;
    private JTextArea messageDetailsArea;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public MessagesPanel(KafkaConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(300);

        JPanel tablePanel = createTablePanel();
        splitPane.setTopComponent(tablePanel);

        JPanel detailsPanel = createDetailsPanel();
        splitPane.setBottomComponent(detailsPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Message Browser"));

        panel.add(new JLabel("Topic:"));
        topicField = new JTextField(20);
        panel.add(topicField);

        panel.add(new JLabel("Partition:"));
        partitionSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 999, 1));
        partitionSpinner.setPreferredSize(new Dimension(80, 25));
        panel.add(partitionSpinner);

        panel.add(new JLabel("Offset:"));
        offsetSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Long.MAX_VALUE, 1));
        offsetSpinner.setPreferredSize(new Dimension(120, 25));
        panel.add(offsetSpinner);

        panel.add(new JLabel("Limit:"));
        limitSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 10000, 10));
        limitSpinner.setPreferredSize(new Dimension(80, 25));
        panel.add(limitSpinner);

        JButton fetchButton = new JButton("Fetch Messages");
        fetchButton.addActionListener(e -> fetchMessages());
        panel.add(fetchButton);

        JButton latestButton = new JButton("Fetch Latest");
        latestButton.addActionListener(e -> fetchLatestMessages());
        panel.add(latestButton);

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = {"Offset", "Timestamp", "Key", "Value (Preview)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        messagesTable = new JTable(tableModel);
        messagesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        messagesTable.setRowHeight(25);
        messagesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onMessageSelected();
            }
        });

        JScrollPane scrollPane = new JScrollPane(messagesTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Message Details"));

        messageDetailsArea = new JTextArea();
        messageDetailsArea.setEditable(false);
        messageDetailsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        messageDetailsArea.setLineWrap(true);
        messageDetailsArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(messageDetailsArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void fetchMessages() {
        String topic = topicField.getText().trim();
        if (topic.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a topic name",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int partition = (Integer) partitionSpinner.getValue();
        long offset = ((Number) offsetSpinner.getValue()).longValue();
        int limit = (Integer) limitSpinner.getValue();

        fetchMessagesFromTopic(topic, partition, offset, limit, false);
    }

    private void fetchLatestMessages() {
        String topic = topicField.getText().trim();
        if (topic.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a topic name",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int partition = (Integer) partitionSpinner.getValue();
        int limit = (Integer) limitSpinner.getValue();

        fetchMessagesFromTopic(topic, partition, 0, limit, true);
    }

    private void fetchMessagesFromTopic(String topic, int partition, long offset, int limit, boolean latest) {
        SwingWorker<List<ConsumerRecord<String, String>>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ConsumerRecord<String, String>> doInBackground() throws Exception {
                String connectionId = ((MainFrame) SwingUtilities.getWindowAncestor(MessagesPanel.this))
                        .getCurrentConnectionId();

                if (connectionId == null) {
                    throw new IllegalStateException("Not connected to any cluster");
                }

                KafkaConnection connection = connectionManager.getConnection(connectionId);
                Map<String, Object> props = connection.toPropertiesMap();
                props.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-manager-browser-" + UUID.randomUUID());
                props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
                props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
                props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
                props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

                List<ConsumerRecord<String, String>> records = new ArrayList<>();

                try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
                    TopicPartition topicPartition = new TopicPartition(topic, partition);
                    consumer.assign(Collections.singletonList(topicPartition));

                    if (latest) {
                        consumer.seekToEnd(Collections.singletonList(topicPartition));
                        long endOffset = consumer.position(topicPartition);
                        long startOffset = Math.max(0, endOffset - limit);
                        consumer.seek(topicPartition, startOffset);
                    } else {
                        consumer.seek(topicPartition, offset);
                    }

                    int fetchedCount = 0;
                    while (fetchedCount < limit) {
                        ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofSeconds(2));
                        if (consumerRecords.isEmpty()) {
                            break;
                        }

                        for (ConsumerRecord<String, String> record : consumerRecords) {
                            records.add(record);
                            fetchedCount++;
                            if (fetchedCount >= limit) {
                                break;
                            }
                        }
                    }
                }

                return records;
            }

            @Override
            protected void done() {
                try {
                    List<ConsumerRecord<String, String>> records = get();
                    updateMessagesTable(records);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(MessagesPanel.this,
                            "Failed to fetch messages: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void updateMessagesTable(List<ConsumerRecord<String, String>> records) {
        tableModel.setRowCount(0);
        for (ConsumerRecord<String, String> record : records) {
            String timestamp = dateFormatter.format(Instant.ofEpochMilli(record.timestamp()));
            String key = record.key() != null ? record.key() : "<null>";
            String valuePreview = record.value();
            if (valuePreview != null && valuePreview.length() > 100) {
                valuePreview = valuePreview.substring(0, 100) + "...";
            }

            Object[] row = {
                    record.offset(),
                    timestamp,
                    key,
                    valuePreview
            };
            tableModel.addRow(row);
        }
    }

    private void onMessageSelected() {
        int selectedRow = messagesTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        long offset = (Long) tableModel.getValueAt(selectedRow, 0);
        String timestamp = (String) tableModel.getValueAt(selectedRow, 1);
        String key = (String) tableModel.getValueAt(selectedRow, 2);

        StringBuilder details = new StringBuilder();
        details.append("Message Details\n");
        details.append("===============\n\n");
        details.append("Offset: ").append(offset).append("\n");
        details.append("Timestamp: ").append(timestamp).append("\n");
        details.append("Key: ").append(key).append("\n");
        details.append("\nValue:\n");
        details.append("------\n");

        messageDetailsArea.setText(details.toString());
    }

    public void clearData() {
        tableModel.setRowCount(0);
        messageDetailsArea.setText("");
        topicField.setText("");
    }
}
