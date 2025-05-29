package com.banenor.mapper;

import com.banenor.dto.RawDataResponse;
import com.banenor.model.AbstractAxles;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper to convert domain entities (extending AbstractAxles)
 * into RawDataResponse.
 */
@Mapper(componentModel = "spring")
public interface SensorMeasurementMapper {
    RawDataResponse toSensorMeasurementDTO(AbstractAxles axle);
}
