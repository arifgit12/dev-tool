package com.ldapmanager;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LdapManagerApplication {

    public static void main(String[] args) {
        // Launch JavaFX application
        Application.launch(JavaFxApplication.class, args);
    }
}
