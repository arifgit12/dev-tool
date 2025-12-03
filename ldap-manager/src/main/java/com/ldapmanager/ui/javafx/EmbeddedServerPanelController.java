package com.ldapmanager.ui.javafx;

import com.ldapmanager.model.LdapConnection;
import com.ldapmanager.service.ConfigurationService;
import com.ldapmanager.service.EmbeddedLdapServer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EmbeddedServerPanelController {

    @Autowired
    private EmbeddedLdapServer embeddedServer;

    @Autowired
    private ConfigurationService configService;

    @FXML
    private Label statusLabel;

    @FXML
    private TextField portField;

    @FXML
    private TextField baseDnField;

    @FXML
    private PasswordField adminPasswordField;

    @FXML
    private Button startButton;

    @FXML
    private Button stopButton;

    @FXML
    private Button addSampleButton;

    @FXML
    private Button clearDataButton;

    @FXML
    private Button createConnectionButton;

    @FXML
    private Button addCustomButton;

    @FXML
    private TextArea statusArea;

    @FXML
    private TextArea customLdifArea;

    @FXML
    public void initialize() {
        updateStatus();
    }

    @FXML
    private void handleStart() {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            String baseDN = baseDnField.getText().trim();
            String password = adminPasswordField.getText();

            if (baseDN.isEmpty() || password.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Invalid Configuration",
                        "Base DN and Admin Password are required");
                return;
            }

            embeddedServer.start(port, baseDN, password);

            // Update UI
            startButton.setDisable(true);
            stopButton.setDisable(false);
            addSampleButton.setDisable(false);
            clearDataButton.setDisable(false);
            createConnectionButton.setDisable(false);
            addCustomButton.setDisable(false);

            portField.setDisable(true);
            baseDnField.setDisable(true);
            adminPasswordField.setDisable(true);

            updateStatus();

            showAlert(Alert.AlertType.INFORMATION, "Server Started",
                    "Embedded LDAP server started successfully!\n\n" +
                    "You can now:\n" +
                    "1. Add sample data\n" +
                    "2. Create a connection configuration\n" +
                    "3. Test with the other tabs");

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to start server: " + e.getMessage());
        }
    }

    @FXML
    private void handleStop() {
        try {
            embeddedServer.stop();

            // Update UI
            startButton.setDisable(false);
            stopButton.setDisable(true);
            addSampleButton.setDisable(true);
            clearDataButton.setDisable(true);
            createConnectionButton.setDisable(true);
            addCustomButton.setDisable(true);

            portField.setDisable(false);
            baseDnField.setDisable(false);
            adminPasswordField.setDisable(false);

            updateStatus();

            showAlert(Alert.AlertType.INFORMATION, "Server Stopped",
                    "Embedded LDAP server stopped");

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to stop server: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddSample() {
        try {
            embeddedServer.addSampleData();
            updateStatus();

            showAlert(Alert.AlertType.INFORMATION, "Sample Data Added",
                    "Sample data added successfully!\n\n" +
                    "5 users and 4 groups have been created.\n" +
                    "All users have password: password123");

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to add sample data: " + e.getMessage());
        }
    }

    @FXML
    private void handleClearData() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Clear");
        confirmAlert.setHeaderText("Clear All Data");
        confirmAlert.setContentText("Are you sure you want to clear all data?\nThis will remove all users and groups.");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    embeddedServer.clearAllData();
                    updateStatus();

                    showAlert(Alert.AlertType.INFORMATION, "Data Cleared",
                            "All data cleared successfully");

                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error",
                            "Failed to clear data: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleAddCustom() {
        String ldif = customLdifArea.getText().trim();

        if (ldif.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Empty LDIF",
                    "Please enter LDIF content");
            return;
        }

        try {
            embeddedServer.addCustomEntry(ldif);

            showAlert(Alert.AlertType.INFORMATION, "Entry Added",
                    "Custom entry added successfully");

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to add custom entry: " + e.getMessage());
        }
    }

    @FXML
    private void handleCreateConnection() {
        if (!embeddedServer.isRunning()) {
            showAlert(Alert.AlertType.WARNING, "Server Not Running",
                    "Server is not running");
            return;
        }

        LdapConnection connection = new LdapConnection();
        connection.setId(UUID.randomUUID().toString());
        connection.setName("Embedded Test Server");
        connection.setHost("localhost");
        connection.setPort(embeddedServer.getPort());
        connection.setBaseDn(embeddedServer.getBaseDN());
        connection.setUserDn("cn=admin," + embeddedServer.getBaseDN());
        connection.setPassword(adminPasswordField.getText());
        connection.setUseSsl(false);
        connection.setUserSearchBase("ou=users," + embeddedServer.getBaseDN());
        connection.setUserSearchFilter("(uid={0})");
        connection.setGroupSearchBase("ou=groups," + embeddedServer.getBaseDN());
        connection.setGroupSearchFilter("(member={0})");

        configService.saveConnection(connection);

        showAlert(Alert.AlertType.INFORMATION, "Connection Created",
                "Connection configuration created successfully!\n\n" +
                "Connection Name: Embedded Test Server\n" +
                "You can now use it in the Connections tab");
    }

    private void updateStatus() {
        Platform.runLater(() -> {
            if (embeddedServer.isRunning()) {
                statusLabel.setText("RUNNING");
                statusLabel.getStyleClass().clear();
                statusLabel.getStyleClass().add("status-connected");

                StringBuilder status = new StringBuilder();
                status.append("Server Status: RUNNING\n\n");
                status.append("Connection Details:\n");
                status.append("  Host: localhost\n");
                status.append("  Port: ").append(embeddedServer.getPort()).append("\n");
                status.append("  Base DN: ").append(embeddedServer.getBaseDN()).append("\n");
                status.append("  Admin DN: cn=admin,").append(embeddedServer.getBaseDN()).append("\n\n");
                status.append("Search Bases:\n");
                status.append("  Users: ou=users,").append(embeddedServer.getBaseDN()).append("\n");
                status.append("  Groups: ou=groups,").append(embeddedServer.getBaseDN()).append("\n");

                statusArea.setText(status.toString());
            } else {
                statusLabel.setText("STOPPED");
                statusLabel.getStyleClass().clear();
                statusLabel.getStyleClass().add("status-disconnected");

                statusArea.setText("Server Status: STOPPED\n\nClick 'Start Server' to begin");
            }
        });
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
