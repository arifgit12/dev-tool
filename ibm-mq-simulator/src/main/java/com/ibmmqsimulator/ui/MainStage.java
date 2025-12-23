package com.ibmmqsimulator.ui;

import com.ibmmqsimulator.model.MqConfig;
import com.ibmmqsimulator.model.MqMessage;
import com.ibmmqsimulator.service.DynamicMqService;
import com.ibmmqsimulator.service.MqService;
import com.ibmmqsimulator.util.TemplateUtil;
import com.ibmmqsimulator.util.XmlUtil;
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
import javax.jms.Message;
import javax.jms.TextMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
public class MainStage {

    private final MqService mqService;
    private final MqConfig mqConfig;
    private final DynamicMqService dynamicMqService;

    private TextArea xmlInputArea;
    private Button sendButton;
    private Button beautifyButton;
    private TextArea receivedMessagesArea;
    private Label statusLabel;
    private Label validationLabel;
    private ComboBox<String> sendQueueCombo;
    private ComboBox<String> receiveQueueCombo;
    private Button connectButton;
    private Button disconnectButton;
    private Button receiveButton;
    private Button clearHistoryButton;
    private ListView<String> historyListView;
    private Spinner<Integer> messageCountSpinner;
    private Spinner<Integer> threadCountSpinner;
    private ProgressBar sendProgressBar;
    private Label progressLabel;
    private Label templateInfoLabel;
    
    // Dynamic tab components
    private Button dynamicSendButton;
    private Button dynamicReceiveButton;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public MainStage(MqService mqService, MqConfig mqConfig, DynamicMqService dynamicMqService) {
        this.mqService = mqService;
        this.mqConfig = mqConfig;
        this.dynamicMqService = dynamicMqService;
    }

    public void start(Stage primaryStage) {
        primaryStage.setTitle("IBM MQ Simulator - Professional Edition");
        
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #2b2b2b;");

        // Create TabPane for multiple tabs
        TabPane tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: #2b2b2b;");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setTabMinHeight(40);
        tabPane.setTabMaxHeight(40);

        // Tab 1: Configured Connection (from properties)
        Tab configuredTab = new Tab("📋 Configured Connection");
        configuredTab.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        BorderPane configuredContent = new BorderPane();
        configuredContent.setStyle("-fx-background-color: #2b2b2b;");
        
        // Top Panel - Connection
        VBox topPanel = createConnectionPanel();
        configuredContent.setTop(topPanel);

        // Center Panel - Main Content
        SplitPane centerPanel = createMainContentPanel();
        configuredContent.setCenter(centerPanel);

        // Bottom Panel - Status
        HBox bottomPanel = createStatusPanel();
        configuredContent.setBottom(bottomPanel);
        
        configuredTab.setContent(configuredContent);

        // Tab 2: Dynamic Configuration
        Tab dynamicTab = new Tab("🔧 Dynamic Configuration");
        dynamicTab.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        BorderPane dynamicContent = createDynamicConfigurationTab();
        dynamicTab.setContent(dynamicContent);

        // Add tabs to TabPane
        tabPane.getTabs().addAll(configuredTab, dynamicTab);

        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1400, 900);
        
        // Load stylesheet if available
        var styleResource = getClass().getResource("/styles.css");
        if (styleResource != null) {
            scene.getStylesheets().add(styleResource.toExternalForm());
        }
        
        primaryStage.setScene(scene);
        primaryStage.show();

