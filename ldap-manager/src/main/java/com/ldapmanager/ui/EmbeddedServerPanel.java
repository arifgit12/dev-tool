package com.ldapmanager.ui;

import com.ldapmanager.model.LdapConnection;
import com.ldapmanager.service.ConfigurationService;
import com.ldapmanager.service.EmbeddedLdapServer;

import javax.swing.*;
import java.awt.*;
import java.util.UUID;

public class EmbeddedServerPanel extends JPanel {
    private final EmbeddedLdapServer embeddedServer;
    private final ConfigurationService configService;

    private JTextField portField;
    private JTextField baseDnField;
    private JPasswordField adminPasswordField;
    private JButton startButton;
    private JButton stopButton;
    private JButton addSampleDataButton;
    private JButton clearDataButton;
    private JButton createConnectionButton;
    private JTextArea statusArea;
    private JTextArea customLdifArea;
    private JButton addCustomButton;

    private JLabel statusLabel;

    public EmbeddedServerPanel(EmbeddedLdapServer embeddedServer, ConfigurationService configService) {
        this.embeddedServer = embeddedServer;
        this.configService = configService;
        initComponents();
        updateStatus();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title
        JLabel titleLabel = new JLabel("Embedded LDAP Server (Test Mode)");
        titleLabel.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));

        // Info panel
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.add(new JLabel("Status:"));
        statusLabel = new JLabel("STOPPED");
        statusLabel.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
        statusLabel.setForeground(Color.RED);
        infoPanel.add(statusLabel);

        // Configuration Panel
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBorder(BorderFactory.createTitledBorder("Server Configuration"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        portField = new JTextField("10389", 10);
        baseDnField = new JTextField("dc=example,dc=com", 25);
        adminPasswordField = new JPasswordField("admin123", 15);

        addConfigField(configPanel, "Port:", portField, 0, gbc);
        addConfigField(configPanel, "Base DN:", baseDnField, 1, gbc);
        addConfigField(configPanel, "Admin Password:", adminPasswordField, 2, gbc);

        // Control Buttons Panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startButton = new JButton("Start Server");
        stopButton = new JButton("Stop Server");
        addSampleDataButton = new JButton("Add Sample Data");
        clearDataButton = new JButton("Clear All Data");
        createConnectionButton = new JButton("Create Connection Config");

        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());
        addSampleDataButton.addActionListener(e -> addSampleData());
        clearDataButton.addActionListener(e -> clearData());
        createConnectionButton.addActionListener(e -> createConnection());

        stopButton.setEnabled(false);
        addSampleDataButton.setEnabled(false);
        clearDataButton.setEnabled(false);
        createConnectionButton.setEnabled(false);

        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(addSampleDataButton);
        controlPanel.add(clearDataButton);
        controlPanel.add(createConnectionButton);

        // Custom LDIF Panel
        JPanel customLdifPanel = new JPanel(new BorderLayout(5, 5));
        customLdifPanel.setBorder(BorderFactory.createTitledBorder("Add Custom LDIF Entry"));

        customLdifArea = new JTextArea(8, 60);
        customLdifArea.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
        customLdifArea.setText("""
            dn: cn=Test User,ou=users,dc=example,dc=com
            objectClass: top
            objectClass: person
            objectClass: organizationalPerson
            objectClass: inetOrgPerson
            cn: Test User
            sn: User
            givenName: Test
            uid: tuser
            mail: test.user@example.com
            userPassword: test123
            mobile: +1-555-9999
            """);

        JScrollPane ldifScrollPane = new JScrollPane(customLdifArea);
        customLdifPanel.add(ldifScrollPane, BorderLayout.CENTER);

        addCustomButton = new JButton("Add Custom Entry");
        addCustomButton.setEnabled(false);
        addCustomButton.addActionListener(e -> addCustomEntry());

        JPanel customButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        customButtonPanel.add(addCustomButton);
        customButtonPanel.add(new JLabel("(Server must be running)"));
        customLdifPanel.add(customButtonPanel, BorderLayout.SOUTH);

        // Status Area
        JPanel statusPanel = new JPanel(new BorderLayout(5, 5));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Server Information"));

        statusArea = new JTextArea(10, 60);
        statusArea.setEditable(false);
        statusArea.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
        JScrollPane statusScrollPane = new JScrollPane(statusArea);
        statusPanel.add(statusScrollPane, BorderLayout.CENTER);

        // Sample Users Info
        JPanel sampleInfoPanel = new JPanel(new BorderLayout());
        sampleInfoPanel.setBorder(BorderFactory.createTitledBorder("Sample Data Information"));
        JTextArea sampleInfoArea = new JTextArea("""
            When you add sample data, the following test accounts will be created:

            USERS (all with password: password123):
            • jdoe (John Doe) - Software Engineer - IT Department
            • jsmith (Jane Smith) - Project Manager - Management
            • bwilson (Bob Wilson) - Database Administrator - IT Department
            • abrown (Alice Brown) - Business Analyst - Business
            • cdavis (Charlie Davis) - DevOps Engineer - IT Department

            GROUPS:
            • Developers: John Doe, Bob Wilson, Charlie Davis
            • Managers: Jane Smith
            • IT Department: John Doe, Bob Wilson, Charlie Davis
            • All Employees: All users

            You can authenticate with any of these users or search for them.
            """);
        sampleInfoArea.setEditable(false);
        sampleInfoArea.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));
        sampleInfoArea.setBackground(new Color(245, 245, 245));
        sampleInfoPanel.add(new JScrollPane(sampleInfoArea), BorderLayout.CENTER);

        // Layout
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(infoPanel, BorderLayout.CENTER);
        topPanel.add(configPanel, BorderLayout.SOUTH);

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.add(controlPanel, BorderLayout.NORTH);

        JPanel middlePanel = new JPanel(new GridLayout(1, 2, 10, 0));
        middlePanel.add(statusPanel);
        middlePanel.add(sampleInfoPanel);
        centerPanel.add(middlePanel, BorderLayout.CENTER);
        centerPanel.add(customLdifPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
    }

    private void addConfigField(JPanel panel, String label, JComponent field, int row, GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    private void startServer() {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            String baseDN = baseDnField.getText().trim();
            String password = new String(adminPasswordField.getPassword());

            if (baseDN.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Base DN and Admin Password are required",
                    "Invalid Configuration", JOptionPane.ERROR_MESSAGE);
                return;
            }

            embeddedServer.start(port, baseDN, password);

            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            addSampleDataButton.setEnabled(true);
            clearDataButton.setEnabled(true);
            createConnectionButton.setEnabled(true);
            addCustomButton.setEnabled(true);

            portField.setEnabled(false);
            baseDnField.setEnabled(false);
            adminPasswordField.setEnabled(false);

            updateStatus();

            JOptionPane.showMessageDialog(this,
                "Embedded LDAP server started successfully!\n\n" +
                "You can now:\n" +
                "1. Add sample data\n" +
                "2. Create a connection configuration\n" +
                "3. Test with the other tabs",
                "Server Started", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Failed to start server: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopServer() {
        try {
            embeddedServer.stop();

            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            addSampleDataButton.setEnabled(false);
            clearDataButton.setEnabled(false);
            createConnectionButton.setEnabled(false);
            addCustomButton.setEnabled(false);

            portField.setEnabled(true);
            baseDnField.setEnabled(true);
            adminPasswordField.setEnabled(true);

            updateStatus();

            JOptionPane.showMessageDialog(this,
                "Embedded LDAP server stopped",
                "Server Stopped", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Failed to stop server: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addSampleData() {
        try {
            embeddedServer.addSampleData();
            updateStatus();

            JOptionPane.showMessageDialog(this,
                "Sample data added successfully!\n\n" +
                "5 users and 4 groups have been created.\n" +
                "All users have password: password123",
                "Sample Data Added", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Failed to add sample data: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearData() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to clear all data?\nThis will remove all users and groups.",
            "Confirm Clear", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                embeddedServer.clearAllData();
                updateStatus();

                JOptionPane.showMessageDialog(this,
                    "All data cleared successfully",
                    "Data Cleared", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Failed to clear data: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void addCustomEntry() {
        String ldif = customLdifArea.getText().trim();

        if (ldif.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter LDIF content",
                "Empty LDIF", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            embeddedServer.addCustomEntry(ldif);

            JOptionPane.showMessageDialog(this,
                "Custom entry added successfully",
                "Entry Added", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Failed to add custom entry: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createConnection() {
        if (!embeddedServer.isRunning()) {
            JOptionPane.showMessageDialog(this,
                "Server is not running",
                "Server Not Running", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LdapConnection connection = new LdapConnection();
        connection.setId(UUID.randomUUID().toString());
        connection.setName("Embedded Test Server");
        connection.setHost("localhost");
        connection.setPort(embeddedServer.getPort());
        connection.setBaseDn(embeddedServer.getBaseDN());
        connection.setUserDn("cn=admin," + embeddedServer.getBaseDN());
        connection.setPassword(new String(adminPasswordField.getPassword()));
        connection.setUseSsl(false);
        connection.setUserSearchBase("ou=users," + embeddedServer.getBaseDN());
        connection.setUserSearchFilter("(uid={0})");
        connection.setGroupSearchBase("ou=groups," + embeddedServer.getBaseDN());
        connection.setGroupSearchFilter("(member={0})");

        configService.saveConnection(connection);

        JOptionPane.showMessageDialog(this,
            "Connection configuration created successfully!\n\n" +
            "Name: Embedded Test Server\n" +
            "Host: localhost:" + embeddedServer.getPort() + "\n" +
            "Base DN: " + embeddedServer.getBaseDN() + "\n\n" +
            "Go to the Connections tab to see it.",
            "Connection Created", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateStatus() {
        if (embeddedServer.isRunning()) {
            statusLabel.setText("RUNNING");
            statusLabel.setForeground(new Color(0, 150, 0));
            statusArea.setText(embeddedServer.getServerInfo());
        } else {
            statusLabel.setText("STOPPED");
            statusLabel.setForeground(Color.RED);
            statusArea.setText("Server is not running.\n\nConfigure and start the server to begin testing.");
        }
    }
}
