package com.ldapmanager.ui.javafx;

import com.ldapmanager.model.LdapConnection;
import com.ldapmanager.service.ConfigurationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

@Component
public class AdvancedTestPanelController {

    @Autowired
    private ConfigurationService configService;

    @FXML
    private ComboBox<LdapConnection> connectionComboBox;

    @FXML
    private ComboBox<String> operationComboBox;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField groupNameField;

    @FXML
    private Button executeButton;

    @FXML
    private Button copyButton;

    @FXML
    private Button clearButton;

    @FXML
    private TextArea resultArea;

    private LdapConnection currentConnection;

    private static final String[] OPERATIONS = {
            "Retrieve User Details",
            "Check Group Membership",
            "Get User Groups",
            "Get Group Members",
            "Search Groups",
            "Get Group Members with Mobiles",
            "Get User Manager",
            "Get Direct Reports"
    };

    @FXML
    public void initialize() {
        // Load connections
        loadConnections();

        // Set up connection combo box
        connectionComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(LdapConnection item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });

        connectionComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(LdapConnection item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });

        // Set up operations
        operationComboBox.setItems(FXCollections.observableArrayList(OPERATIONS));
        operationComboBox.getSelectionModel().selectFirst();
    }

    private void loadConnections() {
        List<LdapConnection> connections = configService.loadConnections();
        connectionComboBox.setItems(FXCollections.observableArrayList(connections));
        if (!connections.isEmpty()) {
            connectionComboBox.getSelectionModel().selectFirst();
            currentConnection = connections.get(0);
        }
    }

    @FXML
    private void handleConnectionChange() {
        currentConnection = connectionComboBox.getSelectionModel().getSelectedItem();
    }

    @FXML
    private void handleExecute() {
        if (currentConnection == null) {
            showAlert(Alert.AlertType.WARNING, "No Connection", "Please select a connection first.");
            return;
        }

        String operation = operationComboBox.getSelectionModel().getSelectedItem();
        if (operation == null) {
            showAlert(Alert.AlertType.WARNING, "No Operation", "Please select an operation.");
            return;
        }

        executeButton.setDisable(true);
        resultArea.setText("Executing operation: " + operation + "\nPlease wait...");

        Task<String> executeTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return switch (operation) {
                    case "Retrieve User Details" -> retrieveUserDetails();
                    case "Check Group Membership" -> checkGroupMembership();
                    case "Get User Groups" -> getUserGroups();
                    case "Get Group Members" -> getGroupMembers();
                    case "Search Groups" -> searchGroups();
                    case "Get Group Members with Mobiles" -> getGroupMembersWithMobiles();
                    case "Get User Manager" -> getUserManager();
                    case "Get Direct Reports" -> getDirectReports();
                    default -> "Unknown operation";
                };
            }
        };

        executeTask.setOnSucceeded(event -> {
            resultArea.setText(executeTask.getValue());
            executeButton.setDisable(false);
        });

        executeTask.setOnFailed(event -> {
            Throwable ex = executeTask.getException();
            resultArea.setText("Error: " + ex.getMessage());
            executeButton.setDisable(false);
        });

        new Thread(executeTask).start();
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
                    result.append("\n");
                    result.append("Note: Some attributes may be incomplete due to LDAP referrals.\n");
                    result.append("The displayed results are from the primary server.\n");
                }
            }
        } catch (javax.naming.PartialResultException e) {
            result.append("Result: PARTIAL SUCCESS ⚠\n\n");
            result.append("The operation partially completed but encountered LDAP referrals.\n");
            result.append("This is normal in Active Directory environments with multiple domain controllers.\n");
        } catch (Exception e) {
            result.append("Result: ERROR ✗\n\n");
            result.append("Error Type: ").append(e.getClass().getSimpleName()).append("\n");
            result.append("Error Message: ").append(e.getMessage()).append("\n");
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }

        return result.toString();
    }

    private String checkGroupMembership() throws Exception {
        String username = usernameField.getText().trim();
        String groupName = groupNameField.getText().trim();

        if (username.isEmpty() || groupName.isEmpty()) {
            return "Error: Both username and group name are required";
        }

        return "Operation: Check Group Membership\n" +
                "Username: " + username + "\n" +
                "Group: " + groupName + "\n\n" +
                "This operation checks if the user is a member of the specified group.\n" +
                "(Full implementation requires JNDI LDAP operations)";
    }

    private String getUserGroups() throws Exception {
        String username = usernameField.getText().trim();

        if (username.isEmpty()) {
            return "Error: Username is required";
        }

        return "Operation: Get User Groups\n" +
                "Username: " + username + "\n\n" +
                "This operation retrieves all groups that the user belongs to.\n" +
                "(Full implementation requires JNDI LDAP operations)";
    }

    private String getGroupMembers() throws Exception {
        String groupName = groupNameField.getText().trim();

        if (groupName.isEmpty()) {
            return "Error: Group name is required";
        }

        return "Operation: Get Group Members\n" +
                "Group: " + groupName + "\n\n" +
                "This operation retrieves all members of the specified group.\n" +
                "(Full implementation requires JNDI LDAP operations)";
    }

    private String searchGroups() throws Exception {
        String groupName = groupNameField.getText().trim();

        if (groupName.isEmpty()) {
            return "Error: Group name is required";
        }

        return "Operation: Search Groups\n" +
                "Search term: " + groupName + "\n\n" +
                "This operation searches for groups matching the search term.\n" +
                "(Full implementation requires JNDI LDAP operations)";
    }

    private String getGroupMembersWithMobiles() throws Exception {
        String groupNames = groupNameField.getText().trim();

        if (groupNames.isEmpty()) {
            return "Error: Group name(s) required (comma-separated for multiple)";
        }

        return "Operation: Get Group Members with Mobiles\n" +
                "Groups: " + groupNames + "\n\n" +
                "This operation retrieves group members with their mobile numbers.\n" +
                "(Full implementation requires JNDI LDAP operations)";
    }

    private String getUserManager() throws Exception {
        String username = usernameField.getText().trim();

        if (username.isEmpty()) {
            return "Error: Username is required";
        }

        return "Operation: Get User Manager\n" +
                "Username: " + username + "\n\n" +
                "This operation retrieves the manager of the specified user.\n" +
                "(Full implementation requires JNDI LDAP operations)";
    }

    private String getDirectReports() throws Exception {
        String username = usernameField.getText().trim();

        if (username.isEmpty()) {
            return "Error: Username is required";
        }

        return "Operation: Get Direct Reports\n" +
                "Username: " + username + "\n\n" +
                "This operation retrieves the direct reports of the specified user.\n" +
                "(Full implementation requires JNDI LDAP operations)";
    }

    private LdapContext createLdapContext(String username, String password) throws Exception {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.PROVIDER_URL, "ldap://" + currentConnection.getHost() + ":" + currentConnection.getPort());
        env.put(Context.SECURITY_PRINCIPAL, username);
        env.put(Context.SECURITY_CREDENTIALS, password);
        env.put(Context.REFERRAL, "ignore");

        return new InitialLdapContext(env, null);
    }

    @FXML
    private void handleCopy() {
        String text = resultArea.getText();
        if (text != null && !text.isEmpty()) {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(text);
            clipboard.setContent(content);

            showAlert(Alert.AlertType.INFORMATION, "Copied", "Results copied to clipboard!");
        } else {
            showAlert(Alert.AlertType.INFORMATION, "Info", "No results to copy");
        }
    }

    @FXML
    private void handleClear() {
        resultArea.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}
