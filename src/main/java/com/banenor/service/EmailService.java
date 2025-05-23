package com.banenor.service;

import reactor.core.publisher.Mono;

public interface EmailService {
    Mono<Void> sendVerificationEmail(String toEmail, String verificationLink);
    String buildVerificationLink(String token);
}
