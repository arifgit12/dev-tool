package com.oraclejms.ui;

import com.oraclejms.model.JmsMessage;
import com.oraclejms.model.OracleJmsConfig;
import com.oraclejms.service.ConnectionConfigService;
import com.oraclejms.service.OracleJmsService;
import com.oraclejms.util.XmlUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
public class MainStage {

    private final OracleJmsService jmsService;
    private final OracleJmsConfig jmsConfig;
    private final ConnectionConfigService configService;

    private TextArea xmlInputArea;
    private Button sendButton;
    private Button beautifyButton;
    private TextArea receivedMessagesArea;
    private Label statusLabel;
    private Label validationLabel;
    private Label templateLabel;
    private TextField sendQueueField;
    private ComboBox<String> receiveQueueCombo;
    private Button connectButton;
    private Button disconnectButton;
    private Button receiveButton;
    private Button clearHistoryButton;
    private ListView<String> historyListView;
    private Spinner<Integer> messageCountSpinner;
    
    // Editable connection fields
    private TextField providerUrlField;
    private TextField connectionFactoryField;
    private TextField usernameField;
    private PasswordField passwordField;
    private Button saveButton;
    private Button testButton;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public MainStage(OracleJmsService jmsService, OracleJmsConfig jmsConfig, ConnectionConfigService configService) {
        this.jmsService = jmsService;
        this.jmsConfig = jmsConfig;
        this.configService = configService;
    }

    public void start(Stage primaryStage) {
        primaryStage.setTitle("Oracle JMS Simulator - Professional Edition");
        
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #2b2b2b;");
        
        // Load saved configuration on startup
        loadSavedConfiguration();

        // Top Panel - Connection
        VBox topPanel = createConnectionPanel();
        root.setTop(topPanel);

        // Center Panel - Main Content
        SplitPane centerPanel = createMainContentPanel();
        root.setCenter(centerPanel);

        // Bottom Panel - Status
        HBox bottomPanel = createStatusPanel();
        root.setBottom(bottomPanel);

        Scene scene = new Scene(root, 1400, 900);
        
        // Make window resizable and set minimum size
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.setScene(scene);
        primaryStage.show();

        updateConnectionState(false);
    }
    
    /**
     * Load saved configuration from database
     */
    private void loadSavedConfiguration() {
        configService.getDefaultConfiguration().ifPresent(config -> {
            log.info("Loading saved configuration from database");
            jmsService.updateConnectionParameters(
                config.getProviderUrl(),
                config.getConnectionFactory(),
                config.getUsername(),
                config.getPassword()
            );
        });
    }

    private VBox createConnectionPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: #1e1e1e; -fx-border-color: #3e3e3e; -fx-border-width: 0 0 1 0;");

