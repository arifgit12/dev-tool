package com.kafkamanager.ui;

import com.kafkamanager.service.KafkaConnectionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class ConsumerGroupsPanel extends JPanel {

    private final KafkaConnectionManager connectionManager;
    private JTable groupsTable;
    private DefaultTableModel tableModel;
    private JTextArea detailsTextArea;

    public ConsumerGroupsPanel(KafkaConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(400);

        JPanel leftPanel = createLeftPanel();
        splitPane.setLeftComponent(leftPanel);

        JPanel rightPanel = createRightPanel();
        splitPane.setRightComponent(rightPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        String[] columns = {"Consumer Group ID"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        groupsTable = new JTable(tableModel);
        groupsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupsTable.setRowHeight(25);
        groupsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onGroupSelected();
            }
        });

        JScrollPane scrollPane = new JScrollPane(groupsTable);
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

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Consumer Group Details"));

        detailsTextArea = new JTextArea();
        detailsTextArea.setEditable(false);
        detailsTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(detailsTextArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    public void loadConsumerGroups(String connectionId) {
        SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return connectionManager.listConsumerGroups(connectionId);
            }

            @Override
            protected void done() {
                try {
                    List<String> groups = get();
                    updateGroupsTable(groups);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ConsumerGroupsPanel.this,
                            "Failed to load consumer groups: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void updateGroupsTable(List<String> groups) {
        tableModel.setRowCount(0);
        for (String group : groups) {
            tableModel.addRow(new Object[]{group});
        }
    }

    private void onGroupSelected() {
        int selectedRow = groupsTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        String groupId = (String) tableModel.getValueAt(selectedRow, 0);
        loadGroupDetails(groupId);
    }

    private void loadGroupDetails(String groupId) {
        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                String connectionId = ((MainFrame) SwingUtilities.getWindowAncestor(ConsumerGroupsPanel.this))
                        .getCurrentConnectionId();
                if (connectionId != null) {
                    return connectionManager.getConsumerGroupInfo(connectionId, groupId);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> info = get();
                    if (info != null) {
                        updateDetailsPanel(info);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ConsumerGroupsPanel.this,
                            "Failed to load consumer group details: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void updateDetailsPanel(Map<String, Object> info) {
        StringBuilder details = new StringBuilder();
        details.append("Consumer Group Information\n");
        details.append("=========================\n\n");
        details.append("Group ID: ").append(info.get("groupId")).append("\n");
        details.append("State: ").append(info.get("state")).append("\n");
        details.append("Members: ").append(info.get("members")).append("\n");
        details.append("Coordinator: ").append(info.get("coordinator")).append("\n");
        details.append("Partition Assignor: ").append(info.get("partitionAssignor")).append("\n");

        detailsTextArea.setText(details.toString());
        detailsTextArea.setCaretPosition(0);
    }

    public void clearData() {
        tableModel.setRowCount(0);
        detailsTextArea.setText("");
    }
}
