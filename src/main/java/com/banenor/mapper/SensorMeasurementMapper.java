package com.banenor.mapper;

import com.banenor.dto.SensorMeasurementDTO;
import com.banenor.model.AbstractAxles;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper to convert domain entities (extending AbstractAxles)
 * into SensorMeasurementDTO.
 */
@Mapper(componentModel = "spring")
public interface SensorMeasurementMapper {
    SensorMeasurementDTO toSensorMeasurementDTO(AbstractAxles axle);
}
