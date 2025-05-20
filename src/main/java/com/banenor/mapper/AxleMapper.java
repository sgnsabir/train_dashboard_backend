// src/main/java/com/banenor/mapper/AxleMapper.java
package com.banenor.mapper;

import com.banenor.dto.RawDataResponse;
import com.banenor.model.AbstractAxles;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for converting an AbstractAxles entity into a RawDataResponse DTO.
 * Now includes header/meta fields as well as segmentId.
 */
@Mapper(componentModel = "spring")
public interface AxleMapper {

    @Mapping(target = "analysisId", source = "header.trainNo")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "segmentId", source = "segmentId")
    // Map header / meta fields
    @Mapping(target = "axleId", source = "axleId")
    @Mapping(target = "ait", source = "ait")
    @Mapping(target = "vty", source = "vty")
    @Mapping(target = "vit", source = "vit")
    @Mapping(target = "aiv", source = "aiv")
    @Mapping(target = "fe", source = "fe")
    @Mapping(target = "idRf2R", source = "idRf2R")
    // sensorType/value are left for dynamic filtering later
    @Mapping(target = "sensorType", ignore = true)
    @Mapping(target = "value", ignore = true)
    RawDataResponse toRawDataResponse(AbstractAxles axle);
}
