package com.kafkamanager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.kafkamanager.ui.MainFrame;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;

@SpringBootApplication
public class KafkaManagerApplication {

    public static void main(String[] args) {
        // Set FlatLaf look and feel
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf theme");
        }

        // Start Spring Boot application in headless mode disabled
        ConfigurableApplicationContext context = new SpringApplicationBuilder(KafkaManagerApplication.class)
                .headless(false)
                .run(args);

        // Launch Swing UI on EDT
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = context.getBean(MainFrame.class);
            mainFrame.setVisible(true);
        });
    }
}
