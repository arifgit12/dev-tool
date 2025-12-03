package com.ldapmanager.ui.javafx;

import com.ldapmanager.model.LdapConnection;
import com.ldapmanager.service.ConfigurationService;
import com.ldapmanager.service.LdapService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class ConnectionPanelController {

    @Autowired
    private ConfigurationService configService;

    @Autowired
    private LdapService ldapService;

    @FXML
    private TableView<LdapConnection> connectionTable;

    @FXML
    private TableColumn<LdapConnection, String> nameColumn;

    @FXML
    private TableColumn<LdapConnection, String> hostColumn;

    @FXML
    private TableColumn<LdapConnection, String> portColumn;

    @FXML
    private TableColumn<LdapConnection, String> baseDnColumn;

    @FXML
    private TableColumn<LdapConnection, String> sslColumn;

    @FXML
    private Button addButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button testButton;

    private ObservableList<LdapConnection> connections = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Set up table columns
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        hostColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getHost()));
        portColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getPort())));
        baseDnColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBaseDn()));
        sslColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isUseSsl() ? "Yes" : "No"));

        connectionTable.setItems(connections);

        // Load connections
        loadConnections();
    }

    private void loadConnections() {
        connections.clear();
        List<LdapConnection> loadedConnections = configService.loadConnections();
        connections.addAll(loadedConnections);
    }

    @FXML
    private void handleAdd() {
        showConnectionDialog(null);
    }

    @FXML
    private void handleEdit() {
        LdapConnection selected = connectionTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showConnectionDialog(selected);
        } else {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a connection to edit.");
        }
    }

    @FXML
    private void handleDelete() {
        LdapConnection selected = connectionTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Delete");
            confirmAlert.setHeaderText("Delete Connection");
            confirmAlert.setContentText("Are you sure you want to delete this connection?");

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                configService.deleteConnection(selected.getId());
                loadConnections();
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a connection to delete.");
        }
    }

    @FXML
    private void handleTest() {
        LdapConnection selected = connectionTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Create progress indicator using ProgressIndicator
            ProgressIndicator progressIndicator = new ProgressIndicator();
            progressIndicator.setPrefSize(50, 50);

            VBox progressBox = new VBox(10);
            progressBox.setAlignment(javafx.geometry.Pos.CENTER);
            progressBox.getChildren().addAll(
                progressIndicator,
                new Label("Testing connection to " + selected.getHost() + "..."),
                new Label("Please wait...")
            );
            progressBox.setPadding(new Insets(20));

            Alert progressAlert = new Alert(Alert.AlertType.NONE);
            progressAlert.setTitle("Testing Connection");
            progressAlert.getDialogPane().setContent(progressBox);

            // Add Cancel button
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            progressAlert.getButtonTypes().setAll(cancelButton);

            // Test connection in background
            Task<Boolean> testTask = new Task<>() {
                @Override
                protected Boolean call() {
                    return ldapService.testConnection(selected);
                }
            };

            testTask.setOnSucceeded(event -> {
                Platform.runLater(() -> {
                    if (progressAlert.isShowing()) {
                        progressAlert.close();
                    }
                    boolean success = testTask.getValue();
                    if (success) {
                        showAlert(Alert.AlertType.INFORMATION, "Success",
                                "Connection successful!\n\nServer: " + selected.getHost() + ":" + selected.getPort());
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Failed",
                                "Connection failed!\n\nPlease verify:\n" +
                                "- Server is accessible\n" +
                                "- Credentials are correct\n" +
                                "- Base DN is valid");
                    }
                });
            });

            testTask.setOnFailed(event -> {
                Platform.runLater(() -> {
                    if (progressAlert.isShowing()) {
                        progressAlert.close();
                    }
                    Throwable ex = testTask.getException();
                    String errorMsg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
                    showAlert(Alert.AlertType.ERROR, "Error",
                            "Error testing connection:\n\n" + errorMsg);
                });
            });

            testTask.setOnCancelled(event -> {
                Platform.runLater(() -> {
                    if (progressAlert.isShowing()) {
                        progressAlert.close();
                    }
                });
            });

            // Handle Cancel button click
            progressAlert.setOnCloseRequest(evt -> {
                if (testTask.isRunning()) {
                    testTask.cancel();
                }
            });

            Thread testThread = new Thread(testTask);
            testThread.setDaemon(true);
            testThread.start();

            progressAlert.showAndWait();
        } else {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a connection to test.");
        }
    }

    private void showConnectionDialog(LdapConnection connection) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/connection-dialog.fxml"));

            // Create a controller for the dialog
            ConnectionDialogController dialogController = new ConnectionDialogController(connection, configService, this);
            loader.setController(dialogController);

            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());

            Stage dialogStage = new Stage();
            dialogStage.setTitle(connection == null ? "Add Connection" : "Edit Connection");
            dialogStage.setScene(scene);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setResizable(false);
            dialogStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load connection dialog: " + e.getMessage());
        }
    }

    public void refreshConnections() {
        loadConnections();
    }

    public LdapConnection getSelectedConnection() {
        return connectionTable.getSelectionModel().getSelectedItem();
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
