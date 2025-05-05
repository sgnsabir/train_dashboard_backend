package com.banenor.repository;

import com.banenor.model.UserSettings;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSettingsRepository extends R2dbcRepository<UserSettings, Long> {
}
