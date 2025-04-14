package com.banenor.mapper;

import com.banenor.dto.AnalysisHeaderDTO;
import com.banenor.model.AbstractHeader;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for converting an AbstractHeader entity into an AnalysisHeaderDTO.
 */
@Mapper(componentModel = "spring")
public interface HeaderMapper {

    // Map the header fields including the createdAt field.
    @Mapping(source = "mstartTime", target = "mstartTime")
    @Mapping(source = "mstopTime", target = "mstopTime")
    @Mapping(source = "astartTime", target = "astartTime")
    @Mapping(source = "astopTime", target = "astopTime")
    @Mapping(source = "createdAt", target = "createdAt")
    AnalysisHeaderDTO toAnalysisHeaderDTO(AbstractHeader header);
}
