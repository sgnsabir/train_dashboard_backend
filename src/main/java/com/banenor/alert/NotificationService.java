package com.banenor.alert;

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.regex.Pattern;

@Slf4j
@Service("notificationService")
@RequiredArgsConstructor
public class NotificationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
            Pattern.CASE_INSENSITIVE
    );

    private final MailClient mailClient;
    private final MeterRegistry meterRegistry;

    @Value("${mail.username}")
    private String fromAddress;

    public Mono<Void> sendAlert(String to, String subject, String body) {
        if (to == null || !EMAIL_PATTERN.matcher(to).matches()) {
            return Mono.error(new IllegalArgumentException("Invalid email address: " + to));
        }

        MailMessage msg = new MailMessage()
                .setFrom(fromAddress)
                .setTo(to)
                .setSubject(subject)
                .setText(body);

        return Mono.<Void>create(sink ->
                        mailClient.sendMail(msg, ar -> {
                            if (ar.succeeded()) {
                                sink.success();
                            } else {
                                sink.error(ar.cause());
                            }
                        })
                )
                .doOnSubscribe(__ -> log.debug("Attempting to send alert to {}", to))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(5))
                        .filter(ex -> ex instanceof Exception)
                        .doBeforeRetry(rs -> log.warn(
                                "Retry #{} for {} due to {}",
                                rs.totalRetries() + 1,
                                to,
                                rs.failure().getMessage()
                        ))
                )
                .doOnSuccess(__ -> {
                    meterRegistry.counter("notification.send.success").increment();
                    log.info("Successfully sent alert to {}", to);
                })
                .doOnError(e -> {
                    meterRegistry.counter("notification.send.failures").increment();
                    log.error("Failed to send alert to {} after retries: {}", to, e.getMessage(), e);
                })
                .onErrorResume(e -> Mono.empty());
    }
}
