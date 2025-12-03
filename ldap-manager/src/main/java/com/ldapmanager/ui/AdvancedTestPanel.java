package com.ldapmanager.ui;

import com.ldapmanager.model.LdapConnection;
import com.ldapmanager.service.LdapService;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class AdvancedTestPanel extends JPanel {
    private final LdapService ldapService;
    private LdapConnection currentConnection;

    private JComboBox<String> operationComboBox;
    private JPanel inputPanel;
    private JTextArea resultArea;
    private JButton executeButton;
    private JButton clearButton;

    // Input fields that will be shown/hidden based on selected operation
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField searchTermField;
    private JTextField attributeNameField;
    private JTextArea groupListArea;

    public AdvancedTestPanel(LdapService ldapService) {
        this.ldapService = ldapService;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title
        JLabel titleLabel = new JLabel("Advanced LDAP Testing");
        titleLabel.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));

        // Operation selection
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Select Operation:"));

        String[] operations = {
            "1. Authenticate User",
            "2. Retrieve User Details",
            "3. Check User Exists",
            "4. Search Users by Name (with Mobile)",
            "5. Search Groups",
            "6. Get Group Members with Mobiles",
            "7. Retrieve Specific User Attributes",
            "8. Test Connection with Custom Credentials"
        };

        operationComboBox = new JComboBox<>(operations);
        operationComboBox.addActionListener(e -> updateInputPanel());
        topPanel.add(operationComboBox);

        // Input panel (dynamic based on operation)
        inputPanel = new JPanel();
        inputPanel.setBorder(BorderFactory.createTitledBorder("Input Parameters"));
        inputPanel.setPreferredSize(new Dimension(800, 150));

        // Initialize input fields
        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        searchTermField = new JTextField(20);
        attributeNameField = new JTextField(20);
        groupListArea = new JTextArea(4, 20);
        groupListArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        executeButton = new JButton("Execute");
        JButton copyButton = new JButton("Copy to Clipboard");
        clearButton = new JButton("Clear Results");

        executeButton.addActionListener(e -> executeOperation());

        copyButton.addActionListener(e -> {
            String text = resultArea.getText();
            if (text != null && !text.isEmpty()) {
                java.awt.datatransfer.StringSelection selection =
                    new java.awt.datatransfer.StringSelection(text);
                java.awt.Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(selection, selection);
                JOptionPane.showMessageDialog(this,
                    "Results copied to clipboard!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "No results to copy",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });

        clearButton.addActionListener(e -> resultArea.setText(""));

        buttonPanel.add(executeButton);
        buttonPanel.add(copyButton);
        buttonPanel.add(clearButton);

        // Results area
        resultArea = new JTextArea(20, 60);
        resultArea.setEditable(false);
        resultArea.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(resultArea);

        // Layout
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(titleLabel, BorderLayout.NORTH);
        northPanel.add(topPanel, BorderLayout.CENTER);
        northPanel.add(inputPanel, BorderLayout.SOUTH);

        add(northPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Initialize with first operation
        updateInputPanel();
    }

    public void setConnection(LdapConnection connection) {
        this.currentConnection = connection;
    }

    private void updateInputPanel() {
        inputPanel.removeAll();
        inputPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        int selectedIndex = operationComboBox.getSelectedIndex();

        switch (selectedIndex) {
            case 0: // Authenticate User
                addField(inputPanel, "Username:", usernameField, gbc, 0);
                addField(inputPanel, "Password:", passwordField, gbc, 1);
                break;

            case 1: // Retrieve User Details
                addField(inputPanel, "Username:", usernameField, gbc, 0);
                break;

            case 2: // Check User Exists
                addField(inputPanel, "Username:", usernameField, gbc, 0);
                break;

            case 3: // Search Users by Name
                addField(inputPanel, "Search Term (Name):", searchTermField, gbc, 0);
                break;

            case 4: // Search Groups
                addField(inputPanel, "Group Name/ID:", searchTermField, gbc, 0);
                break;

            case 5: // Get Group Members
                gbc.gridx = 0;
                gbc.gridy = 0;
                inputPanel.add(new JLabel("Group IDs (one per line):"), gbc);
                gbc.gridx = 1;
                gbc.fill = GridBagConstraints.BOTH;
                inputPanel.add(new JScrollPane(groupListArea), gbc);
                break;

            case 6: // Retrieve Specific Attributes
                addField(inputPanel, "Username:", usernameField, gbc, 0);
                addField(inputPanel, "Attribute Name:", attributeNameField, gbc, 1);
                break;

            case 7: // Test Connection
                addField(inputPanel, "Username:", usernameField, gbc, 0);
                addField(inputPanel, "Password:", passwordField, gbc, 1);
                break;
        }

        inputPanel.revalidate();
        inputPanel.repaint();
    }

    private void addField(JPanel panel, String label, JComponent field, GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    private void executeOperation() {
        if (currentConnection == null) {
            JOptionPane.showMessageDialog(this,
                "Please select a connection first.",
                "No Connection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        executeButton.setEnabled(false);
        executeButton.setText("Executing...");
        resultArea.setText("Executing operation...\n");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    int selectedIndex = operationComboBox.getSelectedIndex();
                    switch (selectedIndex) {
                        case 0:
                            return authenticateUser();
                        case 1:
                            return retrieveUserDetails();
                        case 2:
                            return checkUserExists();
                        case 3:
                            return searchUsersWithMobile();
                        case 4:
                            return searchGroups();
                        case 5:
                            return getGroupMembers();
                        case 6:
                            return retrieveSpecificAttributes();
                        case 7:
                            return testConnection();
                        default:
                            return "Invalid operation selected";
                    }
                } catch (Exception e) {
                    return "Error: " + e.getMessage() + "\n" + getStackTrace(e);
                }
            }

            @Override
            protected void done() {
                try {
                    resultArea.setText(get());
                } catch (Exception ex) {
                    resultArea.setText("Error: " + ex.getMessage());
                }
                executeButton.setEnabled(true);
                executeButton.setText("Execute");
            }
        }.execute();
    }

    private LdapContext createLdapContext(String username, String password) throws Exception {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.PROVIDER_URL, currentConnection.getUrl());
        env.put(Context.SECURITY_PRINCIPAL, username);
        env.put(Context.SECURITY_CREDENTIALS, password);

        // Handle LDAP referrals - ignore them to prevent "Unprocessed Continuation Reference" errors
        // This is common in Active Directory environments with multiple domain controllers
        env.put(Context.REFERRAL, "ignore");

        return new InitialLdapContext(env, null);
    }

    private String getDomain(String domainName) {
        String[] parts = domainName.split("\\.");
        StringBuilder dn = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) dn.append(",");
            dn.append("dc=").append(parts[i]);
        }
        return dn.toString();
    }

    private String authenticateUser() throws Exception {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            return "Error: Username and password are required";
        }

        StringBuilder result = new StringBuilder();
        result.append("=== Authentication Test ===\n\n");
        result.append("Username: ").append(username).append("\n");
        result.append("Connection: ").append(currentConnection.getHost()).append(":").append(currentConnection.getPort()).append("\n");
        result.append("Base DN: ").append(currentConnection.getBaseDn()).append("\n\n");

        try {
            boolean authenticated = ldapService.authenticateUser(currentConnection, username, password);

            if (authenticated) {
                result.append("Result: SUCCESS ✓\n\n");
                result.append("User authenticated successfully.");
            } else {
                result.append("Result: FAILED ✗\n\n");
                result.append("Authentication failed - invalid username or password.\n");
                result.append("Please verify:\n");
                result.append("  - Username is correct\n");
                result.append("  - Password is correct\n");
                result.append("  - User exists in the directory\n");
                result.append("  - User account is not locked or disabled\n");
            }
        } catch (Exception e) {
            result.append("Result: ERROR ✗\n\n");
            result.append("Error Type: ").append(e.getClass().getSimpleName()).append("\n");
            result.append("Error Message: ").append(e.getMessage()).append("\n\n");

            // Add detailed stack trace
            result.append("Detailed Error Log:\n");
            result.append("==================\n");
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            result.append(sw.toString());

            // Log to console as well
            System.err.println("=== Authentication Failed ===");
            System.err.println("Username: " + username);
            System.err.println("Connection: " + currentConnection.getHost() + ":" + currentConnection.getPort());
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        return result.toString();
    }

    private String retrieveUserDetails() throws Exception {
        String username = usernameField.getText().trim();

        if (username.isEmpty()) {
            return "Error: Username is required";
        }

        StringBuilder result = new StringBuilder();
        result.append("=== User Details ===\n\n");
        result.append("Username: ").append(username).append("\n");
        result.append("Connection: ").append(currentConnection.getHost()).append(":").append(currentConnection.getPort()).append("\n");
        result.append("Base DN: ").append(currentConnection.getBaseDn()).append("\n\n");

        LdapContext ctx = null;
        try {
            ctx = createLdapContext(currentConnection.getUserDn(), currentConnection.getPassword());

            SearchControls searchCtls = new SearchControls();
            searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

            String filter = "(sAMAccountName=" + username + ")";
            String searchFilter = "(&(objectcategory=user)(|" + filter + "))";
            String searchBase = currentConnection.getBaseDn();

            NamingEnumeration<?> enums = ctx.search(searchBase, searchFilter, searchCtls);

            if (!enums.hasMore()) {
                result.append("User not found: ").append(username).append("\n\n");
                result.append("Please verify:\n");
                result.append("  - Username is correct\n");
                result.append("  - User exists in the directory\n");
                result.append("  - Search base DN is correct\n");
            } else {
                result.append("User found! Details:\n");
                result.append("==================\n\n");

                try {
                    while (enums.hasMore()) {
                        SearchResult sr = (SearchResult) enums.next();
                        Attributes allAttrs = sr.getAttributes();

                        for (NamingEnumeration<?> ne = allAttrs.getAll(); ne.hasMoreElements();) {
                            Attribute natt = (Attribute) ne.next();
                            String attrName = natt.getID();

                            for (Enumeration<?> vals = natt.getAll(); vals.hasMoreElements();) {
                                Object value = vals.nextElement();
                                result.append(String.format("%-30s: %s\n", attrName, value));
                            }
                        }
                    }
                } catch (javax.naming.PartialResultException e) {
                    // This occurs when the server returns referrals to other LDAP servers
                    // We've already retrieved some results, so just note this and continue
                    result.append("\n");
                    result.append("Note: Some attributes may be incomplete due to LDAP referrals.\n");
                    result.append("The displayed results are from the primary server.\n");
                    result.append("Additional data may exist on other domain controllers.\n");

                    // Log to console for debugging
                    System.out.println("PartialResultException encountered (expected in multi-DC environments)");
                    System.out.println("Displayed partial results successfully");
                }
            }
        } catch (javax.naming.PartialResultException e) {
            // Handle referral exception at the top level (if it occurs before getting any results)
            result.append("Result: PARTIAL SUCCESS ⚠\n\n");
            result.append("The operation partially completed but encountered LDAP referrals.\n");
            result.append("This is normal in Active Directory environments with multiple domain controllers.\n\n");
            result.append("Note: The LDAP server returned referrals to other servers, ");
            result.append("but some results may have been retrieved before the referral was encountered.\n");

            System.err.println("PartialResultException at top level: " + e.getMessage());
        } catch (Exception e) {
            result.append("Result: ERROR ✗\n\n");
            result.append("Error Type: ").append(e.getClass().getSimpleName()).append("\n");
            result.append("Error Message: ").append(e.getMessage()).append("\n\n");

            // Add detailed stack trace
            result.append("Detailed Error Log:\n");
            result.append("==================\n");
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            result.append(sw.toString());

            // Log to console as well
            System.err.println("=== Retrieve User Details Failed ===");
            System.err.println("Username: " + username);
            System.err.println("Connection: " + currentConnection.getHost() + ":" + currentConnection.getPort());
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (Exception e) {
                    System.err.println("Error closing LDAP context: " + e.getMessage());
                }
            }
        }

        return result.toString();
    }

    private String checkUserExists() throws Exception {
        String username = usernameField.getText().trim();

        if (username.isEmpty()) {
            return "Error: Username is required";
        }

        LdapContext ctx = createLdapContext(currentConnection.getUserDn(), currentConnection.getPassword());

        SearchControls searchCtls = new SearchControls();
        searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchCtls.setCountLimit(1);
        searchCtls.setReturningAttributes(new String[]{"sAMAccountName"});

        String filter = "(sAMAccountName=" + username + ")";
        String searchFilter = "(&(objectcategory=user)(|" + filter + "))";
        String searchBase = currentConnection.getBaseDn();

        NamingEnumeration<?> enums = ctx.search(searchBase, searchFilter, searchCtls);

        boolean exists = enums.hasMore();

        StringBuilder result = new StringBuilder();
        result.append("=== User Existence Check ===\n\n");
        result.append("Username: ").append(username).append("\n");
        result.append("Exists: ").append(exists ? "YES ✓" : "NO ✗").append("\n");

        ctx.close();
        return result.toString();
    }

    private String searchUsersWithMobile() throws Exception {
        String searchTerm = searchTermField.getText().trim();

        if (searchTerm.isEmpty()) {
            return "Error: Search term is required";
        }

        LdapContext ctx = createLdapContext(currentConnection.getUserDn(), currentConnection.getPassword());

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        String searchFilter = "(&(objectCategory=person)(objectClass=user)(cn=" + searchTerm + "*)(!(userAccountControl:1.2.840.113556.1.4.803:=2)))";
        String searchBase = currentConnection.getBaseDn();

        NamingEnumeration<SearchResult> results = ctx.search(searchBase, searchFilter, searchControls);

        StringBuilder result = new StringBuilder();
        result.append("=== Users with Mobile Numbers ===\n\n");

        int count = 0;
        while (results.hasMoreElements()) {
            SearchResult searchResult = results.nextElement();
            Attributes allAttrs = searchResult.getAttributes();

            String displayName = null;
            String mobile = null;
            String email = null;

            for (NamingEnumeration<?> ne = allAttrs.getAll(); ne.hasMoreElements();) {
                Attribute natt = (Attribute) ne.next();
                String sid = natt.getID();

                for (Enumeration<?> vals = natt.getAll(); vals.hasMoreElements();) {
                    String val = vals.nextElement().toString();
                    if (sid.equals("displayName")) {
                        displayName = val;
                    } else if (sid.equals("mobile")) {
                        mobile = val;
                    } else if (sid.equals("mail")) {
                        email = val;
                    }
                }
            }

            if (displayName != null) {
                count++;
                result.append(String.format("%d. %s\n", count, displayName));
                if (mobile != null) result.append("   Mobile: ").append(mobile).append("\n");
                if (email != null) result.append("   Email: ").append(email).append("\n");
                result.append("\n");
            }
        }

        result.append("Total users found: ").append(count);

        ctx.close();
        return result.toString();
    }

    private String searchGroups() throws Exception {
        String searchTerm = searchTermField.getText().trim();

        if (searchTerm.isEmpty()) {
            return "Error: Group name/ID is required";
        }

        LdapContext ctx = createLdapContext(currentConnection.getUserDn(), currentConnection.getPassword());

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        String searchFilter = "(&(objectCategory=group)(cn=" + searchTerm + "*))";
        String searchBase = currentConnection.getBaseDn();

        NamingEnumeration<SearchResult> results = ctx.search(searchBase, searchFilter, searchControls);

        StringBuilder result = new StringBuilder();
        result.append("=== Groups ===\n\n");

        int count = 0;
        while (results.hasMoreElements()) {
            SearchResult searchResult = results.nextElement();
            Attributes allAttrs = searchResult.getAttributes();

            String groupName = null;
            String description = null;
            List<String> members = new ArrayList<>();

            for (NamingEnumeration<?> ne = allAttrs.getAll(); ne.hasMoreElements();) {
                Attribute natt = (Attribute) ne.next();
                String sid = natt.getID();

                for (Enumeration<?> vals = natt.getAll(); vals.hasMoreElements();) {
                    String val = vals.nextElement().toString();
                    if (sid.equals("sAMAccountName")) {
                        groupName = val;
                    } else if (sid.equals("description")) {
                        description = val;
                    } else if (sid.equals("member")) {
                        if (val.startsWith("CN=")) {
                            String memberName = val.substring(3, val.indexOf(","));
                            members.add(memberName);
                        }
                    }
                }
            }

            if (groupName != null) {
                count++;
                result.append(String.format("%d. Group: %s\n", count, groupName));
                if (description != null) result.append("   Description: ").append(description).append("\n");
                result.append("   Members (").append(members.size()).append("):\n");
                for (String member : members) {
                    result.append("      - ").append(member).append("\n");
                }
                result.append("\n");
            }
        }

        result.append("Total groups found: ").append(count);

        ctx.close();
        return result.toString();
    }

    private String getGroupMembers() throws Exception {
        String groupListText = groupListArea.getText().trim();

        if (groupListText.isEmpty()) {
            return "Error: At least one group ID is required";
        }

        String[] groupIds = groupListText.split("\n");
        LdapContext ctx = createLdapContext(currentConnection.getUserDn(), currentConnection.getPassword());

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        StringBuilder result = new StringBuilder();
        result.append("=== Group Members with Mobile Numbers ===\n\n");

        for (String groupId : groupIds) {
            groupId = groupId.trim();
            if (groupId.isEmpty()) continue;

            String searchFilter = "(&(objectCategory=group)(cn=" + groupId + "))";
            String searchBase = currentConnection.getBaseDn();

            NamingEnumeration<SearchResult> results = ctx.search(searchBase, searchFilter, searchControls);

            while (results.hasMoreElements()) {
                SearchResult searchResult = results.nextElement();
                Attributes allAttrs = searchResult.getAttributes();

                String groupName = null;
                List<String> members = new ArrayList<>();

                for (NamingEnumeration<?> ne = allAttrs.getAll(); ne.hasMoreElements();) {
                    Attribute natt = (Attribute) ne.next();
                    String sid = natt.getID();

                    for (Enumeration<?> vals = natt.getAll(); vals.hasMoreElements();) {
                        String val = vals.nextElement().toString();
                        if (sid.equals("sAMAccountName")) {
                            groupName = val;
                        } else if (sid.equals("member")) {
                            if (val.startsWith("CN=") && !val.contains("CN=Group")) {
                                String memberName = val.substring(3, val.indexOf(","));
                                members.add(memberName);
                            }
                        }
                    }
                }

                if (groupName != null) {
                    result.append("Group: ").append(groupName).append("\n");
                    result.append("Members: ").append(members.size()).append("\n\n");

                    // Get mobile numbers for members
                    for (String memberName : members) {
                        String memberMobile = getMemberMobile(ctx, memberName);
                        result.append("  - ").append(memberName);
                        if (memberMobile != null) {
                            result.append(" (").append(memberMobile).append(")");
                        }
                        result.append("\n");
                    }
                    result.append("\n");
                }
            }
        }

        ctx.close();
        return result.toString();
    }

    private String getMemberMobile(LdapContext ctx, String memberName) {
        try {
            SearchControls searchCtls = new SearchControls();
            searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchCtls.setReturningAttributes(new String[]{"mobile"});

            String filter = "(cn=" + memberName + ")";
            String searchFilter = "(&(objectCategory=person)(objectClass=user)(|" + filter + "))";
            String searchBase = currentConnection.getBaseDn();

            NamingEnumeration<?> enums = ctx.search(searchBase, searchFilter, searchCtls);

            if (enums.hasMore()) {
                SearchResult sr = (SearchResult) enums.next();
                Attribute mobileAttr = sr.getAttributes().get("mobile");
                if (mobileAttr != null) {
                    return (String) mobileAttr.get();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private String retrieveSpecificAttributes() throws Exception {
        String username = usernameField.getText().trim();
        String attributeName = attributeNameField.getText().trim();

        if (username.isEmpty()) {
            return "Error: Username is required";
        }

        LdapContext ctx = createLdapContext(currentConnection.getUserDn(), currentConnection.getPassword());

        SearchControls searchCtls = new SearchControls();
        searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        if (!attributeName.isEmpty()) {
            searchCtls.setReturningAttributes(new String[]{attributeName});
        }

        String filter = "(sAMAccountName=" + username + ")";
        String searchFilter = "(&(objectcategory=user)(|" + filter + "))";
        String searchBase = currentConnection.getBaseDn();

        NamingEnumeration<?> enums = ctx.search(searchBase, searchFilter, searchCtls);

        StringBuilder result = new StringBuilder();
        result.append("=== Specific Attributes ===\n\n");
        result.append("User: ").append(username).append("\n");
        if (!attributeName.isEmpty()) {
            result.append("Attribute: ").append(attributeName).append("\n\n");
        } else {
            result.append("All Attributes:\n\n");
        }

        if (!enums.hasMore()) {
            result.append("User not found");
        } else {
            while (enums.hasMore()) {
                SearchResult sr = (SearchResult) enums.next();
                Attributes allAttrs = sr.getAttributes();

                for (NamingEnumeration<?> ne = allAttrs.getAll(); ne.hasMoreElements();) {
                    Attribute natt = (Attribute) ne.next();
                    String attrName = natt.getID();

                    for (Enumeration<?> vals = natt.getAll(); vals.hasMoreElements();) {
                        Object value = vals.nextElement();
                        result.append(String.format("%s: %s\n", attrName, value));
                    }
                }
            }
        }

        ctx.close();
        return result.toString();
    }

    private String testConnection() throws Exception {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            return "Error: Username and password are required";
        }

        StringBuilder result = new StringBuilder();
        result.append("=== Connection Test ===\n\n");
        result.append("Server: ").append(currentConnection.getHost()).append(":").append(currentConnection.getPort()).append("\n");
        result.append("Username: ").append(username).append("\n");
        result.append("SSL: ").append(currentConnection.isUseSsl() ? "Yes" : "No").append("\n\n");

        try {
            LdapContext ctx = createLdapContext(username, password);
            result.append("Connection Status: SUCCESS ✓\n");
            result.append("Context loaded successfully\n");
            ctx.close();
        } catch (Exception e) {
            result.append("Connection Status: FAILED ✗\n");
            result.append("Error: ").append(e.getMessage()).append("\n");
        }

        return result.toString();
    }

    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}
