package com.banenor.repository;

import com.banenor.model.DigitalTwin;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DigitalTwinRepository extends R2dbcRepository<DigitalTwin, Integer> {
    // No additional methods required; default CRUD operations are sufficient.
}
