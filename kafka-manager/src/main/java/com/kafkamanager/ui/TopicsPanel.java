package com.kafkamanager.ui;

import com.kafkamanager.model.TopicInfo;
import com.kafkamanager.service.KafkaConnectionManager;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class TopicsPanel extends JPanel {

    private final KafkaConnectionManager connectionManager;
    private JTable topicsTable;
    private DefaultTableModel tableModel;
    private JPanel detailsPanel;
    private JTextArea configTextArea;
    private ChartPanel chartPanel;

    public TopicsPanel(KafkaConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(400);

        JPanel topPanel = createTopPanel();
        splitPane.setTopComponent(topPanel);

        detailsPanel = createDetailsPanel();
        splitPane.setBottomComponent(detailsPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        String[] columns = {"Topic Name", "Partitions", "Replication Factor", "Internal"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        topicsTable = new JTable(tableModel);
        topicsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        topicsTable.setRowHeight(25);
        topicsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onTopicSelected();
            }
        });

        JScrollPane scrollPane = new JScrollPane(topicsTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> {
            // Refresh will be triggered from main frame
        });
        buttonPanel.add(refreshButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Topic Details"));

        JTabbedPane tabbedPane = new JTabbedPane();

        configTextArea = new JTextArea();
        configTextArea.setEditable(false);
        configTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane configScrollPane = new JScrollPane(configTextArea);
        tabbedPane.addTab("Configuration", configScrollPane);

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        JFreeChart chart = ChartFactory.createBarChart(
                "Partition Statistics",
                "Partition",
                "Message Count",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 300));
        tabbedPane.addTab("Statistics", chartPanel);

        panel.add(tabbedPane, BorderLayout.CENTER);

        return panel;
    }

    public void loadTopics(String connectionId) {
        SwingWorker<List<TopicInfo>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<TopicInfo> doInBackground() throws Exception {
                return connectionManager.listTopics(connectionId);
            }

            @Override
            protected void done() {
                try {
                    List<TopicInfo> topics = get();
                    updateTopicsTable(topics);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(TopicsPanel.this,
                            "Failed to load topics: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void updateTopicsTable(List<TopicInfo> topics) {
        tableModel.setRowCount(0);
        for (TopicInfo topic : topics) {
            Object[] row = {
                    topic.getName(),
                    topic.getPartitionCount(),
                    topic.getReplicationFactor(),
                    topic.isInternal() ? "Yes" : "No"
            };
            tableModel.addRow(row);
        }
    }

    private void onTopicSelected() {
        int selectedRow = topicsTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        String topicName = (String) tableModel.getValueAt(selectedRow, 0);
        loadTopicDetails(topicName);
    }

    private void loadTopicDetails(String topicName) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Map<String, Object> stats;

            @Override
            protected Void doInBackground() throws Exception {
                String connectionId = ((MainFrame) SwingUtilities.getWindowAncestor(TopicsPanel.this))
                        .getCurrentConnectionId();
                if (connectionId != null) {
                    stats = connectionManager.getTopicStatistics(connectionId, topicName);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    if (stats != null) {
                        updateDetailsPanel(topicName, stats);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(TopicsPanel.this,
                            "Failed to load topic details: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void updateDetailsPanel(String topicName, Map<String, Object> stats) {
        StringBuilder config = new StringBuilder();
        config.append("Topic: ").append(topicName).append("\n");
        config.append("Total Messages: ").append(stats.get("totalMessages")).append("\n");
        config.append("Partitions: ").append(stats.get("partitions")).append("\n");
        config.append("Replication Factor: ").append(stats.get("replicationFactor")).append("\n\n");

        config.append("Partition Details:\n");
        config.append("-----------------\n");

        @SuppressWarnings("unchecked")
        Map<Integer, Map<String, Object>> partitionDetails =
                (Map<Integer, Map<String, Object>>) stats.get("partitionDetails");

        if (partitionDetails != null) {
            for (Map.Entry<Integer, Map<String, Object>> entry : partitionDetails.entrySet()) {
                config.append(String.format("Partition %d:\n", entry.getKey()));
                Map<String, Object> details = entry.getValue();
                config.append(String.format("  Start Offset: %s\n", details.get("startOffset")));
                config.append(String.format("  End Offset: %s\n", details.get("endOffset")));
                config.append(String.format("  Message Count: %s\n", details.get("messageCount")));
                config.append("\n");
            }
        }

        configTextArea.setText(config.toString());
        configTextArea.setCaretPosition(0);

        updateChart(partitionDetails);
    }

    private void updateChart(Map<Integer, Map<String, Object>> partitionDetails) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        if (partitionDetails != null) {
            for (Map.Entry<Integer, Map<String, Object>> entry : partitionDetails.entrySet()) {
                Integer partition = entry.getKey();
                Map<String, Object> details = entry.getValue();
                Long messageCount = (Long) details.get("messageCount");

                dataset.addValue(messageCount, "Messages", "P" + partition);
            }
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Partition Statistics",
                "Partition",
                "Message Count",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        chartPanel.setChart(chart);
    }

    public void clearData() {
        tableModel.setRowCount(0);
        configTextArea.setText("");
    }
}
