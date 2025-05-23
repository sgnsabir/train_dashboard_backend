package com.banenor.config;

import io.vertx.core.Vertx;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.StartTLSOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MailConfig {

    @Value("${mail.host}")
    private String host;

    @Value("${mail.port:1025}")
    private int port;

    @Value("${mail.username:}")
    private String username;

    @Value("${mail.password:}")
    private String password;

    @Value("${mail.ssl:false}")
    private boolean ssl;

    @Value("${mail.starttls.required:false}")
    private boolean starttlsRequired;

    @Bean
    public MailClient mailClient(Vertx vertx) {
        io.vertx.ext.mail.MailConfig cfg = new io.vertx.ext.mail.MailConfig()
                .setHostname(host)
                .setPort(port)
                .setSsl(ssl)
                .setStarttls(starttlsRequired ? StartTLSOptions.REQUIRED : StartTLSOptions.DISABLED)
                .setTrustAll(true);

        if (!username.isBlank() && !password.isBlank()) {
            log.warn("MailConfig: credentials provided but MailHog doesn’t require auth—ignoring username/password");
        }

        log.info("MailClient configured for MailHog @ {}:{}", host, port);
        return MailClient.createShared(vertx, cfg, "reactive-mail-client");
    }

    @Bean
    public Vertx vertx() {
        return Vertx.vertx();
    }
}
