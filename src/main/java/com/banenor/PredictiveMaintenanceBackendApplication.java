package com.banenor;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PredictiveMaintenanceBackendApplication {

    public static void main(String[] args) {
        // Set the default time zone to Europe/Oslo to handle daylight saving time dynamically.
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Oslo"));
        SpringApplication.run(PredictiveMaintenanceBackendApplication.class, args);
    }

    // Uncomment the following main method to generate a new JWT secret key if needed.
//     public static void main(String[] args) {
//         byte[] key = Keys.secretKeyFor(SignatureAlgorithm.HS512).getEncoded();
//         String base64Key = Base64.getEncoder().encodeToString(key);
//         System.out.println("JWT Secret Key (Base64): " + base64Key);
//     }
}
