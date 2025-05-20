package com.banenor.mapper;

import org.mapstruct.*;
import com.banenor.dto.AnalysisHeaderDTO;
import com.banenor.dto.AnalysisMeasurementDTO;

/**
 * Base mapper for Haugfjell MP1/MP3.  Now includes speed fields.
 */
@MapperConfig(
        componentModel        = "spring",
        unmappedTargetPolicy  = ReportingPolicy.IGNORE
)
public interface AbstractHaugfjellMapper<H, A> {

    @Mappings({
            // Header → entity
            @Mapping(target = "trainNo", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "mstartTime", source = "mstartTime", qualifiedByName = "localDateTimeToTimestamp"),
            @Mapping(target = "mstopTime",  source = "mstopTime",  qualifiedByName = "localDateTimeToTimestamp"),
            @Mapping(target = "astartTime", source = "astartTime", qualifiedByName = "localDateTimeToTimestamp"),
            @Mapping(target = "astopTime",  source = "astopTime",  qualifiedByName = "localDateTimeToTimestamp"),
            @Mapping(target = "rTemp", source = "RTemp"),
            @Mapping(target = "aTemp", source = "ATemp"),
            @Mapping(target = "aPress", source = "APress"),
            @Mapping(target = "aHum", source = "AHum")
    })
    H headerDtoToEntity(AnalysisHeaderDTO headerDTO);

