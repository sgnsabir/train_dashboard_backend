package com.banenor.service;

import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final MailClient mailClient;

    @Value("${mail.from:no-reply@yourdomain.local}")
    private String fromAddress;

    @Value("${verification.base-url}")
    private String verificationBaseUrl;

    @Value("${verification.token.ttl.minutes:600}")
    private int tokenTtlMinutes;

    @Override
    public String buildVerificationLink(String token) {
        return verificationBaseUrl + "?token=" + token;
    }

    @Override
    public Mono<Void> sendVerificationEmail(String toEmail, String verificationLink) {
        MailMessage msg = new MailMessage()
                .setFrom(fromAddress)
                .setTo(toEmail)
                .setSubject("🔒 Verify Your Email Address")
                .setHtml(buildHtml(verificationLink));

        return Mono.<Void>create(sink ->
                mailClient.sendMail(msg, ar -> {
                    if (ar.succeeded()) {
                        log.info("Verification email sent to {}", toEmail);
                        sink.success();
                    } else {
                        log.error("Failed to send verification email to {}", toEmail, ar.cause());
                        sink.error(ar.cause());
                    }
                })
        );
    }

    private String buildHtml(String link) {
        return """
            <html>
              <body>
                <p>Welcome!</p>
                <p>Please verify your email by clicking the link below within %d minutes:</p>
                <p><a href="%s">Verify Email</a></p>
                <hr/>
                <p>If you did not register, you can safely ignore this message.</p>
              </body>
            </html>
            """.formatted(tokenTtlMinutes, link);
    }
}
