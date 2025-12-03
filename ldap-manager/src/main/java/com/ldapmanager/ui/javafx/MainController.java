package com.ldapmanager.ui.javafx;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import org.springframework.stereotype.Component;

@Component
public class MainController {

    @FXML
    private TabPane mainTabPane;

    @FXML
    private Label statusLabel;

    @FXML
    public void initialize() {
        // Initialize main controller
        updateStatus("Ready");
    }

    public void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
}