        Label titleLabel = new Label("Oracle JMS Connection");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.web("#e0e0e0"));

        GridPane connectionGrid = new GridPane();
        connectionGrid.setHgap(10);
        connectionGrid.setVgap(8);
        connectionGrid.setPadding(new Insets(10, 0, 0, 0));

        int row = 0;
        
        // JMS Provider URL
        Label providerUrlLabel = new Label("JMS Provider URL:");
        providerUrlLabel.setTextFill(Color.web("#b0b0b0"));
        providerUrlLabel.setFont(Font.font("System", 12));
        providerUrlField = new TextField(jmsConfig.getProviderUrl());
        providerUrlField.setStyle("-fx-control-inner-background: #3e3e3e; -fx-text-fill: #e0e0e0;");
        providerUrlField.setPromptText("e.g., t3://localhost:7001");
        connectionGrid.add(providerUrlLabel, 0, row);
        connectionGrid.add(providerUrlField, 1, row++);
        
        // Connection Factory
        Label connectionFactoryLabel = new Label("Connection Factory:");
        connectionFactoryLabel.setTextFill(Color.web("#b0b0b0"));
        connectionFactoryLabel.setFont(Font.font("System", 12));
        connectionFactoryField = new TextField(jmsConfig.getConnectionFactory());
        connectionFactoryField.setStyle("-fx-control-inner-background: #3e3e3e; -fx-text-fill: #e0e0e0;");
        connectionFactoryField.setPromptText("e.g., weblogic.jms.ConnectionFactory");
        connectionGrid.add(connectionFactoryLabel, 0, row);
        connectionGrid.add(connectionFactoryField, 1, row++);
        
        // Username
        Label usernameLabel = new Label("Username:");
        usernameLabel.setTextFill(Color.web("#b0b0b0"));
        usernameLabel.setFont(Font.font("System", 12));
        usernameField = new TextField(jmsConfig.getUser());
        usernameField.setStyle("-fx-control-inner-background: #3e3e3e; -fx-text-fill: #e0e0e0;");
        usernameField.setPromptText("Username");
        connectionGrid.add(usernameLabel, 0, row);
        connectionGrid.add(usernameField, 1, row++);
        
        // Password
        Label passwordLabel = new Label("Password:");
        passwordLabel.setTextFill(Color.web("#b0b0b0"));
        passwordLabel.setFont(Font.font("System", 12));
        passwordField = new PasswordField();
        passwordField.setText(jmsConfig.getPassword());
        passwordField.setStyle("-fx-control-inner-background: #3e3e3e; -fx-text-fill: #e0e0e0;");
        passwordField.setPromptText("Password");
        connectionGrid.add(passwordLabel, 0, row);
        connectionGrid.add(passwordField, 1, row++);
        
        // Make text fields grow horizontally
        providerUrlField.setMaxWidth(Double.MAX_VALUE);
        connectionFactoryField.setMaxWidth(Double.MAX_VALUE);
        usernameField.setMaxWidth(Double.MAX_VALUE);
        passwordField.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(providerUrlField, Priority.ALWAYS);
        GridPane.setHgrow(connectionFactoryField, Priority.ALWAYS);
        GridPane.setHgrow(usernameField, Priority.ALWAYS);
        GridPane.setHgrow(passwordField, Priority.ALWAYS);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        
        saveButton = createStyledButton("Save", "#2196F3");
        testButton = createStyledButton("Test Connection", "#FF9800");
        connectButton = createStyledButton("Connect", "#4CAF50");
        disconnectButton = createStyledButton("Disconnect", "#f44336");
        
        saveButton.setOnAction(e -> saveConfiguration());
        testButton.setOnAction(e -> testConnection());
        connectButton.setOnAction(e -> connectToJms());
        disconnectButton.setOnAction(e -> disconnectFromJms());
        
        buttonBox.getChildren().addAll(saveButton, testButton, connectButton, disconnectButton);

        panel.getChildren().addAll(titleLabel, connectionGrid, buttonBox);
        return panel;
    }
    
    private void saveConfiguration() {
        String providerUrl = providerUrlField.getText().trim();
        String connectionFactory = connectionFactoryField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        if (providerUrl.isEmpty() || connectionFactory.isEmpty()) {
            showAlert("Validation Error", "Provider URL and Connection Factory are required", Alert.AlertType.WARNING);
            return;
        }
        
        try {
            configService.saveConfiguration(providerUrl, connectionFactory, username, password);
            jmsService.updateConnectionParameters(providerUrl, connectionFactory, username, password);
            showStatus("Configuration saved successfully", "#4CAF50");
            showAlert("Success", "Connection configuration saved successfully", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            log.error("Failed to save configuration", e);
            showAlert("Error", "Failed to save configuration: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void testConnection() {
        String providerUrl = providerUrlField.getText().trim();
        String connectionFactory = connectionFactoryField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        if (providerUrl.isEmpty() || connectionFactory.isEmpty()) {
            showAlert("Validation Error", "Provider URL and Connection Factory are required", Alert.AlertType.WARNING);
            return;
        }
        
        // Update parameters temporarily for testing
        jmsService.updateConnectionParameters(providerUrl, connectionFactory, username, password);
        
        new Thread(() -> {
            boolean success = jmsService.testConnection();
            Platform.runLater(() -> {
                if (success) {
                    showStatus("Connection test successful", "#4CAF50");
                    showAlert("Success", "Connection test successful!", Alert.AlertType.INFORMATION);
                } else {
                    showStatus("Connection test failed", "#f44336");
                    showAlert("Connection Test Failed", 
                        "Connection test failed. Please check:\n\n" +
                        "1. WebLogic client JAR is installed (see README.md)\n" +
                        "2. Provider URL is correct\n" +
                        "3. WebLogic server is running\n" +
                        "4. Network connectivity to the server\n" +
                        "5. Credentials are valid\n\n" +
                        "Check the console logs for detailed error information.", 
                        Alert.AlertType.ERROR);
                }
            });
        }).start();
    }

    private void addConnectionField(GridPane grid, int row, String labelText, String value) {
        Label label = new Label(labelText);
        label.setTextFill(Color.web("#b0b0b0"));
        label.setFont(Font.font("System", 12));
        
        Label valueLabel = new Label(value);
        valueLabel.setTextFill(Color.web("#e0e0e0"));
        valueLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        
        grid.add(label, 0, row);
        grid.add(valueLabel, 1, row);
    }

    private SplitPane createMainContentPanel() {
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.5);

        // Left Panel - Send Messages
        VBox sendPanel = createSendMessagePanel();
        
        // Right Panel - Receive Messages
        VBox receivePanel = createReceiveMessagePanel();

        splitPane.getItems().addAll(sendPanel, receivePanel);
        return splitPane;
    }

    private VBox createSendMessagePanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: #2b2b2b;");

        Label titleLabel = new Label("Send Message");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.web("#e0e0e0"));

        HBox queueBox = new HBox(10);
        queueBox.setAlignment(Pos.CENTER_LEFT);
        Label queueLabel = new Label("Queue:");
        queueLabel.setTextFill(Color.web("#b0b0b0"));
        sendQueueField = new TextField();
        sendQueueField.setText(jmsConfig.getQueue().getOut());
        sendQueueField.setPromptText("Enter queue name (e.g., DEV.QUEUE.2)");
        sendQueueField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(sendQueueField, Priority.ALWAYS);
        sendQueueField.setStyle("-fx-control-inner-background: #3e3e3e; -fx-text-fill: #e0e0e0;");
        queueBox.getChildren().addAll(queueLabel, sendQueueField);
        
        // Message count spinner
        HBox countBox = new HBox(10);
        countBox.setAlignment(Pos.CENTER_LEFT);
        Label countLabel = new Label("Message Count:");
        countLabel.setTextFill(Color.web("#b0b0b0"));
        messageCountSpinner = new Spinner<>(1, 20000, 1);
        messageCountSpinner.setEditable(true);
        messageCountSpinner.setPrefWidth(100);
        messageCountSpinner.setStyle("-fx-background-color: #3e3e3e;");
        Label countHelpLabel = new Label("(1-20000 messages)");
        countHelpLabel.setTextFill(Color.web("#757575"));
        countHelpLabel.setFont(Font.font("System", 10));
        countBox.getChildren().addAll(countLabel, messageCountSpinner, countHelpLabel);

        Label xmlLabel = new Label("XML Message Content:");
        xmlLabel.setTextFill(Color.web("#b0b0b0"));

        xmlInputArea = new TextArea();
        xmlInputArea.setPromptText("Enter XML content here...\n\nExample:\n<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<message>\n  <text>Hello Oracle JMS</text>\n</message>\n\nUse ${PLACEHOLDER} for dynamic values:\n${ID}, ${UUID}, ${NAME}, ${EMAIL}, ${DATE}, etc.");
        xmlInputArea.setPrefRowCount(12);
        xmlInputArea.setWrapText(true);
        xmlInputArea.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: #e0e0e0; " +
                              "-fx-font-family: 'Courier New'; -fx-font-size: 12px;");
        xmlInputArea.textProperty().addListener((obs, oldVal, newVal) -> validateXml(newVal));

        validationLabel = new Label("");
        validationLabel.setFont(Font.font("System", 11));
        
        // Template indicator label
        templateLabel = new Label("");
        templateLabel.setFont(Font.font("System", 11));
        templateLabel.setTextFill(Color.web("#FF9800"));
        templateLabel.setVisible(false);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        
        beautifyButton = createStyledButton("Beautify XML", "#2196F3");
        sendButton = createStyledButton("Send Message(s)", "#4CAF50");
        Button clearButton = createStyledButton("Clear", "#757575");
        
        beautifyButton.setOnAction(e -> beautifyXml());
        sendButton.setOnAction(e -> sendMessage());
        clearButton.setOnAction(e -> xmlInputArea.clear());
        
        buttonBox.getChildren().addAll(beautifyButton, sendButton, clearButton);

        // Message History
        Label historyLabel = new Label("Message History:");
        historyLabel.setTextFill(Color.web("#b0b0b0"));
        
        historyListView = new ListView<>();
        historyListView.setPrefHeight(120);
        historyListView.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: #e0e0e0;");
        
        clearHistoryButton = createStyledButton("Clear History", "#757575");
        clearHistoryButton.setOnAction(e -> clearHistory());

        VBox.setVgrow(xmlInputArea, Priority.ALWAYS);
        panel.getChildren().addAll(
            titleLabel, queueBox, countBox, xmlLabel, xmlInputArea, validationLabel, templateLabel,
            buttonBox, historyLabel, historyListView, clearHistoryButton
        );
        
        return panel;
    }

    private VBox createReceiveMessagePanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: #2b2b2b;");

        Label titleLabel = new Label("Receive Messages");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.web("#e0e0e0"));

        HBox queueBox = new HBox(10);
        queueBox.setAlignment(Pos.CENTER_LEFT);
        Label queueLabel = new Label("Queue:");
        queueLabel.setTextFill(Color.web("#b0b0b0"));
        receiveQueueCombo = new ComboBox<>();
        receiveQueueCombo.getItems().addAll(jmsConfig.getQueue().getIn(), jmsConfig.getQueue().getOut());
        receiveQueueCombo.setValue(jmsConfig.getQueue().getIn());
        receiveQueueCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(receiveQueueCombo, Priority.ALWAYS);
        receiveQueueCombo.setStyle("-fx-background-color: #3e3e3e; -fx-text-fill: #e0e0e0;");
        queueBox.getChildren().addAll(queueLabel, receiveQueueCombo);

        receivedMessagesArea = new TextArea();
        receivedMessagesArea.setEditable(false);
        receivedMessagesArea.setWrapText(true);
        receivedMessagesArea.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: #e0e0e0; " +
                                      "-fx-font-family: 'Courier New'; -fx-font-size: 12px;");

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        
        receiveButton = createStyledButton("Receive Messages", "#FF9800");
        Button clearReceiveButton = createStyledButton("Clear", "#757575");
        
        receiveButton.setOnAction(e -> receiveMessages());
        clearReceiveButton.setOnAction(e -> receivedMessagesArea.clear());
        
        buttonBox.getChildren().addAll(receiveButton, clearReceiveButton);

        VBox.setVgrow(receivedMessagesArea, Priority.ALWAYS);
        panel.getChildren().addAll(titleLabel, queueBox, receivedMessagesArea, buttonBox);
        
        return panel;
    }

    private HBox createStatusPanel() {
        HBox panel = new HBox();
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #1e1e1e; -fx-border-color: #3e3e3e; -fx-border-width: 1 0 0 0;");

        statusLabel = new Label("Disconnected");
        statusLabel.setTextFill(Color.web("#f44336"));
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        panel.getChildren().add(statusLabel);
        return panel;
    }

    private Button createStyledButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle(String.format(
            "-fx-background-color: %s; -fx-text-fill: white; " +
            "-fx-font-weight: bold; -fx-padding: 8 16; " +
            "-fx-cursor: hand; -fx-background-radius: 4;", color));
        
        button.setOnMouseEntered(e -> 
            button.setStyle(String.format(
                "-fx-background-color: derive(%s, -10%%); -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-padding: 8 16; " +
                "-fx-cursor: hand; -fx-background-radius: 4;", color)));
        
        button.setOnMouseExited(e -> 
            button.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-padding: 8 16; " +
                "-fx-cursor: hand; -fx-background-radius: 4;", color)));
        
        return button;
    }

    private void validateXml(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            validationLabel.setText("");
            validationLabel.setTextFill(Color.web("#b0b0b0"));
            templateLabel.setVisible(false);
            sendButton.setDisable(true);
            return;
        }
        
        // Check for template variables
        boolean hasTemplates = com.oraclejms.util.TemplateUtil.hasTemplateVariables(xml);
        if (hasTemplates) {
            templateLabel.setText("ℹ Dynamic parameters detected - each message will have unique values");
            templateLabel.setVisible(true);
        } else {
            templateLabel.setVisible(false);
        }

        boolean isValid = XmlUtil.isValidXml(xml);
        if (isValid) {
            validationLabel.setText("✓ Valid XML");
            validationLabel.setTextFill(Color.web("#4CAF50"));
            sendButton.setDisable(!jmsService.isConnected());
        } else {
            String error = XmlUtil.getXmlError(xml);
            validationLabel.setText("✗ " + error);
            validationLabel.setTextFill(Color.web("#f44336"));
            sendButton.setDisable(true);
        }
    }

    private void beautifyXml() {
        String xml = xmlInputArea.getText();
        if (xml == null || xml.trim().isEmpty()) {
            showAlert("Error", "Please enter XML content first", Alert.AlertType.WARNING);
            return;
        }

        try {
            String beautified = XmlUtil.beautifyXml(xml);
            xmlInputArea.setText(beautified);
            showStatus("XML beautified successfully", "#4CAF50");
        } catch (Exception e) {
            showAlert("Error", "Failed to beautify XML: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void connectToJms() {
        new Thread(() -> {
            try {
                jmsService.connect();
                Platform.runLater(() -> {
                    updateConnectionState(true);
                    showStatus("Connected to " + jmsService.getCurrentProviderUrl(), "#4CAF50");
                });
            } catch (JMSException e) {
                log.error("Connection failed", e);
                Platform.runLater(() -> 
                    showAlert("Connection Error", "Failed to connect: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        }).start();
    }

    private void disconnectFromJms() {
        jmsService.disconnect();
        updateConnectionState(false);
        showStatus("Disconnected", "#f44336");
    }

    private void sendMessage() {
        String xml = xmlInputArea.getText();
        String queue = sendQueueField.getText().trim();
        int messageCount = messageCountSpinner.getValue();
        
        if (queue.isEmpty()) {
            showAlert("Error", "Queue name cannot be empty", Alert.AlertType.ERROR);
            return;
        }

        if (!XmlUtil.isValidXml(xml)) {
            showAlert("Error", "Invalid XML content", Alert.AlertType.ERROR);
            return;
        }
        
        if (messageCount < 1 || messageCount > 20000) {
            showAlert("Error", "Message count must be between 1 and 20000", Alert.AlertType.ERROR);
            return;
        }
        
        // Disable send button during sending
        sendButton.setDisable(true);
        
        if (messageCount == 1) {
            // Single message - use original method
            new Thread(() -> {
                try {
                    String processedXml = com.oraclejms.util.TemplateUtil.processTemplate(xml);
                    jmsService.sendMessage(queue, processedXml);
                    Platform.runLater(() -> {
                        showStatus("Message sent successfully to " + queue, "#4CAF50");
                        updateHistoryList();
                        sendButton.setDisable(!jmsService.isConnected());
                    });
                } catch (JMSException e) {
                    log.error("Failed to send message", e);
                    Platform.runLater(() -> {
                        showAlert("Send Error", "Failed to send message: " + e.getMessage(), Alert.AlertType.ERROR);
                        sendButton.setDisable(!jmsService.isConnected());
                    });
                }
            }).start();
        } else {
            // Multiple messages - use batch method with progress
            showStatus("Sending " + messageCount + " messages to " + queue + "...", "#FF9800");
            
            new Thread(() -> {
                try {
                    jmsService.sendMessagesAsync(queue, xml, messageCount, (sent, total) -> {
                        Platform.runLater(() -> {
                            showStatus(String.format("Sending messages: %d/%d (%.1f%%)", 
                                sent, total, (sent * 100.0 / total)), "#FF9800");
                        });
                    });
                    
                    Platform.runLater(() -> {
                        showStatus("Successfully sent " + messageCount + " message(s) to " + queue, "#4CAF50");
                        updateHistoryList();
                        sendButton.setDisable(!jmsService.isConnected());
                    });
                } catch (JMSException e) {
                    log.error("Failed to send messages", e);
                    Platform.runLater(() -> {
                        showAlert("Send Error", "Failed to send messages: " + e.getMessage(), Alert.AlertType.ERROR);
                        sendButton.setDisable(!jmsService.isConnected());
                    });
                }
            }).start();
        }
    }

    private void receiveMessages() {
        String queue = receiveQueueCombo.getValue();
        
        new Thread(() -> {
            try {
                List<JmsMessage> messages = jmsService.receiveMessages(queue, 10);
                Platform.runLater(() -> {
                    if (messages.isEmpty()) {
                        receivedMessagesArea.appendText("\n[" + java.time.LocalDateTime.now().format(TIME_FORMATTER) + "] No messages available\n");
                    } else {
                        for (JmsMessage msg : messages) {
                            receivedMessagesArea.appendText(
                                "\n========================================\n" +
                                "Time: " + msg.getTimestamp().format(TIME_FORMATTER) + "\n" +
                                "Queue: " + msg.getQueue() + "\n" +
                                "Message ID: " + msg.getMessageId() + "\n" +
                                "----------------------------------------\n" +
                                msg.getContent() + "\n" +
                                "========================================\n"
                            );
                        }
                        showStatus("Received " + messages.size() + " message(s) from " + queue, "#4CAF50");
                        updateHistoryList();
                    }
                });
            } catch (JMSException e) {
                log.error("Failed to receive messages", e);
                Platform.runLater(() -> 
                    showAlert("Receive Error", "Failed to receive messages: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        }).start();
    }

    private void clearHistory() {
        jmsService.clearHistory();
        historyListView.getItems().clear();
        showStatus("History cleared", "#4CAF50");
    }

    private void updateHistoryList() {
        historyListView.getItems().clear();
        List<JmsMessage> history = jmsService.getMessageHistory();
        for (JmsMessage msg : history) {
            String icon = msg.getType() == JmsMessage.MessageType.SENT ? "→" : "←";
            String item = String.format("%s %s [%s] %s", 
                icon,
                msg.getTimestamp().format(TIME_FORMATTER),
                msg.getQueue(),
                msg.getType());
            historyListView.getItems().add(item);
        }
    }

    private void updateConnectionState(boolean connected) {
        connectButton.setDisable(connected);
        disconnectButton.setDisable(!connected);
        receiveButton.setDisable(!connected);
        beautifyButton.setDisable(false);
        
        // Update send button based on connection and current validation state
        String xml = xmlInputArea.getText();
        sendButton.setDisable(!connected || xml == null || xml.trim().isEmpty() || !XmlUtil.isValidXml(xml));
        
        if (connected) {
            statusLabel.setText("Connected to " + jmsService.getCurrentProviderUrl());
            statusLabel.setTextFill(Color.web("#4CAF50"));
        } else {
            statusLabel.setText("Disconnected");
            statusLabel.setTextFill(Color.web("#f44336"));
        }
    }

    private void showStatus(String message, String color) {
        statusLabel.setText(message);
        statusLabel.setTextFill(Color.web(color));
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
