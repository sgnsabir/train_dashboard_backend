package com.banenor.alert;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Recover;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service("notificationService")
public class NotificationService {

    private final JavaMailSender mailSender;
    private final MeterRegistry meterRegistry;

    public NotificationService(JavaMailSender mailSender, MeterRegistry meterRegistry) {
        this.mailSender = mailSender;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Sends an alert email reactively.
     *
     * @param to      recipient email
     * @param subject email subject
     * @param body    email body
     * @return Mono signaling completion
     */
    public Mono<Object> sendAlert(String to, String subject, String body) {
        return Mono.fromRunnable(() -> {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(to);
                    message.setSubject(subject);
                    message.setText(body);
                    mailSender.send(message);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Recover
    public Mono<Void> recoverSendAlert(Exception e, String to, String subject, String body) {
        System.err.println("Failed to send alert email to " + to + " after retries. Subject: " + subject + ". Error: " + e.getMessage());
        meterRegistry.counter("notification.send.failures").increment();
        return Mono.empty();
    }
}
