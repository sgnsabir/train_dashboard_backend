package com.banenor.repository;

import com.banenor.model.DigitalTwin;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

/**
 * Primary repository for DigitalTwin state.
 * Inherits standard CRUD methods and our dynamic‐filter extension.
 */
public interface DigitalTwinRepository
        extends R2dbcRepository<DigitalTwin, Integer>,
        DigitalTwinRepositoryCustom {
}
