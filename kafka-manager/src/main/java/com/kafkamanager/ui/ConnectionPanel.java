package com.kafkamanager.ui;

import com.kafkamanager.model.KafkaConnection;
import com.kafkamanager.service.ConfigurationService;
import com.kafkamanager.service.KafkaConnectionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.UUID;

public class ConnectionPanel extends JPanel {

    private final KafkaConnectionManager connectionManager;
    private final ConfigurationService configService;
    private final Runnable onConnectionsUpdated;

    private JTable connectionsTable;
    private DefaultTableModel tableModel;
    private JButton connectDisconnectButton;

    public ConnectionPanel(KafkaConnectionManager connectionManager,
                          ConfigurationService configService,
                          Runnable onConnectionsUpdated) {
        this.connectionManager = connectionManager;
        this.configService = configService;
        this.onConnectionsUpdated = onConnectionsUpdated;

        initializeUI();
        loadConnections();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        String[] columns = {"Name", "Bootstrap Servers", "Security Protocol", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        connectionsTable = new JTable(tableModel);
        connectionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        connectionsTable.setRowHeight(25);
        connectionsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateConnectDisconnectButton();
            }
        });

        JScrollPane scrollPane = new JScrollPane(connectionsTable);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.EAST);
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(0, 10, 0, 0));

        JButton addButton = new JButton("Add Connection");
        JButton editButton = new JButton("Edit Connection");
        JButton deleteButton = new JButton("Delete Connection");
        connectDisconnectButton = new JButton("Connect");
        JButton testButton = new JButton("Test Connection");

        Dimension buttonSize = new Dimension(150, 30);
        addButton.setMaximumSize(buttonSize);
        editButton.setMaximumSize(buttonSize);
        deleteButton.setMaximumSize(buttonSize);
        connectDisconnectButton.setMaximumSize(buttonSize);
        testButton.setMaximumSize(buttonSize);

        addButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        editButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        deleteButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        connectDisconnectButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        testButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        addButton.addActionListener(e -> showConnectionDialog(null));
        editButton.addActionListener(e -> editSelectedConnection());
        deleteButton.addActionListener(e -> deleteSelectedConnection());
        connectDisconnectButton.addActionListener(e -> toggleConnection());
        testButton.addActionListener(e -> testSelectedConnection());

        connectDisconnectButton.setEnabled(false);

        panel.add(addButton);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(editButton);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(deleteButton);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(connectDisconnectButton);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(testButton);

        return panel;
    }

    private void loadConnections() {
        tableModel.setRowCount(0);
        for (KafkaConnection connection : connectionManager.getAllConnections()) {
            addConnectionToTable(connection);
        }
    }

    private void addConnectionToTable(KafkaConnection connection) {
        Object[] row = {
                connection.getName(),
                connection.getBootstrapServers(),
                connection.getSecurityProtocol() != null ? connection.getSecurityProtocol() : "PLAINTEXT",
                connection.isConnected() ? "Connected" : "Disconnected"
        };
        tableModel.addRow(row);
    }

    private void showConnectionDialog(KafkaConnection existingConnection) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Connection Configuration", true);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField nameField = new JTextField(20);
        JTextField serversField = new JTextField(20);
        JComboBox<String> securityProtocolCombo = new JComboBox<>(new String[]{
                "PLAINTEXT", "SSL", "SASL_PLAINTEXT", "SASL_SSL"
        });
        JComboBox<String> saslMechanismCombo = new JComboBox<>(new String[]{
                "", "PLAIN", "SCRAM-SHA-256", "SCRAM-SHA-512", "GSSAPI"
        });
        JTextField usernameField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);

        if (existingConnection != null) {
            nameField.setText(existingConnection.getName());
            serversField.setText(existingConnection.getBootstrapServers());
            if (existingConnection.getSecurityProtocol() != null) {
                securityProtocolCombo.setSelectedItem(existingConnection.getSecurityProtocol());
            }
            if (existingConnection.getSaslMechanism() != null) {
                saslMechanismCombo.setSelectedItem(existingConnection.getSaslMechanism());
            }
            if (existingConnection.getSaslUsername() != null) {
                usernameField.setText(existingConnection.getSaslUsername());
            }
            if (existingConnection.getSaslPassword() != null) {
                passwordField.setText(existingConnection.getSaslPassword());
            }
        }

        int row = 0;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Connection Name:"), gbc);
        gbc.gridx = 1;
        panel.add(nameField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Bootstrap Servers:"), gbc);
        gbc.gridx = 1;
        panel.add(serversField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Security Protocol:"), gbc);
        gbc.gridx = 1;
        panel.add(securityProtocolCombo, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("SASL Mechanism:"), gbc);
        gbc.gridx = 1;
        panel.add(saslMechanismCombo, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        panel.add(usernameField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        panel.add(passwordField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String servers = serversField.getText().trim();

            if (name.isEmpty() || servers.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Name and Bootstrap Servers are required",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            KafkaConnection connection = existingConnection != null ? existingConnection :
                    KafkaConnection.builder().id(UUID.randomUUID().toString()).build();

            connection.setName(name);
            connection.setBootstrapServers(servers);
            connection.setSecurityProtocol((String) securityProtocolCombo.getSelectedItem());
            connection.setSaslMechanism((String) saslMechanismCombo.getSelectedItem());
            connection.setSaslUsername(usernameField.getText().trim());
            connection.setSaslPassword(new String(passwordField.getPassword()));

            if (existingConnection == null) {
                connectionManager.addConnection(connection);
            }

            loadConnections();
            onConnectionsUpdated.run();
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        dialog.setLayout(new BorderLayout());
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void editSelectedConnection() {
        int selectedRow = connectionsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a connection to edit",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String connectionName = (String) tableModel.getValueAt(selectedRow, 0);
        KafkaConnection connection = connectionManager.getAllConnections().stream()
                .filter(c -> c.getName().equals(connectionName))
                .findFirst()
                .orElse(null);

        if (connection != null) {
            showConnectionDialog(connection);
        }
    }

    private void deleteSelectedConnection() {
        int selectedRow = connectionsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a connection to delete",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this connection?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            String connectionName = (String) tableModel.getValueAt(selectedRow, 0);
            KafkaConnection connection = connectionManager.getAllConnections().stream()
                    .filter(c -> c.getName().equals(connectionName))
                    .findFirst()
                    .orElse(null);

            if (connection != null) {
                connectionManager.removeConnection(connection.getId());
                loadConnections();
                onConnectionsUpdated.run();
            }
        }
    }

    private void testSelectedConnection() {
        int selectedRow = connectionsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a connection to test",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String connectionName = (String) tableModel.getValueAt(selectedRow, 0);
        KafkaConnection connection = connectionManager.getAllConnections().stream()
                .filter(c -> c.getName().equals(connectionName))
                .findFirst()
                .orElse(null);

        if (connection != null) {
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return connectionManager.testConnection(connection);
                }

                @Override
                protected void done() {
                    try {
                        boolean success = get();
                        if (success) {
                            JOptionPane.showMessageDialog(ConnectionPanel.this,
                                    "Connection test successful!",
                                    "Success", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(ConnectionPanel.this,
                                    "Connection test failed!",
                                    "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(ConnectionPanel.this,
                                "Connection test error: " + e.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    private void updateConnectDisconnectButton() {
        int selectedRow = connectionsTable.getSelectedRow();
        if (selectedRow < 0) {
            connectDisconnectButton.setEnabled(false);
            connectDisconnectButton.setText("Connect");
            return;
        }

        String connectionName = (String) tableModel.getValueAt(selectedRow, 0);
        KafkaConnection connection = connectionManager.getAllConnections().stream()
                .filter(c -> c.getName().equals(connectionName))
                .findFirst()
                .orElse(null);

        if (connection != null) {
            connectDisconnectButton.setEnabled(true);
            if (connection.isConnected()) {
                connectDisconnectButton.setText("Disconnect");
            } else {
                connectDisconnectButton.setText("Connect");
            }
        } else {
            connectDisconnectButton.setEnabled(false);
            connectDisconnectButton.setText("Connect");
        }
    }

    private void toggleConnection() {
        int selectedRow = connectionsTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        String connectionName = (String) tableModel.getValueAt(selectedRow, 0);
        KafkaConnection connection = connectionManager.getAllConnections().stream()
                .filter(c -> c.getName().equals(connectionName))
                .findFirst()
                .orElse(null);

        if (connection == null) {
            return;
        }

        if (connection.isConnected()) {
            disconnectFromCluster(connection);
        } else {
            connectToCluster(connection);
        }
    }

    private void connectToCluster(KafkaConnection connection) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    connectionManager.connect(connection.getId());
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(ConnectionPanel.this,
                                "Connection failed: " + e.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    });
                }
                return null;
            }

            @Override
            protected void done() {
                refreshConnections();
                updateConnectDisconnectButton();
                onConnectionsUpdated.run();
            }
        };
        worker.execute();
    }

    private void disconnectFromCluster(KafkaConnection connection) {
        connectionManager.closeConnection(connection.getId());
        refreshConnections();
        updateConnectDisconnectButton();
        onConnectionsUpdated.run();
    }

    public void refreshConnections() {
        loadConnections();
    }
}
