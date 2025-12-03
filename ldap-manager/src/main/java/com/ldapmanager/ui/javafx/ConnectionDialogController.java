package com.ldapmanager.ui.javafx;

import com.ldapmanager.model.LdapConnection;
import com.ldapmanager.service.ConfigurationService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ConnectionDialogController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField hostField;

    @FXML
    private TextField portField;

    @FXML
    private TextField baseDnField;

    @FXML
    private TextField userDnField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private CheckBox sslCheckBox;

    @FXML
    private TextField userSearchBaseField;

    @FXML
    private TextField userSearchFilterField;

    @FXML
    private TextField groupSearchBaseField;

    @FXML
    private TextField groupSearchFilterField;

    @FXML
    private Button convertButton;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private LdapConnection connection;
    private ConfigurationService configService;
    private ConnectionPanelController parentController;

    public ConnectionDialogController(LdapConnection connection, ConfigurationService configService,
                                     ConnectionPanelController parentController) {
        this.connection = connection;
        this.configService = configService;
        this.parentController = parentController;
    }

    @FXML
    public void initialize() {
        // If editing an existing connection, populate fields
        if (connection != null) {
            nameField.setText(connection.getName());
            hostField.setText(connection.getHost());
            portField.setText(String.valueOf(connection.getPort()));
            baseDnField.setText(connection.getBaseDn());
            userDnField.setText(connection.getUserDn());
            passwordField.setText(connection.getPassword());
            sslCheckBox.setSelected(connection.isUseSsl());
            userSearchBaseField.setText(connection.getUserSearchBase());
            userSearchFilterField.setText(connection.getUserSearchFilter());
            groupSearchBaseField.setText(connection.getGroupSearchBase());
            groupSearchFilterField.setText(connection.getGroupSearchFilter());
        }

        // Set tooltips
        nameField.setTooltip(new Tooltip("A friendly name for this connection"));
        hostField.setTooltip(new Tooltip("LDAP server hostname or IP address"));
        portField.setTooltip(new Tooltip("LDAP port (default: 389, LDAPS: 636)"));
        baseDnField.setTooltip(new Tooltip("Base DN in format: dc=local,dc=cerebra,dc=sa (NOT local.cerebra.sa)"));
        userDnField.setTooltip(new Tooltip("Username or DN to bind with, e.g., 'admin' or 'cn=admin,dc=example,dc=com'"));
        passwordField.setTooltip(new Tooltip("Password for the bind user"));
        userSearchBaseField.setTooltip(new Tooltip("Base DN for user searches, e.g., ou=users,dc=example,dc=com"));
        userSearchFilterField.setTooltip(new Tooltip("LDAP filter for finding users. Use {0} as username placeholder"));
        groupSearchBaseField.setTooltip(new Tooltip("Base DN for group searches, e.g., ou=groups,dc=example,dc=com"));
        groupSearchFilterField.setTooltip(new Tooltip("LDAP filter for finding groups. Use {0} as user DN placeholder"));
    }

    @FXML
    private void handleConvert() {
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
    }

    @FXML
    private void handleSave() {
        // Validate required fields
        if (nameField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Name is required");
            return;
        }
        if (hostField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Host is required");
            return;
        }
        if (baseDnField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Base DN is required");
            return;
        }

        // Create or update connection
        LdapConnection conn = connection != null ? connection : new LdapConnection();
        conn.setName(nameField.getText().trim());
        conn.setHost(hostField.getText().trim());

        try {
            conn.setPort(Integer.parseInt(portField.getText().trim()));
        } catch (NumberFormatException e) {
            conn.setPort(389);
        }

        conn.setBaseDn(baseDnField.getText().trim());
        conn.setUserDn(userDnField.getText().trim());
        conn.setPassword(passwordField.getText());
        conn.setUseSsl(sslCheckBox.isSelected());
        conn.setUserSearchBase(userSearchBaseField.getText().trim());
        conn.setUserSearchFilter(userSearchFilterField.getText().trim());
        conn.setGroupSearchBase(groupSearchBaseField.getText().trim());
        conn.setGroupSearchFilter(groupSearchFilterField.getText().trim());

        // Save connection
        configService.saveConnection(conn);

        // Refresh parent table
        parentController.refreshConnections();

        // Close dialog
        closeDialog();
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
