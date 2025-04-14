package com.banenor.repository;

import com.banenor.model.UserSettings;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSettingsRepository extends ReactiveCrudRepository<UserSettings, Long> {
}
