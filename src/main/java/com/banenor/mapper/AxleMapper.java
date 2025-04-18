package com.banenor.mapper;

import com.banenor.dto.RawDataResponse;
import com.banenor.model.AbstractAxles;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for converting an AbstractAxles entity into a RawDataResponse DTO.
 * All Tp‑fields are mapped automatically by name.
 */
@Mapper(componentModel = "spring")
public interface AxleMapper {

    @Mapping(target = "analysisId", source = "header.trainNo")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "sensorType", ignore = true)
    @Mapping(target = "value", ignore = true)
    RawDataResponse toRawDataResponse(AbstractAxles axle);
}
