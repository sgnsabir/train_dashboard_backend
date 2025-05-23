package com.banenor.repository;

import com.banenor.model.VerificationToken;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface VerificationTokenRepository
        extends ReactiveCrudRepository<VerificationToken, Long> {
    Mono<VerificationToken> findByToken(String token);
    Mono<Void> deleteByUserId(Long userId);
}
