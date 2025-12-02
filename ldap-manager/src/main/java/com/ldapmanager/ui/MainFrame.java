package com.ldapmanager.ui;

import com.ldapmanager.model.LdapConnection;
import com.ldapmanager.service.ConfigurationService;
import com.ldapmanager.service.LdapService;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private final ConfigurationService configService;
    private final LdapService ldapService;

    private ConnectionPanel connectionPanel;
    private UserSearchPanel userSearchPanel;
    private UserDetailsPanel userDetailsPanel;
    private AdvancedTestPanel advancedTestPanel;
    private EmbeddedServerPanel embeddedServerPanel;
    private JComboBox<String> connectionSelector;

    public MainFrame(ConfigurationService configService, LdapService ldapService,
                     com.ldapmanager.service.EmbeddedLdapServer embeddedLdapServer) {
        this.configService = configService;
        this.ldapService = ldapService;
        this.embeddedServerPanel = new EmbeddedServerPanel(embeddedLdapServer, configService);
        initComponents();
    }

    private void initComponents() {
        setTitle("LDAP Manager - User Validation & Management Tool");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);

        // Create menu bar
        createMenuBar();

        // Create main panel with tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();

        // Connection Management Tab
        connectionPanel = new ConnectionPanel(configService, ldapService);
        tabbedPane.addTab("Connections", new ImageIcon(), connectionPanel, "Manage LDAP Connections");

        // User Search & Validation Tab
        JPanel userPanel = new JPanel(new BorderLayout());

        // Connection selector at top
        JPanel selectorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectorPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        selectorPanel.add(new JLabel("Active Connection:"));

        connectionSelector = new JComboBox<>();
        connectionSelector.setPreferredSize(new Dimension(300, 25));
        connectionSelector.addActionListener(e -> updateActiveConnection());
        selectorPanel.add(connectionSelector);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadConnections());
        selectorPanel.add(refreshButton);

        userPanel.add(selectorPanel, BorderLayout.NORTH);

        // Split pane with search and details
        userSearchPanel = new UserSearchPanel(ldapService);
        userDetailsPanel = new UserDetailsPanel();

        userSearchPanel.setOnUserSelected(user -> userDetailsPanel.displayUser(user));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            userSearchPanel, userDetailsPanel);
        splitPane.setResizeWeight(0.5);

        userPanel.add(splitPane, BorderLayout.CENTER);

        tabbedPane.addTab("User Search", new ImageIcon(), userPanel, "Search and Validate Users");

        // Advanced Testing Tab
        JPanel advancedPanel = new JPanel(new BorderLayout());

        // Connection selector for advanced tab
        JPanel advancedSelectorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        advancedSelectorPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        advancedSelectorPanel.add(new JLabel("Active Connection:"));

        JComboBox<String> advancedConnectionSelector = new JComboBox<>();
        advancedConnectionSelector.setPreferredSize(new Dimension(300, 25));
        advancedSelectorPanel.add(advancedConnectionSelector);

        JButton advancedRefreshButton = new JButton("Refresh");
        advancedRefreshButton.addActionListener(e -> {
            advancedConnectionSelector.removeAllItems();
            configService.loadConnections().forEach(conn ->
                advancedConnectionSelector.addItem(conn.getName() + " (" + conn.getHost() + ")")
            );
            if (advancedConnectionSelector.getItemCount() > 0) {
                advancedConnectionSelector.setSelectedIndex(0);
                int selectedIndex = advancedConnectionSelector.getSelectedIndex();
                if (selectedIndex >= 0) {
                    LdapConnection connection = configService.loadConnections().get(selectedIndex);
                    advancedTestPanel.setConnection(connection);
                }
            }
        });
        advancedSelectorPanel.add(advancedRefreshButton);

        advancedPanel.add(advancedSelectorPanel, BorderLayout.NORTH);

        advancedTestPanel = new AdvancedTestPanel(ldapService);
        advancedPanel.add(advancedTestPanel, BorderLayout.CENTER);

        advancedConnectionSelector.addActionListener(e -> {
            int selectedIndex = advancedConnectionSelector.getSelectedIndex();
            if (selectedIndex >= 0) {
                LdapConnection connection = configService.loadConnections().get(selectedIndex);
                advancedTestPanel.setConnection(connection);
            }
        });

        tabbedPane.addTab("Advanced Testing", new ImageIcon(), advancedPanel, "Advanced LDAP Testing Operations");

        // Embedded Server Tab
        tabbedPane.addTab("Test Server", new ImageIcon(), embeddedServerPanel, "Embedded LDAP Server for Testing");

        // Add tabbed pane to frame
        add(tabbedPane, BorderLayout.CENTER);

        // Status bar
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        JLabel statusLabel = new JLabel("Ready");
        statusBar.add(statusLabel);
        add(statusBar, BorderLayout.SOUTH);

        // Load connections
        loadConnections();
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        // Connection Menu
        JMenu connectionMenu = new JMenu("Connection");
        JMenuItem addConnectionItem = new JMenuItem("Add Connection");
        addConnectionItem.addActionListener(e -> {
            // Switch to connections tab
            JTabbedPane tabbedPane = (JTabbedPane) getContentPane().getComponent(0);
            tabbedPane.setSelectedIndex(0);
        });
        connectionMenu.add(addConnectionItem);

        JMenuItem refreshConnectionsItem = new JMenuItem("Refresh Connections");
        refreshConnectionsItem.addActionListener(e -> loadConnections());
        connectionMenu.add(refreshConnectionsItem);

        menuBar.add(connectionMenu);

        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void loadConnections() {
        connectionSelector.removeAllItems();
        configService.loadConnections().forEach(conn ->
            connectionSelector.addItem(conn.getName() + " (" + conn.getHost() + ")")
        );

        if (connectionSelector.getItemCount() > 0) {
            connectionSelector.setSelectedIndex(0);
            updateActiveConnection();
        }
    }

    private void updateActiveConnection() {
        int selectedIndex = connectionSelector.getSelectedIndex();
        if (selectedIndex >= 0) {
            LdapConnection connection = configService.loadConnections().get(selectedIndex);
            userSearchPanel.setConnection(connection);
        }
    }

    private void showAboutDialog() {
        String message = """
            LDAP Manager v1.0.0

            A Spring Boot Desktop Application for LDAP User Management

            Features:
            - Manage multiple LDAP connections
            - Search and validate users
            - Authenticate users
            - View user details and group memberships
            - Export user data to Excel

            Built with Spring Boot and Java Swing
            """;

        JOptionPane.showMessageDialog(this, message, "About LDAP Manager",
            JOptionPane.INFORMATION_MESSAGE);
    }

    public void display() {
        SwingUtilities.invokeLater(() -> {
            setVisible(true);
        });
    }
}
