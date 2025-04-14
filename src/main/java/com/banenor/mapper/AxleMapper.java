package com.banenor.mapper;

import com.banenor.dto.RawDataResponse;
import com.banenor.model.AbstractAxles;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import java.time.LocalDateTime;

/**
 * Mapper for converting an AbstractAxles entity into a RawDataResponse DTO.
 */
@Mapper(componentModel = "spring")
public interface AxleMapper {

    @Mapping(target = "analysisId", source = "header.trainNo")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "spdTp1", target = "spdTp1")
    @Mapping(source = "vfrclTp1", target = "vfrclTp1")
    @Mapping(source = "vfrcrTp1", target = "vfrcrTp1")
    @Mapping(source = "aoaTp1", target = "aoaTp1")
    @Mapping(source = "vviblTp1", target = "vviblTp1")
    @Mapping(source = "vvibrTp1", target = "vvibrTp1")
    @Mapping(source = "dtlTp1", target = "dtlTp1")
    @Mapping(source = "dtrTp1", target = "dtrTp1")
    @Mapping(source = "lfrclTp1", target = "lfrclTp1")
    @Mapping(source = "lfrcrTp1", target = "lfrcrTp1")
    @Mapping(source = "lviblTp1", target = "lviblTp1")
    @Mapping(source = "lvibrTp1", target = "lvibrTp1")
    @Mapping(source = "lnglTp1", target = "lnglTp1")
    @Mapping(source = "lngrTp1", target = "lngrTp1")
    // Ignore additional fields used for filtering.
    @Mapping(target = "sensorType", ignore = true)
    @Mapping(target = "value", ignore = true)
    RawDataResponse toRawDataResponse(AbstractAxles axle);

    @Named("timestampToLocalDateTime")
    default LocalDateTime timestampToLocalDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
