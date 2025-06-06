package com.banenor.mapper;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.banenor.dto.AnalysisHeaderDTO;
import com.banenor.dto.AnalysisMeasurementDTO;
import com.banenor.model.HaugfjellMP3Axles;
import com.banenor.model.HaugfjellMP3Header;

/**
 * Mapper for Haugfjell MP3 entities.
 * This mapper extends the common AbstractHaugfjellMapper to inherit shared mapping logic.
 * It explicitly maps LocalDateTime fields (mstartTime, mstopTime, astartTime, astopTime, createdAt)
 * from the header DTO to SQL Timestamp fields in the entity using the qualified conversion method.
 */
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface HaugfjellMP3Mapper extends AbstractHaugfjellMapper<HaugfjellMP3Header, HaugfjellMP3Axles> {

    @Override
    default Integer deriveSegmentId(HaugfjellMP3Header header) {
        return header != null ? header.getTrackKm() : null;
    }

    /**
     * Maps an AnalysisMeasurementDTO and its corresponding header into a HaugfjellMP3Axles entity.
     */
    @Override
    @Mapping(target = "header", source = "header")
    HaugfjellMP3Axles measurementDtoToEntity(AnalysisMeasurementDTO measurementDTO, HaugfjellMP3Header header);

    /**
     * Maps an AnalysisHeaderDTO into a HaugfjellMP3Header entity.
     * Explicitly converts LocalDateTime fields to SQL Timestamps using a qualified mapping.
     */
    @Mapping(target = "trainNo", ignore = true)
    @Mapping(target = "mstartTime", source = "mstartTime", qualifiedByName = "localDateTimeToTimestamp")
    @Mapping(target = "mstopTime", source = "mstopTime", qualifiedByName = "localDateTimeToTimestamp")
    @Mapping(target = "astartTime", source = "astartTime", qualifiedByName = "localDateTimeToTimestamp")
    @Mapping(target = "astopTime", source = "astopTime", qualifiedByName = "localDateTimeToTimestamp")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "localDateTimeToTimestamp")
    HaugfjellMP3Header headerDtoToEntity(AnalysisHeaderDTO headerDTO);

    /**
     * Converts a LocalDateTime to a SQL Timestamp.
     * This conversion method is used to map all LocalDateTime fields to Timestamp.
     *
     * @param localDateTime the LocalDateTime value.
     * @return the corresponding SQL Timestamp, or null if the input is null.
     */
    @Named("localDateTimeToTimestamp")
    @Override
    default Timestamp localDateTimeToTimestamp(LocalDateTime localDateTime) {
        return localDateTime == null ? null : Timestamp.valueOf(localDateTime);
    }
}