        updateConnectionState(false);
    }

    private VBox createConnectionPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: #1e1e1e; -fx-border-color: #3e3e3e; -fx-border-width: 0 0 1 0;");

        Label titleLabel = new Label("IBM MQ Connection");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.web("#e0e0e0"));

        GridPane connectionGrid = new GridPane();
        connectionGrid.setHgap(10);
        connectionGrid.setVgap(8);
        connectionGrid.setPadding(new Insets(10, 0, 0, 0));

        int row = 0;
        addConnectionField(connectionGrid, row++, "Queue Manager:", mqConfig.getQueueManager());
        addConnectionField(connectionGrid, row++, "Channel:", mqConfig.getChannel());
        addConnectionField(connectionGrid, row++, "Connection:", mqConfig.getConnName());
        addConnectionField(connectionGrid, row++, "User:", mqConfig.getUser());

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        
        connectButton = createStyledButton("Connect", "#4CAF50");
        disconnectButton = createStyledButton("Disconnect", "#f44336");
        
        connectButton.setOnAction(e -> connectToMq());
        disconnectButton.setOnAction(e -> disconnectFromMq());
        
        buttonBox.getChildren().addAll(connectButton, disconnectButton);

        panel.getChildren().addAll(titleLabel, connectionGrid, buttonBox);
        return panel;
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
        sendQueueCombo = new ComboBox<>();
        sendQueueCombo.getItems().addAll(mqConfig.getQueue().getIn(), mqConfig.getQueue().getOut());
        sendQueueCombo.setValue(mqConfig.getQueue().getOut());
        sendQueueCombo.setPrefWidth(200);
        sendQueueCombo.setStyle("-fx-background-color: #3e3e3e; -fx-text-fill: #e0e0e0;");
        queueBox.getChildren().addAll(queueLabel, sendQueueCombo);

        // Message count and thread configuration
        HBox configBox = new HBox(15);
        configBox.setAlignment(Pos.CENTER_LEFT);
        
        Label countLabel = new Label("Message Count:");
        countLabel.setTextFill(Color.web("#b0b0b0"));
        messageCountSpinner = new Spinner<>(1, 20000, 1);
        messageCountSpinner.setEditable(true);
        messageCountSpinner.setPrefWidth(100);
        messageCountSpinner.setStyle("-fx-background-color: #3e3e3e;");
        
        Label threadLabel = new Label("Threads:");
        threadLabel.setTextFill(Color.web("#b0b0b0"));
        threadCountSpinner = new Spinner<>(1, 100, 1);
        threadCountSpinner.setEditable(true);
        threadCountSpinner.setPrefWidth(80);
        threadCountSpinner.setStyle("-fx-background-color: #3e3e3e;");
        
        configBox.getChildren().addAll(countLabel, messageCountSpinner, threadLabel, threadCountSpinner);

        Label xmlLabel = new Label("XML Message Content:");
        xmlLabel.setTextFill(Color.web("#b0b0b0"));

        xmlInputArea = new TextArea();
        xmlInputArea.setPromptText("Enter XML content here...\n\nExample:\n<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<message>\n  <text>Hello IBM MQ</text>\n</message>");
        xmlInputArea.setPrefRowCount(15);
        xmlInputArea.setWrapText(true);
        xmlInputArea.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: #e0e0e0; " +
                              "-fx-font-family: 'Courier New'; -fx-font-size: 12px;");
        xmlInputArea.textProperty().addListener((obs, oldVal, newVal) -> validateXml(newVal));

        validationLabel = new Label("");
        validationLabel.setFont(Font.font("System", 11));

        // Template info label
        templateInfoLabel = new Label("");
        templateInfoLabel.setFont(Font.font("System", 11));
        templateInfoLabel.setTextFill(Color.web("#FF9800"));

        // Progress bar for multi-threaded sending
        progressLabel = new Label("");
        progressLabel.setTextFill(Color.web("#b0b0b0"));
        progressLabel.setFont(Font.font("System", 11));
        
        sendProgressBar = new ProgressBar(0);
        sendProgressBar.setPrefWidth(400);
        sendProgressBar.setVisible(false);
        sendProgressBar.setStyle("-fx-accent: #4CAF50;");

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        
        beautifyButton = createStyledButton("Beautify XML", "#2196F3");
        sendButton = createStyledButton("Send Message", "#4CAF50");
        Button clearButton = createStyledButton("Clear", "#757575");
        
        beautifyButton.setOnAction(e -> beautifyXml());
        sendButton.setOnAction(e -> sendMessage());
        clearButton.setOnAction(e -> xmlInputArea.clear());
        
        buttonBox.getChildren().addAll(beautifyButton, sendButton, clearButton);

        // Message History
        Label historyLabel = new Label("Message History:");
        historyLabel.setTextFill(Color.web("#b0b0b0"));
        
        historyListView = new ListView<>();
        historyListView.setPrefHeight(150);
        historyListView.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: #e0e0e0;");
        
        clearHistoryButton = createStyledButton("Clear History", "#757575");
        clearHistoryButton.setOnAction(e -> clearHistory());

        VBox.setVgrow(xmlInputArea, Priority.ALWAYS);
        panel.getChildren().addAll(
            titleLabel, queueBox, configBox, xmlLabel, xmlInputArea, validationLabel, templateInfoLabel,
            progressLabel, sendProgressBar, 
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
        receiveQueueCombo.getItems().addAll(mqConfig.getQueue().getIn(), mqConfig.getQueue().getOut());
        receiveQueueCombo.setValue(mqConfig.getQueue().getIn());
        receiveQueueCombo.setPrefWidth(200);
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
            templateInfoLabel.setText("");
            sendButton.setDisable(true);
            return;
        }

        boolean isValid = XmlUtil.isValidXml(xml);
        if (isValid) {
            validationLabel.setText("✓ Valid XML");
            validationLabel.setTextFill(Color.web("#4CAF50"));
            sendButton.setDisable(!mqService.isConnected());
            
            // Check for template variables
            if (TemplateUtil.hasPlaceholders(xml)) {
                templateInfoLabel.setText("ℹ Dynamic parameters detected - each message will have unique values");
                templateInfoLabel.setTextFill(Color.web("#FF9800"));
            } else {
                templateInfoLabel.setText("");
            }
        } else {
            String error = XmlUtil.getXmlError(xml);
            validationLabel.setText("✗ " + error);
            validationLabel.setTextFill(Color.web("#f44336"));
            templateInfoLabel.setText("");
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

    private void connectToMq() {
        new Thread(() -> {
            try {
                mqService.connect();
                Platform.runLater(() -> {
                    updateConnectionState(true);
                    showStatus("Connected to IBM MQ", "#4CAF50");
                });
            } catch (JMSException e) {
                log.error("Connection failed", e);
                Platform.runLater(() -> 
                    showAlert("Connection Error", "Failed to connect: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        }).start();
    }

    private void disconnectFromMq() {
        mqService.disconnect();
        updateConnectionState(false);
        showStatus("Disconnected from IBM MQ", "#f44336");
    }

    private void sendMessage() {
        String xml = xmlInputArea.getText();
        String queue = sendQueueCombo.getValue();
        int messageCount = messageCountSpinner.getValue();
        int threadCount = threadCountSpinner.getValue();

        if (!XmlUtil.isValidXml(xml)) {
            showAlert("Error", "Invalid XML content", Alert.AlertType.ERROR);
            return;
        }

        // Disable controls during sending
        sendButton.setDisable(true);
        messageCountSpinner.setDisable(true);
        threadCountSpinner.setDisable(true);
        sendProgressBar.setVisible(true);
        sendProgressBar.setProgress(0);
        progressLabel.setText(String.format("Sending 0/%d messages...", messageCount));
        
        new Thread(() -> {
            try {
                mqService.sendMessagesMultiThreaded(queue, xml, messageCount, threadCount, 
                    (sent, total) -> {
                        // Progress callback
                        Platform.runLater(() -> {
                            double progress = (double) sent / total;
                            sendProgressBar.setProgress(progress);
                            progressLabel.setText(String.format("Sending %d/%d messages...", sent, total));
                        });
                    });
                
                Platform.runLater(() -> {
                    showStatus(String.format("Successfully sent %d messages to %s using %d threads", 
                        messageCount, queue, threadCount), "#4CAF50");
                    updateHistoryList();
                    sendProgressBar.setVisible(false);
                    progressLabel.setText("");
                    sendButton.setDisable(!mqService.isConnected());
                    messageCountSpinner.setDisable(false);
                    threadCountSpinner.setDisable(false);
                });
            } catch (JMSException e) {
                log.error("Failed to send messages", e);
                Platform.runLater(() -> {
                    showAlert("Send Error", "Failed to send messages: " + e.getMessage(), Alert.AlertType.ERROR);
                    sendProgressBar.setVisible(false);
                    progressLabel.setText("");
                    sendButton.setDisable(!mqService.isConnected());
                    messageCountSpinner.setDisable(false);
                    threadCountSpinner.setDisable(false);
                });
            } catch (InterruptedException e) {
                log.error("Message sending interrupted", e);
                Thread.currentThread().interrupt();
                Platform.runLater(() -> {
                    showAlert("Send Error", "Message sending was interrupted", Alert.AlertType.ERROR);
                    sendProgressBar.setVisible(false);
                    progressLabel.setText("");
                    sendButton.setDisable(!mqService.isConnected());
                    messageCountSpinner.setDisable(false);
                    threadCountSpinner.setDisable(false);
                });
            }
        }).start();
    }

    private void receiveMessages() {
        String queue = receiveQueueCombo.getValue();
        
        new Thread(() -> {
            try {
                List<MqMessage> messages = mqService.receiveMessages(queue, 10);
                Platform.runLater(() -> {
                    if (messages.isEmpty()) {
                        receivedMessagesArea.appendText("\n[" + java.time.LocalDateTime.now().format(TIME_FORMATTER) + "] No messages available\n");
                    } else {
                        for (MqMessage msg : messages) {
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
        mqService.clearHistory();
        historyListView.getItems().clear();
        showStatus("History cleared", "#4CAF50");
    }

    private void updateHistoryList() {
        historyListView.getItems().clear();
        List<MqMessage> history = mqService.getMessageHistory();
        for (MqMessage msg : history) {
            String icon = msg.getType() == MqMessage.MessageType.SENT ? "→" : "←";
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
        sendButton.setDisable(!connected || !XmlUtil.isValidXml(xmlInputArea.getText()));
        receiveButton.setDisable(!connected);
        beautifyButton.setDisable(false);
        
        if (connected) {
            statusLabel.setText("Connected to " + mqConfig.getQueueManager());
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

    /**
     * Creates the Dynamic Configuration tab with MQ connection form and send/receive capabilities
     */
    private BorderPane createDynamicConfigurationTab() {
        BorderPane mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: #2b2b2b;");

        // Top Section: Connection Configuration Form
        VBox configSection = new VBox(15);
        configSection.setPadding(new Insets(20));
        configSection.setStyle("-fx-background-color: #2b2b2b;");

        Label titleLabel = new Label("Dynamic IBM MQ Configuration");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.web("#e0e0e0"));

        // Configuration Form
        GridPane configForm = new GridPane();
        configForm.setHgap(15);
        configForm.setVgap(10);
        configForm.setPadding(new Insets(15));
        configForm.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 5;");

        // Queue Manager
        Label qmLabel = new Label("Queue Manager:");
        qmLabel.setTextFill(Color.web("#b0b0b0"));
        TextField qmField = new TextField();
        qmField.setPromptText("e.g., QM1");
        qmField.setStyle("-fx-control-inner-background: #3e3e3e; -fx-text-fill: #e0e0e0;");
        qmField.setPrefWidth(300);

        // Channel
        Label channelLabel = new Label("Channel:");
        channelLabel.setTextFill(Color.web("#b0b0b0"));
        TextField channelField = new TextField();
        channelField.setPromptText("e.g., DEV.APP.SVRCONN");
        channelField.setStyle("-fx-control-inner-background: #3e3e3e; -fx-text-fill: #e0e0e0;");
        channelField.setPrefWidth(300);

        // Connection Name
        Label connLabel = new Label("Connection:");
        connLabel.setTextFill(Color.web("#b0b0b0"));
        TextField connField = new TextField();
        connField.setPromptText("e.g., localhost(1414)");
        connField.setStyle("-fx-control-inner-background: #3e3e3e; -fx-text-fill: #e0e0e0;");
        connField.setPrefWidth(300);

        // User
        Label userLabel = new Label("User:");
        userLabel.setTextFill(Color.web("#b0b0b0"));
        TextField userField = new TextField();
        userField.setPromptText("e.g., app");
        userField.setStyle("-fx-control-inner-background: #3e3e3e; -fx-text-fill: #e0e0e0;");
        userField.setPrefWidth(300);

        // Password
        Label passwordLabel = new Label("Password:");
        passwordLabel.setTextFill(Color.web("#b0b0b0"));
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setStyle("-fx-control-inner-background: #3e3e3e; -fx-text-fill: #e0e0e0;");
        passwordField.setPrefWidth(300);

        // Add to grid
        configForm.add(qmLabel, 0, 0);
        configForm.add(qmField, 1, 0);
        configForm.add(channelLabel, 0, 1);
        configForm.add(channelField, 1, 1);
        configForm.add(connLabel, 0, 2);
        configForm.add(connField, 1, 2);
        configForm.add(userLabel, 0, 3);
        configForm.add(userField, 1, 3);
        configForm.add(passwordLabel, 0, 4);
        configForm.add(passwordField, 1, 4);

        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        Button testButton = createStyledButton("Test Connection", "#2196F3");
        Button dynamicConnectButton = createStyledButton("Connect", "#4CAF50");
        Button dynamicDisconnectButton = createStyledButton("Disconnect", "#f44336");
        
        dynamicDisconnectButton.setDisable(true);

        // Status label for dynamic connection
        Label dynamicStatusLabel = new Label("Not connected");
        dynamicStatusLabel.setTextFill(Color.web("#f44336"));
        dynamicStatusLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        dynamicStatusLabel.setPadding(new Insets(0, 0, 0, 20));

        buttonBox.getChildren().addAll(testButton, dynamicConnectButton, dynamicDisconnectButton, dynamicStatusLabel);

        configSection.getChildren().addAll(titleLabel, configForm, buttonBox);

        // Center Section: Send/Receive Panel
        SplitPane messagingPanel = new SplitPane();
        messagingPanel.setStyle("-fx-background-color: #2b2b2b;");

        // Send Panel
        VBox sendPanel = createDynamicSendPanel();
        
        // Receive Panel
        VBox receivePanel = createDynamicReceivePanel();

        messagingPanel.getItems().addAll(sendPanel, receivePanel);
        messagingPanel.setDividerPositions(0.5);

        mainLayout.setTop(configSection);
        mainLayout.setCenter(messagingPanel);

        // Event Handlers
        testButton.setOnAction(e -> {
            String qm = qmField.getText().trim();
            String channel = channelField.getText().trim();
            String conn = connField.getText().trim();
            String user = userField.getText().trim();
            String password = passwordField.getText();

            if (qm.isEmpty() || channel.isEmpty() || conn.isEmpty()) {
                showAlert("Validation Error", "Please fill in Queue Manager, Channel, and Connection fields", Alert.AlertType.WARNING);
                return;
            }

            dynamicStatusLabel.setText("Testing connection...");
            dynamicStatusLabel.setTextFill(Color.web("#FF9800"));

            new Thread(() -> {
                boolean success = dynamicMqService.testConnection(qm, channel, conn, user, password);
                Platform.runLater(() -> {
                    if (success) {
                        dynamicStatusLabel.setText("✓ Connection test successful");
                        dynamicStatusLabel.setTextFill(Color.web("#4CAF50"));
                        showAlert("Success", "Connection test successful!", Alert.AlertType.INFORMATION);
                    } else {
                        dynamicStatusLabel.setText("✗ Connection test failed");
                        dynamicStatusLabel.setTextFill(Color.web("#f44336"));
                        showAlert("Error", "Connection test failed. Please check your configuration.", Alert.AlertType.ERROR);
                    }
                });
            }).start();
        });

        dynamicConnectButton.setOnAction(e -> {
            String qm = qmField.getText().trim();
            String channel = channelField.getText().trim();
            String conn = connField.getText().trim();
            String user = userField.getText().trim();
            String password = passwordField.getText();

            if (qm.isEmpty() || channel.isEmpty() || conn.isEmpty()) {
                showAlert("Validation Error", "Please fill in Queue Manager, Channel, and Connection fields", Alert.AlertType.WARNING);
                return;
            }

            new Thread(() -> {
                try {
                    dynamicMqService.connect(qm, channel, conn, user, password);
                    Platform.runLater(() -> {
                        dynamicStatusLabel.setText("Connected to " + qm);
                        dynamicStatusLabel.setTextFill(Color.web("#4CAF50"));
                        dynamicConnectButton.setDisable(true);
                        dynamicDisconnectButton.setDisable(false);
                        testButton.setDisable(true);
                        updateDynamicButtonStates(true);
                        showAlert("Success", "Connected to IBM MQ successfully!", Alert.AlertType.INFORMATION);
                    });
                } catch (JMSException ex) {
                    log.error("Connection failed", ex);
                    Platform.runLater(() -> {
                        dynamicStatusLabel.setText("Connection failed");
                        dynamicStatusLabel.setTextFill(Color.web("#f44336"));
                        showAlert("Connection Error", "Failed to connect: " + ex.getMessage(), Alert.AlertType.ERROR);
                    });
                }
            }).start();
        });

        dynamicDisconnectButton.setOnAction(e -> {
            dynamicMqService.disconnect();
            dynamicStatusLabel.setText("Disconnected");
            dynamicStatusLabel.setTextFill(Color.web("#f44336"));
            dynamicConnectButton.setDisable(false);
            dynamicDisconnectButton.setDisable(true);
            testButton.setDisable(false);
            updateDynamicButtonStates(false);
        });

        return mainLayout;
    }

    private VBox createDynamicSendPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: #2b2b2b;");

        Label titleLabel = new Label("Send Message");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.web("#e0e0e0"));

        // Queue name input
        HBox queueBox = new HBox(10);
        queueBox.setAlignment(Pos.CENTER_LEFT);
        Label queueLabel = new Label("Queue:");
        queueLabel.setTextFill(Color.web("#b0b0b0"));
        TextField dynamicQueueField = new TextField();
        dynamicQueueField.setPromptText("e.g., DEV.QUEUE.1");
        dynamicQueueField.setPrefWidth(200);
        dynamicQueueField.setStyle("-fx-background-color: #3e3e3e; -fx-text-fill: #e0e0e0;");
        queueBox.getChildren().addAll(queueLabel, dynamicQueueField);

        Label xmlLabel = new Label("XML Message Content:");
        xmlLabel.setTextFill(Color.web("#b0b0b0"));

        TextArea dynamicXmlInput = new TextArea();
        dynamicXmlInput.setPromptText("Enter XML content here...");
        dynamicXmlInput.setPrefRowCount(15);
        dynamicXmlInput.setWrapText(true);
        dynamicXmlInput.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: #e0e0e0; " +
                              "-fx-font-family: 'Courier New'; -fx-font-size: 12px;");

        Label dynamicValidationLabel = new Label("");
        dynamicValidationLabel.setFont(Font.font("System", 11));

        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        
        Button dynamicBeautifyButton = createStyledButton("Beautify XML", "#2196F3");
        dynamicSendButton = createStyledButton("Send Message", "#4CAF50");
        Button clearButton = createStyledButton("Clear", "#757575");
        
        dynamicSendButton.setDisable(true);
        
        buttonBox.getChildren().addAll(dynamicBeautifyButton, dynamicSendButton, clearButton);

        // Result area
        Label resultLabel = new Label("Result:");
        resultLabel.setTextFill(Color.web("#b0b0b0"));
        
        TextArea resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setPrefRowCount(5);
        resultArea.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: #e0e0e0;");

        VBox.setVgrow(dynamicXmlInput, Priority.ALWAYS);
        panel.getChildren().addAll(titleLabel, queueBox, xmlLabel, dynamicXmlInput, dynamicValidationLabel, buttonBox, resultLabel, resultArea);

        // Event handlers
        dynamicXmlInput.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                dynamicValidationLabel.setText("");
                dynamicSendButton.setDisable(true);
                return;
            }

            boolean isValid = XmlUtil.isValidXml(newVal);
            if (isValid) {
                dynamicValidationLabel.setText("✓ Valid XML");
                dynamicValidationLabel.setTextFill(Color.web("#4CAF50"));
                dynamicSendButton.setDisable(!dynamicMqService.isConnected());
            } else {
                String error = XmlUtil.getXmlError(newVal);
                dynamicValidationLabel.setText("✗ " + error);
                dynamicValidationLabel.setTextFill(Color.web("#f44336"));
                dynamicSendButton.setDisable(true);
            }
        });

        dynamicBeautifyButton.setOnAction(e -> {
            String xml = dynamicXmlInput.getText();
            if (xml == null || xml.trim().isEmpty()) {
                showAlert("Error", "Please enter XML content first", Alert.AlertType.WARNING);
                return;
            }

            try {
                String beautified = XmlUtil.beautifyXml(xml);
                dynamicXmlInput.setText(beautified);
                resultArea.setText("XML beautified successfully");
            } catch (Exception ex) {
                showAlert("Beautify Error", "Failed to beautify XML: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        dynamicSendButton.setOnAction(e -> {
            String xml = dynamicXmlInput.getText();
            String queue = dynamicQueueField.getText().trim();

            if (queue.isEmpty()) {
                showAlert("Error", "Please enter a queue name", Alert.AlertType.WARNING);
                return;
            }

            if (!XmlUtil.isValidXml(xml)) {
                showAlert("Error", "Invalid XML content", Alert.AlertType.ERROR);
                return;
            }

            new Thread(() -> {
                try {
                    dynamicMqService.sendMessage(queue, xml);
                    Platform.runLater(() -> {
                        resultArea.setText(String.format("✓ Message sent successfully to %s at %s", 
                            queue, LocalDateTime.now().format(TIME_FORMATTER)));
                    });
                } catch (JMSException ex) {
                    log.error("Failed to send message", ex);
                    Platform.runLater(() -> {
                        resultArea.setText("✗ Failed to send message: " + ex.getMessage());
                        showAlert("Send Error", "Failed to send message: " + ex.getMessage(), Alert.AlertType.ERROR);
                    });
                }
            }).start();
        });

        clearButton.setOnAction(e -> dynamicXmlInput.clear());

        return panel;
    }

    private VBox createDynamicReceivePanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: #2b2b2b;");

        Label titleLabel = new Label("Receive Messages");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.web("#e0e0e0"));

        // Queue name input
        HBox queueBox = new HBox(10);
        queueBox.setAlignment(Pos.CENTER_LEFT);
        Label queueLabel = new Label("Queue:");
        queueLabel.setTextFill(Color.web("#b0b0b0"));
        TextField dynamicReceiveQueueField = new TextField();
        dynamicReceiveQueueField.setPromptText("e.g., DEV.QUEUE.2");
        dynamicReceiveQueueField.setPrefWidth(200);
        dynamicReceiveQueueField.setStyle("-fx-background-color: #3e3e3e; -fx-text-fill: #e0e0e0;");
        queueBox.getChildren().addAll(queueLabel, dynamicReceiveQueueField);

        Label messagesLabel = new Label("Received Messages:");
        messagesLabel.setTextFill(Color.web("#b0b0b0"));

        TextArea dynamicReceivedArea = new TextArea();
        dynamicReceivedArea.setEditable(false);
        dynamicReceivedArea.setWrapText(true);
        dynamicReceivedArea.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: #e0e0e0; " +
                                     "-fx-font-family: 'Courier New'; -fx-font-size: 12px;");

        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        
        dynamicReceiveButton = createStyledButton("Receive Messages", "#FF9800");
        Button clearButton = createStyledButton("Clear", "#757575");
        
        dynamicReceiveButton.setDisable(true);
        
        buttonBox.getChildren().addAll(dynamicReceiveButton, clearButton);

        VBox.setVgrow(dynamicReceivedArea, Priority.ALWAYS);
        panel.getChildren().addAll(titleLabel, queueBox, messagesLabel, dynamicReceivedArea, buttonBox);

        // Event handlers
        dynamicReceiveButton.setOnAction(e -> {
            String queue = dynamicReceiveQueueField.getText().trim();

            if (queue.isEmpty()) {
                showAlert("Error", "Please enter a queue name", Alert.AlertType.WARNING);
                return;
            }

            new Thread(() -> {
                try {
                    List<MqMessage> messages = dynamicMqService.receiveMessages(queue, 10);
                    Platform.runLater(() -> {
                        if (messages.isEmpty()) {
                            dynamicReceivedArea.appendText("\n[No messages received]\n");
                        } else {
                            for (MqMessage msg : messages) {
                                dynamicReceivedArea.appendText(String.format(
                                    "\n========================================\n" +
                                    "Time: %s\n" +
                                    "Queue: %s\n" +
                                    "Message ID: %s\n" +
                                    "----------------------------------------\n" +
                                    "%s\n" +
                                    "========================================\n",
                                    msg.getTimestamp().format(TIME_FORMATTER),
                                    msg.getQueue(),
                                    msg.getMessageId(),
                                    msg.getContent()
                                ));
                            }
                        }
                    });
                } catch (JMSException ex) {
                    log.error("Failed to receive messages", ex);
                    Platform.runLater(() -> 
                        showAlert("Receive Error", "Failed to receive messages: " + ex.getMessage(), Alert.AlertType.ERROR));
                }
            }).start();
        });

        clearButton.setOnAction(e -> dynamicReceivedArea.clear());

        return panel;
    }

    /**
     * Updates the enabled/disabled state of dynamic tab buttons based on connection status
     */
    private void updateDynamicButtonStates(boolean connected) {
        if (dynamicSendButton != null) {
            dynamicSendButton.setDisable(!connected);
        }
        if (dynamicReceiveButton != null) {
            dynamicReceiveButton.setDisable(!connected);
        }
    }
}
