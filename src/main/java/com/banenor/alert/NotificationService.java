package com.banenor.alert;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.regex.Pattern;

@Service("notificationService")
public class NotificationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
            Pattern.CASE_INSENSITIVE
    );

    private final MailSender mailSender;
    private final MeterRegistry meterRegistry;

    public NotificationService(MailSender mailSender, MeterRegistry meterRegistry) {
        this.mailSender = mailSender;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Sends an alert email reactively, retrying up to 2 times on failure
     * (for a total of 3 attempts), and increments a success counter on success.
     *
     * @param to      recipient email
     * @param subject email subject
     * @param body    email body
     * @return Mono signaling completion or error after retries
     */
    public Mono<Void> sendAlert(String to, String subject, String body) {
        // basic email validation
        if (to == null || !EMAIL_PATTERN.matcher(to).matches()) {
            return Mono.error(new IllegalArgumentException("sendAlert: invalid email address"));
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        return Mono.<Void>fromRunnable(() -> mailSender.send(message))
                .subscribeOn(Schedulers.boundedElastic())
                // retry twice on any exception (total 3 attempts)
                .retry(2)
                // on final success
                .doOnSuccess(ignored -> meterRegistry.counter("notification.send.success").increment());
    }

    /**
     * Recovery method invoked manually after retries are exhausted.
     * Increments a failure counter.
     *
     * @param e       the exception that caused the failure
     * @param to      recipient email
     * @param subject email subject
     * @param body    email body
     * @return empty Mono signaling recovery complete
     */
    public Mono<Void> recoverSendAlert(Throwable e, String to, String subject, String body) {
        // log to stderr for visibility
        System.err.println("Failed to send alert email to " + to +
                " after retries. Subject: " + subject + ". Error: " + e.getMessage());
        meterRegistry.counter("notification.send.failures").increment();
        return Mono.empty();
    }
}
