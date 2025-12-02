package com.ldapmanager.ui;

import com.ldapmanager.model.LdapConnection;
import com.ldapmanager.service.ConfigurationService;
import com.ldapmanager.service.LdapService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ConnectionPanel extends JPanel {
    private final ConfigurationService configService;
    private final LdapService ldapService;
    private JTable connectionTable;
    private DefaultTableModel tableModel;
    private JButton addButton, editButton, deleteButton, testButton;

    public ConnectionPanel(ConfigurationService configService, LdapService ldapService) {
        this.configService = configService;
        this.ldapService = ldapService;
        initComponents();
        loadConnections();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title
        JLabel titleLabel = new JLabel("LDAP Connections");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        add(titleLabel, BorderLayout.NORTH);

        // Table
        String[] columns = {"Name", "Host", "Port", "Base DN", "SSL"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        connectionTable = new JTable(tableModel);
        connectionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(connectionTable);
        add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addButton = new JButton("Add");
        editButton = new JButton("Edit");
        deleteButton = new JButton("Delete");
        testButton = new JButton("Test Connection");

        addButton.addActionListener(e -> showConnectionDialog(null));
        editButton.addActionListener(e -> editSelectedConnection());
        deleteButton.addActionListener(e -> deleteSelectedConnection());
        testButton.addActionListener(e -> testSelectedConnection());

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(testButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadConnections() {
        tableModel.setRowCount(0);
        List<LdapConnection> connections = configService.loadConnections();
        for (LdapConnection conn : connections) {
            Object[] row = {
                conn.getName(),
                conn.getHost(),
                conn.getPort(),
                conn.getBaseDn(),
                conn.isUseSsl() ? "Yes" : "No"
            };
            tableModel.addRow(row);
        }
    }

    private void showConnectionDialog(LdapConnection connection) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            connection == null ? "Add Connection" : "Edit Connection", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(11, 2, 5, 5));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField nameField = new JTextField(connection != null ? connection.getName() : "");
        JTextField hostField = new JTextField(connection != null ? connection.getHost() : "");
        JTextField portField = new JTextField(connection != null ? String.valueOf(connection.getPort()) : "389");
        JTextField baseDnField = new JTextField(connection != null ? connection.getBaseDn() : "");
        JTextField userDnField = new JTextField(connection != null ? connection.getUserDn() : "");
        JPasswordField passwordField = new JPasswordField(connection != null ? connection.getPassword() : "");
        JCheckBox sslCheckBox = new JCheckBox("Use SSL", connection != null && connection.isUseSsl());
        JTextField userSearchBaseField = new JTextField(connection != null ? connection.getUserSearchBase() : "");
        JTextField userSearchFilterField = new JTextField(connection != null ? connection.getUserSearchFilter() : "(uid={0})");
        JTextField groupSearchBaseField = new JTextField(connection != null ? connection.getGroupSearchBase() : "");
        JTextField groupSearchFilterField = new JTextField(connection != null ? connection.getGroupSearchFilter() : "(member={0})");

        // Set tooltips for better clarity
        nameField.setToolTipText("A friendly name for this connection");
        hostField.setToolTipText("LDAP server hostname or IP address");
        portField.setToolTipText("LDAP port (default: 389, LDAPS: 636)");
        baseDnField.setToolTipText("Base DN in format: dc=local,dc=cerebra,dc=sa (NOT local.cerebra.sa)");
        userDnField.setToolTipText("Username or DN to bind with, e.g., 'admin' or 'cn=admin,dc=example,dc=com'");
        passwordField.setToolTipText("Password for the bind user");
        userSearchBaseField.setToolTipText("Base DN for user searches, e.g., ou=users,dc=example,dc=com");
        userSearchFilterField.setToolTipText("LDAP filter for finding users. Use {0} as username placeholder");
        groupSearchBaseField.setToolTipText("Base DN for group searches, e.g., ou=groups,dc=example,dc=com");
        groupSearchFilterField.setToolTipText("LDAP filter for finding groups. Use {0} as user DN placeholder");

        formPanel.add(new JLabel("Name:"));
        formPanel.add(nameField);
        formPanel.add(new JLabel("Host:"));
        formPanel.add(hostField);
        formPanel.add(new JLabel("Port:"));
        formPanel.add(portField);

        // Base DN with helper button
        JLabel baseDnLabel = new JLabel("Base DN:");
        baseDnLabel.setToolTipText("Format: dc=local,dc=cerebra,dc=sa");
        formPanel.add(baseDnLabel);

        JPanel baseDnPanel = new JPanel(new BorderLayout(5, 0));
        baseDnPanel.add(baseDnField, BorderLayout.CENTER);
        JButton convertButton = new JButton("Convert");
        convertButton.setToolTipText("Convert domain name to DN format");
        convertButton.addActionListener(e -> {
            String value = baseDnField.getText().trim();
            if (!value.isEmpty() && !value.startsWith("dc=")) {
                // Convert domain name to DN format
                String[] parts = value.split("\\.");
                StringBuilder dn = new StringBuilder();
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) dn.append(",");
                    dn.append("dc=").append(parts[i]);
                }
                baseDnField.setText(dn.toString());
            }
        });
        baseDnPanel.add(convertButton, BorderLayout.EAST);
        formPanel.add(baseDnPanel);

        JLabel bindUserLabel = new JLabel("Bind Username/DN:");
        bindUserLabel.setToolTipText("Can be a simple username (e.g., 'admin') or full DN (e.g., 'cn=admin,dc=example,dc=com')");
        formPanel.add(bindUserLabel);
        formPanel.add(userDnField);

        formPanel.add(new JLabel("Password:"));
        formPanel.add(passwordField);
        formPanel.add(new JLabel(""));
        formPanel.add(sslCheckBox);
        formPanel.add(new JLabel("User Search Base:"));
        formPanel.add(userSearchBaseField);
        formPanel.add(new JLabel("User Search Filter:"));
        formPanel.add(userSearchFilterField);
        formPanel.add(new JLabel("Group Search Base:"));
        formPanel.add(groupSearchBaseField);
        formPanel.add(new JLabel("Group Search Filter:"));
        formPanel.add(groupSearchFilterField);

        dialog.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> {
            LdapConnection conn = connection != null ? connection : new LdapConnection();
            conn.setName(nameField.getText());
            conn.setHost(hostField.getText());
            try {
                conn.setPort(Integer.parseInt(portField.getText()));
            } catch (NumberFormatException ex) {
                conn.setPort(389);
            }
            conn.setBaseDn(baseDnField.getText());
            conn.setUserDn(userDnField.getText());
            conn.setPassword(new String(passwordField.getPassword()));
            conn.setUseSsl(sslCheckBox.isSelected());
            conn.setUserSearchBase(userSearchBaseField.getText());
            conn.setUserSearchFilter(userSearchFilterField.getText());
            conn.setGroupSearchBase(groupSearchBaseField.getText());
            conn.setGroupSearchFilter(groupSearchFilterField.getText());

            configService.saveConnection(conn);
            loadConnections();
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setSize(500, 450);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void editSelectedConnection() {
        int selectedRow = connectionTable.getSelectedRow();
        if (selectedRow >= 0) {
            List<LdapConnection> connections = configService.loadConnections();
            LdapConnection connection = connections.get(selectedRow);
            showConnectionDialog(connection);
        } else {
            JOptionPane.showMessageDialog(this, "Please select a connection to edit.");
        }
    }

    private void deleteSelectedConnection() {
        int selectedRow = connectionTable.getSelectedRow();
        if (selectedRow >= 0) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this connection?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                List<LdapConnection> connections = configService.loadConnections();
                LdapConnection connection = connections.get(selectedRow);
                configService.deleteConnection(connection.getId());
                loadConnections();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a connection to delete.");
        }
    }

    private void testSelectedConnection() {
        int selectedRow = connectionTable.getSelectedRow();
        if (selectedRow >= 0) {
            List<LdapConnection> connections = configService.loadConnections();
            LdapConnection connection = connections.get(selectedRow);

            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    return ldapService.testConnection(connection);
                }

                @Override
                protected void done() {
                    try {
                        boolean success = get();
                        if (success) {
                            JOptionPane.showMessageDialog(ConnectionPanel.this,
                                "Connection successful!",
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(ConnectionPanel.this,
                                "Connection failed!",
                                "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ConnectionPanel.this,
                            "Error testing connection: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        } else {
            JOptionPane.showMessageDialog(this, "Please select a connection to test.");
        }
    }

    public LdapConnection getSelectedConnection() {
        int selectedRow = connectionTable.getSelectedRow();
        if (selectedRow >= 0) {
            List<LdapConnection> connections = configService.loadConnections();
            return connections.get(selectedRow);
        }
        return null;
    }
}
