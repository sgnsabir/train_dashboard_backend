package com.banenor.repository;

import com.banenor.model.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends R2dbcRepository<User, Long> {
    // Use a case-insensitive query to match the username regardless of letter case.
    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username)")
    Mono<User> findByUsername(String username);

    Mono<User> findByEmail(String email);
}
