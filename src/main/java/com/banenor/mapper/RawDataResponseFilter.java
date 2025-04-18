package com.banenor.mapper;

import com.banenor.dto.RawDataResponse;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RawDataResponseFilter {

    /**
     * If sensorType is blank, returns the full DTO.
     * Otherwise returns a DTO containing only analysisId, createdAt, sensorType and the matching value.
     * Supports any field name from RawDataResponse (e.g. "spdTp1", "vviblTp5", etc.).
     */
    public RawDataResponse filter(RawDataResponse dto, String sensorType) {
        if (!StringUtils.hasText(sensorType)) {
            return dto;
        }

        RawDataResponse filtered = new RawDataResponse();
        filtered.setAnalysisId(dto.getAnalysisId());
        filtered.setCreatedAt(dto.getCreatedAt());
        filtered.setSensorType(sensorType.trim());

        // dynamically read the matching property (field names match sensorType exactly)
        BeanWrapper wrapper = new BeanWrapperImpl(dto);
        Object raw = wrapper.getPropertyValue(sensorType.trim());
        if (raw instanceof Number) {
            filtered.setValue(((Number) raw).doubleValue());
        }

        return filtered;
    }
}
