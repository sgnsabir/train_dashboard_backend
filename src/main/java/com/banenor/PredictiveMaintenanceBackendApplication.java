// src/main/java/com/banenor/PredictiveMaintenanceBackendApplication.java
package com.banenor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class PredictiveMaintenanceBackendApplication {

    public static void main(String[] args) {
        // Ensure consistent timezone handling in production
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Oslo"));
        SpringApplication.run(PredictiveMaintenanceBackendApplication.class, args);
    }
}
