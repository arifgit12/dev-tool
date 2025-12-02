package com.ldapmanager.ui;

import com.ldapmanager.model.LdapConnection;
import com.ldapmanager.model.LdapUser;
import com.ldapmanager.service.LdapService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

public class UserSearchPanel extends JPanel {
    private final LdapService ldapService;
    private JTextField searchField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton searchButton;
    private JButton authenticateButton;
    private JButton clearButton;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private Consumer<LdapUser> onUserSelected;
    private LdapConnection currentConnection;

    public UserSearchPanel(LdapService ldapService) {
        this.ldapService = ldapService;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title
        JLabel titleLabel = new JLabel("User Search & Authentication");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));

        // Search Panel
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        JPanel searchInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(30);
        searchButton = new JButton("Search Users");
        clearButton = new JButton("Clear");

        searchInputPanel.add(new JLabel("Search:"));
        searchInputPanel.add(searchField);
        searchInputPanel.add(searchButton);
        searchInputPanel.add(clearButton);

        searchPanel.add(searchInputPanel, BorderLayout.NORTH);

        // Authentication Panel
        JPanel authPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        authPanel.setBorder(BorderFactory.createTitledBorder("User Authentication"));
        usernameField = new JTextField(15);
        passwordField = new JPasswordField(15);
        authenticateButton = new JButton("Authenticate");

        authPanel.add(new JLabel("Username:"));
        authPanel.add(usernameField);
        authPanel.add(new JLabel("Password:"));
        authPanel.add(passwordField);
        authPanel.add(authenticateButton);

        searchPanel.add(authPanel, BorderLayout.SOUTH);

        // Top Panel (Title + Search)
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        // Results Table
        String[] columns = {"Username", "Full Name", "Email", "Department", "Enabled"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultsTable = new JTable(tableModel);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        resultsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = resultsTable.getSelectedRow();
                if (selectedRow >= 0 && onUserSelected != null) {
                    String username = (String) tableModel.getValueAt(selectedRow, 0);
                    loadUserDetails(username);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(resultsTable);
        add(scrollPane, BorderLayout.CENTER);

        // Button Actions
        searchButton.addActionListener(e -> performSearch());
        clearButton.addActionListener(e -> clearResults());
        authenticateButton.addActionListener(e -> performAuthentication());

        searchField.addActionListener(e -> performSearch());
        passwordField.addActionListener(e -> performAuthentication());
    }

    public void setConnection(LdapConnection connection) {
        this.currentConnection = connection;
        clearResults();
    }

    public void setOnUserSelected(Consumer<LdapUser> callback) {
        this.onUserSelected = callback;
    }

    private void performSearch() {
        if (currentConnection == null) {
            JOptionPane.showMessageDialog(this,
                "Please select a connection first.",
                "No Connection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter a search term.",
                "Empty Search", JOptionPane.WARNING_MESSAGE);
            return;
        }

        searchButton.setEnabled(false);
        searchButton.setText("Searching...");

        new SwingWorker<List<LdapUser>, Void>() {
            @Override
            protected List<LdapUser> doInBackground() throws Exception {
                return ldapService.searchUsers(currentConnection, searchTerm);
            }

            @Override
            protected void done() {
                try {
                    List<LdapUser> users = get();
                    displayResults(users);

                    if (users.isEmpty()) {
                        JOptionPane.showMessageDialog(UserSearchPanel.this,
                            "No users found matching: " + searchTerm,
                            "No Results", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    String errorMsg = ex.getMessage();
                    if (ex.getCause() != null) {
                        errorMsg += "\nCause: " + ex.getCause().getMessage();
                    }
                    JOptionPane.showMessageDialog(UserSearchPanel.this,
                        "Search failed:\n" + errorMsg,
                        "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    searchButton.setEnabled(true);
                    searchButton.setText("Search Users");
                }
            }
        }.execute();
    }

    private void performAuthentication() {
        if (currentConnection == null) {
            JOptionPane.showMessageDialog(this,
                "Please select a connection first.",
                "No Connection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter both username and password.",
                "Missing Credentials", JOptionPane.WARNING_MESSAGE);
            return;
        }

        authenticateButton.setEnabled(false);
        authenticateButton.setText("Authenticating...");

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return ldapService.authenticateUser(currentConnection, username, password);
            }

            @Override
            protected void done() {
                try {
                    boolean authenticated = get();
                    if (authenticated) {
                        JOptionPane.showMessageDialog(UserSearchPanel.this,
                            "Authentication successful for user: " + username,
                            "Success", JOptionPane.INFORMATION_MESSAGE);

                        // Load user details
                        loadUserDetails(username);
                    } else {
                        JOptionPane.showMessageDialog(UserSearchPanel.this,
                            "Authentication failed for user: " + username,
                            "Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    String errorMsg = ex.getMessage();
                    if (ex.getCause() != null) {
                        errorMsg += "\nCause: " + ex.getCause().getMessage();
                    }
                    JOptionPane.showMessageDialog(UserSearchPanel.this,
                        "Authentication error:\n" + errorMsg,
                        "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    authenticateButton.setEnabled(true);
                    authenticateButton.setText("Authenticate");
                    passwordField.setText("");
                }
            }
        }.execute();
    }

    private void loadUserDetails(String username) {
        new SwingWorker<LdapUser, Void>() {
            @Override
            protected LdapUser doInBackground() throws Exception {
                return ldapService.findUser(currentConnection, username);
            }

            @Override
            protected void done() {
                try {
                    LdapUser user = get();
                    if (user != null && onUserSelected != null) {
                        onUserSelected.accept(user);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void displayResults(List<LdapUser> users) {
        tableModel.setRowCount(0);
        for (LdapUser user : users) {
            Object[] row = {
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getDepartment(),
                user.isEnabled() ? "Yes" : "No"
            };
            tableModel.addRow(row);
        }
    }

    private void clearResults() {
        tableModel.setRowCount(0);
        searchField.setText("");
    }
}
