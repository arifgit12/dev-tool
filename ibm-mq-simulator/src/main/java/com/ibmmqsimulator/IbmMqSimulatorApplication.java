package com.ibmmqsimulator;

import com.ibmmqsimulator.ui.MainStage;
import javafx.application.Application;
import javafx.application.Platform;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@EnableConfigurationProperties
public class IbmMqSimulatorApplication extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        context = new SpringApplicationBuilder(IbmMqSimulatorApplication.class)
                .headless(false)
                .run();
    }

    @Override
    public void start(javafx.stage.Stage primaryStage) {
        MainStage mainStage = context.getBean(MainStage.class);
        mainStage.start(primaryStage);
    }

    @Override
    public void stop() {
        context.close();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
