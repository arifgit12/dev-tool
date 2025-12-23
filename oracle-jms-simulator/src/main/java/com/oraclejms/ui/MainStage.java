package com.oraclejms.ui;

import com.oraclejms.model.JmsMessage;
import com.oraclejms.model.OracleJmsConfig;
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

import jakarta.jms.JMSException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
public class MainStage {

    private final OracleJmsService jmsService;
    private final OracleJmsConfig jmsConfig;

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

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public MainStage(OracleJmsService jmsService, OracleJmsConfig jmsConfig) {
        this.jmsService = jmsService;
        this.jmsConfig = jmsConfig;
    }

    public void start(Stage primaryStage) {
        primaryStage.setTitle("Oracle JMS Simulator - Professional Edition");
        
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #2b2b2b;");

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
        addConnectionField(connectionGrid, row++, "JMS Provider URL:", jmsConfig.getProviderUrl());
        addConnectionField(connectionGrid, row++, "Connection Factory:", jmsConfig.getConnectionFactory());
        addConnectionField(connectionGrid, row++, "User:", jmsConfig.getUser());

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        
        connectButton = createStyledButton("Connect", "#4CAF50");
        disconnectButton = createStyledButton("Disconnect", "#f44336");
        
        connectButton.setOnAction(e -> connectToJms());
        disconnectButton.setOnAction(e -> disconnectFromJms());
        
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
        sendQueueCombo.getItems().addAll(jmsConfig.getQueue().getIn(), jmsConfig.getQueue().getOut());
        sendQueueCombo.setValue(jmsConfig.getQueue().getOut());
        sendQueueCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(sendQueueCombo, Priority.ALWAYS);
        sendQueueCombo.setStyle("-fx-background-color: #3e3e3e; -fx-text-fill: #e0e0e0;");
        queueBox.getChildren().addAll(queueLabel, sendQueueCombo);

        Label xmlLabel = new Label("XML Message Content:");
        xmlLabel.setTextFill(Color.web("#b0b0b0"));

        xmlInputArea = new TextArea();
        xmlInputArea.setPromptText("Enter XML content here...\n\nExample:\n<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<message>\n  <text>Hello Oracle JMS</text>\n</message>");
        xmlInputArea.setPrefRowCount(15);
        xmlInputArea.setWrapText(true);
        xmlInputArea.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: #e0e0e0; " +
                              "-fx-font-family: 'Courier New'; -fx-font-size: 12px;");
        xmlInputArea.textProperty().addListener((obs, oldVal, newVal) -> validateXml(newVal));

        validationLabel = new Label("");
        validationLabel.setFont(Font.font("System", 11));

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
            titleLabel, queueBox, xmlLabel, xmlInputArea, validationLabel,
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
            sendButton.setDisable(true);
            return;
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
                    showStatus("Connected to " + jmsConfig.getProviderUrl(), "#4CAF50");
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
        String queue = sendQueueCombo.getValue();

        if (!XmlUtil.isValidXml(xml)) {
            showAlert("Error", "Invalid XML content", Alert.AlertType.ERROR);
            return;
        }

        new Thread(() -> {
            try {
                jmsService.sendMessage(queue, xml);
                Platform.runLater(() -> {
                    showStatus("Message sent successfully to " + queue, "#4CAF50");
                    updateHistoryList();
                });
            } catch (JMSException e) {
                log.error("Failed to send message", e);
                Platform.runLater(() -> 
                    showAlert("Send Error", "Failed to send message: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        }).start();
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
            statusLabel.setText("Connected to " + jmsConfig.getProviderUrl());
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
