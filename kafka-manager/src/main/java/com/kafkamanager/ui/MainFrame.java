package com.kafkamanager.ui;

import com.kafkamanager.model.KafkaConnection;
import com.kafkamanager.service.ConfigurationService;
import com.kafkamanager.service.KafkaConnectionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

@Slf4j
@Component
public class MainFrame extends JFrame {

    private final KafkaConnectionManager connectionManager;
    private final ConfigurationService configService;

    private JTabbedPane tabbedPane;
    private ConnectionPanel connectionPanel;
    private TopicsPanel topicsPanel;
    private ConsumerGroupsPanel consumerGroupsPanel;
    private MessagesPanel messagesPanel;

    private JComboBox<String> connectionComboBox;
    private JLabel statusLabel;
    private String currentConnectionId;

    public MainFrame(KafkaConnectionManager connectionManager, ConfigurationService configService) {
        this.connectionManager = connectionManager;
        this.configService = configService;

        initializeUI();
        loadSavedConnections();
    }

    private void initializeUI() {
        setTitle("Kafka Manager - Enterprise Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel topPanel = createTopPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();

        connectionPanel = new ConnectionPanel(connectionManager, configService, this::onConnectionsUpdated);
        topicsPanel = new TopicsPanel(connectionManager);
        consumerGroupsPanel = new ConsumerGroupsPanel(connectionManager);
        messagesPanel = new MessagesPanel(connectionManager);

        tabbedPane.addTab("Connections", connectionPanel);
        tabbedPane.addTab("Topics", topicsPanel);
        tabbedPane.addTab("Consumer Groups", consumerGroupsPanel);
        tabbedPane.addTab("Messages", messagesPanel);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        JPanel statusPanel = createStatusPanel();
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel titleLabel = new JLabel("Kafka Cluster:");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));

        connectionComboBox = new JComboBox<>();
        connectionComboBox.setPreferredSize(new Dimension(300, 30));
        connectionComboBox.addActionListener(e -> onConnectionSelected());

        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connectToSelectedCluster());

        JButton disconnectButton = new JButton("Disconnect");
        disconnectButton.addActionListener(e -> disconnectFromCluster());

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshCurrentView());

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.add(titleLabel);
        leftPanel.add(connectionComboBox);
        leftPanel.add(connectButton);
        leftPanel.add(disconnectButton);
        leftPanel.add(refreshButton);

        panel.add(leftPanel, BorderLayout.WEST);

        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        panel.add(statusLabel, BorderLayout.WEST);

        return panel;
    }

    private void loadSavedConnections() {
        List<KafkaConnection> connections = configService.loadConnections();
        for (KafkaConnection connection : connections) {
            connectionManager.addConnection(connection);
        }
        updateConnectionComboBox();
    }

    private void updateConnectionComboBox() {
        connectionComboBox.removeAllItems();
        for (KafkaConnection connection : connectionManager.getAllConnections()) {
            connectionComboBox.addItem(connection.getId() + " - " + connection.getName());
        }
    }

    private void onConnectionSelected() {
        String selected = (String) connectionComboBox.getSelectedItem();
        if (selected != null) {
            currentConnectionId = selected.split(" - ")[0];
        }
    }

    private void connectToSelectedCluster() {
        if (currentConnectionId == null) {
            JOptionPane.showMessageDialog(this, "Please select a connection", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                setStatus("Connecting...");
                try {
                    connectionManager.connect(currentConnectionId);
                    setStatus("Connected to " + connectionManager.getConnection(currentConnectionId).getName());
                    connectionPanel.refreshConnections();
                    refreshCurrentView();
                } catch (Exception e) {
                    log.error("Connection failed", e);
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(MainFrame.this,
                                "Connection failed: " + e.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        setStatus("Connection failed");
                    });
                }
                return null;
            }
        };
        worker.execute();
    }

    private void disconnectFromCluster() {
        if (currentConnectionId != null) {
            connectionManager.closeConnection(currentConnectionId);
            setStatus("Disconnected");
            connectionPanel.refreshConnections();
            topicsPanel.clearData();
            consumerGroupsPanel.clearData();
            messagesPanel.clearData();
        }
    }

    private void refreshCurrentView() {
        if (currentConnectionId == null || !connectionManager.isConnected(currentConnectionId)) {
            return;
        }

        int selectedTab = tabbedPane.getSelectedIndex();
        switch (selectedTab) {
            case 1:
                topicsPanel.loadTopics(currentConnectionId);
                break;
            case 2:
                consumerGroupsPanel.loadConsumerGroups(currentConnectionId);
                break;
            case 3:
                break;
        }
    }

    private void onConnectionsUpdated() {
        updateConnectionComboBox();
        List<KafkaConnection> connections = (List<KafkaConnection>) connectionManager.getAllConnections()
                .stream().toList();
        configService.saveConnections(connections);
    }

    public void setStatus(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }

    public String getCurrentConnectionId() {
        return currentConnectionId;
    }
}