    @Mappings({
            // link back to header/train
            @Mapping(target = "axleId",    source = "measurementDTO.axleId"),
            @Mapping(target = "segmentId", expression = "java(deriveSegmentId(header))"),

            // --- SPEED fields (were missing before!) ---
            @Mapping(target = "spdTp1", source = "measurementDTO.spdTp1"),
            @Mapping(target = "spdTp2", source = "measurementDTO.spdTp2"),
            @Mapping(target = "spdTp3", source = "measurementDTO.spdTp3"),
            @Mapping(target = "spdTp5", source = "measurementDTO.spdTp5"),
            @Mapping(target = "spdTp6", source = "measurementDTO.spdTp6"),
            @Mapping(target = "spdTp8", source = "measurementDTO.spdTp8"),

            // --- ANGLE OF ATTACK ---
            @Mapping(target = "aoaTp1", source = "measurementDTO.aoaTp1"),
            @Mapping(target = "aoaTp2", source = "measurementDTO.aoaTp2"),
            @Mapping(target = "aoaTp3", source = "measurementDTO.aoaTp3"),
            @Mapping(target = "aoaTp5", source = "measurementDTO.aoaTp5"),
            @Mapping(target = "aoaTp6", source = "measurementDTO.aoaTp6"),
            @Mapping(target = "aoaTp8", source = "measurementDTO.aoaTp8"),

            // --- TIME DELAY LEFT/RIGHT ---
            @Mapping(target = "dtlTp1", source = "measurementDTO.dtlTp1"),
            @Mapping(target = "dtlTp2", source = "measurementDTO.dtlTp2"),
            @Mapping(target = "dtlTp3", source = "measurementDTO.dtlTp3"),
            @Mapping(target = "dtlTp5", source = "measurementDTO.dtlTp5"),
            @Mapping(target = "dtlTp6", source = "measurementDTO.dtlTp6"),
            @Mapping(target = "dtlTp8", source = "measurementDTO.dtlTp8"),

            @Mapping(target = "dtrTp1", source = "measurementDTO.dtrTp1"),
            @Mapping(target = "dtrTp2", source = "measurementDTO.dtrTp2"),
            @Mapping(target = "dtrTp3", source = "measurementDTO.dtrTp3"),
            @Mapping(target = "dtrTp5", source = "measurementDTO.dtrTp5"),
            @Mapping(target = "dtrTp6", source = "measurementDTO.dtrTp6"),
            @Mapping(target = "dtrTp8", source = "measurementDTO.dtrTp8"),

            // --- VERTICAL FORCES LEFT/RIGHT ---
            @Mapping(target = "vfrclTp1", source = "measurementDTO.vfrclTp1"),
            @Mapping(target = "vfrclTp2", source = "measurementDTO.vfrclTp2"),
            @Mapping(target = "vfrclTp3", source = "measurementDTO.vfrclTp3"),
            @Mapping(target = "vfrclTp5", source = "measurementDTO.vfrclTp5"),
            @Mapping(target = "vfrclTp6", source = "measurementDTO.vfrclTp6"),
            @Mapping(target = "vfrclTp8", source = "measurementDTO.vfrclTp8"),

            @Mapping(target = "vfrcrTp1", source = "measurementDTO.vfrcrTp1"),
            @Mapping(target = "vfrcrTp2", source = "measurementDTO.vfrcrTp2"),
            @Mapping(target = "vfrcrTp3", source = "measurementDTO.vfrcrTp3"),
            @Mapping(target = "vfrcrTp5", source = "measurementDTO.vfrcrTp5"),
            @Mapping(target = "vfrcrTp6", source = "measurementDTO.vfrcrTp6"),
            @Mapping(target = "vfrcrTp8", source = "measurementDTO.vfrcrTp8"),

            // --- VIBRATION LEFT/RIGHT ---
            @Mapping(target = "vviblTp1", source = "measurementDTO.vviblTp1"),
            @Mapping(target = "vviblTp2", source = "measurementDTO.vviblTp2"),
            @Mapping(target = "vviblTp3", source = "measurementDTO.vviblTp3"),
            @Mapping(target = "vviblTp5", source = "measurementDTO.vviblTp5"),
            @Mapping(target = "vviblTp6", source = "measurementDTO.vviblTp6"),
            @Mapping(target = "vviblTp8", source = "measurementDTO.vviblTp8"),

            @Mapping(target = "vvibrTp1", source = "measurementDTO.vvibrTp1"),
            @Mapping(target = "vvibrTp2", source = "measurementDTO.vvibrTp2"),
            @Mapping(target = "vvibrTp3", source = "measurementDTO.vvibrTp3"),
            @Mapping(target = "vvibrTp5", source = "measurementDTO.vvibrTp5"),
            @Mapping(target = "vvibrTp6", source = "measurementDTO.vvibrTp6"),
            @Mapping(target = "vvibrTp8", source = "measurementDTO.vvibrTp8"),

            // --- LATERAL FORCES LEFT/RIGHT ---
            @Mapping(target = "lfrclTp1", source = "measurementDTO.lfrclTp1"),
            @Mapping(target = "lfrclTp2", source = "measurementDTO.lfrclTp2"),
            @Mapping(target = "lfrclTp3", source = "measurementDTO.lfrclTp3"),
            @Mapping(target = "lfrclTp5", source = "measurementDTO.lfrclTp5"),
            @Mapping(target = "lfrclTp6", source = "measurementDTO.lfrclTp6"),

            @Mapping(target = "lfrcrTp1", source = "measurementDTO.lfrcrTp1"),
            @Mapping(target = "lfrcrTp2", source = "measurementDTO.lfrcrTp2"),
            @Mapping(target = "lfrcrTp3", source = "measurementDTO.lfrcrTp3"),
            @Mapping(target = "lfrcrTp5", source = "measurementDTO.lfrcrTp5"),
            @Mapping(target = "lfrcrTp6", source = "measurementDTO.lfrcrTp6"),

            // --- LATERAL VIBRATION LEFT/RIGHT ---
            @Mapping(target = "lviblTp1", source = "measurementDTO.lviblTp1"),
            @Mapping(target = "lviblTp2", source = "measurementDTO.lviblTp2"),
            @Mapping(target = "lviblTp3", source = "measurementDTO.lviblTp3"),
            @Mapping(target = "lviblTp5", source = "measurementDTO.lviblTp5"),
            @Mapping(target = "lviblTp6", source = "measurementDTO.lviblTp6"),

            @Mapping(target = "lvibrTp1", source = "measurementDTO.lvibrTp1"),
            @Mapping(target = "lvibrTp2", source = "measurementDTO.lvibrTp2"),
            @Mapping(target = "lvibrTp3", source = "measurementDTO.lvibrTp3"),
            @Mapping(target = "lvibrTp5", source = "measurementDTO.lvibrTp5"),
            @Mapping(target = "lvibrTp6", source = "measurementDTO.lvibrTp6"),

            // --- LONGITUDINAL ---
            @Mapping(target = "lnglTp1", source = "measurementDTO.lnglTp1"),
            @Mapping(target = "lnglTp8", source = "measurementDTO.lnglTp8"),
            @Mapping(target = "lngrTp1", source = "measurementDTO.lngrTp1"),
            @Mapping(target = "lngrTp8", source = "measurementDTO.lngrTp8")
    })
    A measurementDtoToEntity(AnalysisMeasurementDTO measurementDTO, H header);

    @Named("localDateTimeToTimestamp")
    default java.sql.Timestamp localDateTimeToTimestamp(java.time.LocalDateTime dt) {
        return dt == null ? null : java.sql.Timestamp.valueOf(dt);
    }

    Integer deriveSegmentId(H header);
}
