package com.ldapmanager.ui.javafx;

import com.ldapmanager.model.LdapConnection;
import com.ldapmanager.model.LdapUser;
import com.ldapmanager.service.ConfigurationService;
import com.ldapmanager.service.LdapService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class UserSearchPanelController {

    @Autowired
    private ConfigurationService configService;

    @Autowired
    private LdapService ldapService;

    @FXML
    private ComboBox<LdapConnection> connectionComboBox;

    @FXML
    private TextField searchField;

    @FXML
    private Button searchButton;

    @FXML
    private Button clearButton;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button authenticateButton;

    @FXML
    private TableView<LdapUser> resultsTable;

    @FXML
    private TableColumn<LdapUser, String> usernameColumn;

    @FXML
    private TableColumn<LdapUser, String> fullNameColumn;

    @FXML
    private TableColumn<LdapUser, String> emailColumn;

    @FXML
    private TableColumn<LdapUser, String> departmentColumn;

    @FXML
    private TableColumn<LdapUser, String> enabledColumn;

    @FXML
    private VBox userDetailsSection;

    @FXML
    private TextArea userDetailsArea;

    private ObservableList<LdapUser> users = FXCollections.observableArrayList();
    private LdapConnection currentConnection;
    private LdapUser selectedUser;

    @FXML
    public void initialize() {
        // Set up table columns
        usernameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        fullNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        emailColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        departmentColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDepartment()));
        enabledColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isEnabled() ? "Yes" : "No"));

        resultsTable.setItems(users);

        // Handle user selection
        resultsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                loadUserDetails(newSelection);
            }
        });

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
        handleClear();
    }

    @FXML
    private void handleSearch() {
        if (currentConnection == null) {
            showAlert(Alert.AlertType.WARNING, "No Connection", "Please select a connection first.");
            return;
        }

        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Empty Search", "Please enter a search term.");
            return;
        }

        searchButton.setDisable(true);
        searchButton.setText("Searching...");

        Task<List<LdapUser>> searchTask = new Task<>() {
            @Override
            protected List<LdapUser> call() throws Exception {
                return ldapService.searchUsers(currentConnection, searchTerm);
            }
        };

        searchTask.setOnSucceeded(event -> {
            List<LdapUser> foundUsers = searchTask.getValue();
            users.clear();
            users.addAll(foundUsers);

            if (foundUsers.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "No Results",
                        "No users found matching: " + searchTerm);
            }

            searchButton.setDisable(false);
            searchButton.setText("Search Users");
        });

        searchTask.setOnFailed(event -> {
            Throwable ex = searchTask.getException();
            String errorMsg = ex.getMessage();
            if (ex.getCause() != null) {
                errorMsg += "\nCause: " + ex.getCause().getMessage();
            }
            showAlert(Alert.AlertType.ERROR, "Search Failed", "Search failed:\n" + errorMsg);

            searchButton.setDisable(false);
            searchButton.setText("Search Users");
        });

        new Thread(searchTask).start();
    }

    @FXML
    private void handleClear() {
        users.clear();
        searchField.clear();
        userDetailsSection.setVisible(false);
        userDetailsSection.setManaged(false);
    }

    @FXML
    private void handleAuthenticate() {
        if (currentConnection == null) {
            showAlert(Alert.AlertType.WARNING, "No Connection", "Please select a connection first.");
            return;
        }

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Credentials",
                    "Please enter both username and password.");
            return;
        }

        authenticateButton.setDisable(true);
        authenticateButton.setText("Authenticating...");

        Task<Boolean> authTask = new Task<>() {
            @Override
            protected Boolean call() {
                return ldapService.authenticateUser(currentConnection, username, password);
            }
        };

        authTask.setOnSucceeded(event -> {
            boolean authenticated = authTask.getValue();
            if (authenticated) {
                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Authentication successful for user: " + username);

                // Load user details
                Task<LdapUser> loadTask = new Task<>() {
                    @Override
                    protected LdapUser call() throws Exception {
                        return ldapService.findUser(currentConnection, username);
                    }
                };

                loadTask.setOnSucceeded(e -> {
                    LdapUser user = loadTask.getValue();
                    if (user != null) {
                        loadUserDetails(user);
                    }
                });

                new Thread(loadTask).start();
            } else {
                showAlert(Alert.AlertType.ERROR, "Failed",
                        "Authentication failed for user: " + username);
            }

            authenticateButton.setDisable(false);
            authenticateButton.setText("Authenticate");
            passwordField.clear();
        });

        authTask.setOnFailed(event -> {
            Throwable ex = authTask.getException();
            String errorMsg = ex.getMessage();
            if (ex.getCause() != null) {
                errorMsg += "\nCause: " + ex.getCause().getMessage();
            }
            showAlert(Alert.AlertType.ERROR, "Authentication Error",
                    "Authentication error:\n" + errorMsg);

            authenticateButton.setDisable(false);
            authenticateButton.setText("Authenticate");
            passwordField.clear();
        });

        new Thread(authTask).start();
    }

    private void loadUserDetails(LdapUser user) {
        selectedUser = user;

        StringBuilder details = new StringBuilder();
        details.append("=== User Details ===\n\n");
        details.append("DN: ").append(user.getDn()).append("\n");
        details.append("Username: ").append(user.getUsername()).append("\n");
        details.append("CN: ").append(user.getCn()).append("\n");
        details.append("Full Name: ").append(user.getFullName()).append("\n");
        details.append("Email: ").append(user.getEmail()).append("\n");
        details.append("First Name: ").append(user.getFirstName()).append("\n");
        details.append("Last Name: ").append(user.getLastName()).append("\n");
        details.append("Display Name: ").append(user.getDisplayName()).append("\n");
        details.append("Telephone: ").append(user.getTelephone()).append("\n");
        details.append("Department: ").append(user.getDepartment()).append("\n");
        details.append("Title: ").append(user.getTitle()).append("\n");
        details.append("Enabled: ").append(user.isEnabled() ? "Yes" : "No").append("\n\n");

        if (user.getGroups() != null && !user.getGroups().isEmpty()) {
            details.append("=== Group Memberships ===\n");
            for (String group : user.getGroups()) {
                details.append("  - ").append(group).append("\n");
            }
            details.append("\n");
        }

        if (user.getAttributes() != null && !user.getAttributes().isEmpty()) {
            details.append("=== All Attributes ===\n");
            for (Map.Entry<String, List<String>> entry : user.getAttributes().entrySet()) {
                details.append(entry.getKey()).append(": ");
                details.append(String.join(", ", entry.getValue())).append("\n");
            }
        }

        userDetailsArea.setText(details.toString());
        userDetailsSection.setVisible(true);
        userDetailsSection.setManaged(true);
    }

    @FXML
    private void handleExport() {
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "No User Selected",
                    "Please select a user to export.");
            return;
        }

        try {
            String filename = "user_" + selectedUser.getUsername() + ".xlsx";

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("User Details");

            int rowNum = 0;

            // Header
            Row headerRow = sheet.createRow(rowNum++);
            headerRow.createCell(0).setCellValue("Attribute");
            headerRow.createCell(1).setCellValue("Value");

            // Basic info
            addRow(sheet, rowNum++, "DN", selectedUser.getDn());
            addRow(sheet, rowNum++, "Username", selectedUser.getUsername());
            addRow(sheet, rowNum++, "CN", selectedUser.getCn());
            addRow(sheet, rowNum++, "Full Name", selectedUser.getFullName());
            addRow(sheet, rowNum++, "Email", selectedUser.getEmail());
            addRow(sheet, rowNum++, "First Name", selectedUser.getFirstName());
            addRow(sheet, rowNum++, "Last Name", selectedUser.getLastName());
            addRow(sheet, rowNum++, "Display Name", selectedUser.getDisplayName());
            addRow(sheet, rowNum++, "Telephone", selectedUser.getTelephone());
            addRow(sheet, rowNum++, "Department", selectedUser.getDepartment());
            addRow(sheet, rowNum++, "Title", selectedUser.getTitle());
            addRow(sheet, rowNum++, "Enabled", selectedUser.isEnabled() ? "Yes" : "No");

            // Groups
            if (selectedUser.getGroups() != null && !selectedUser.getGroups().isEmpty()) {
                rowNum++;
                Row groupHeader = sheet.createRow(rowNum++);
                groupHeader.createCell(0).setCellValue("Groups");

                for (String group : selectedUser.getGroups()) {
                    Row groupRow = sheet.createRow(rowNum++);
                    groupRow.createCell(1).setCellValue(group);
                }
            }

            // Auto-size columns
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            // Write to file
            try (FileOutputStream fileOut = new FileOutputStream(filename)) {
                workbook.write(fileOut);
            }

            workbook.close();

            showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                    "User data exported to: " + filename);

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Export Failed",
                    "Failed to export user data: " + e.getMessage());
        }
    }

    private void addRow(Sheet sheet, int rowNum, String key, String value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(key);
        row.createCell(1).setCellValue(value != null ? value : "");
    }

    @FXML
    private void handleCopyDetails() {
        String details = userDetailsArea.getText();
        if (details != null && !details.isEmpty()) {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(details);
            clipboard.setContent(content);

            showAlert(Alert.AlertType.INFORMATION, "Copied", "User details copied to clipboard!");
        }
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
