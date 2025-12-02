package com.ldapmanager;

import com.ldapmanager.service.ConfigurationService;
import com.ldapmanager.service.LdapService;
import com.ldapmanager.ui.MainFrame;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.swing.*;

@SpringBootApplication
public class LdapManagerApplication {

    public static void main(String[] args) {
        // Set system properties for better UI on different platforms
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", "LDAP Manager");

        // Use system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SpringApplication.run(LdapManagerApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ConfigurationService configService,
                                                LdapService ldapService,
                                                com.ldapmanager.service.EmbeddedLdapServer embeddedLdapServer) {
        return args -> {
            // Create and display the main frame
            MainFrame mainFrame = new MainFrame(configService, ldapService, embeddedLdapServer);
            mainFrame.display();
        };
    }
}
