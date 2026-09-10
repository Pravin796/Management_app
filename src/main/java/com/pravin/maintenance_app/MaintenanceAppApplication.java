package com.pravin.maintenance_app;

import com.pravin.maintenance_app.config.MaintenanceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MaintenanceProperties.class)
public class MaintenanceAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaintenanceAppApplication.class, args);
    }

}
