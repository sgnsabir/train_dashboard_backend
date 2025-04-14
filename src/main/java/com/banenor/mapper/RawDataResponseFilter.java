package com.banenor.mapper;

import com.banenor.dto.RawDataResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RawDataResponseFilter {

    public RawDataResponse filter(RawDataResponse dto, String sensorType) {
        if (!StringUtils.hasText(sensorType)) {
            return dto;
        }
        RawDataResponse filtered = new RawDataResponse();
        filtered.setAnalysisId(dto.getAnalysisId());
        switch (sensorType.trim().toLowerCase()) {
            case "speed":
                filtered.setSpdTp1(dto.getSpdTp1());
                break;
            case "verticalforceleft":
                filtered.setVfrclTp1(dto.getVfrclTp1());
                break;
            case "verticalforceright":
                filtered.setVfrcrTp1(dto.getVfrcrTp1());
                break;
            case "aoa":
                filtered.setAoaTp1(dto.getAoaTp1());
                break;
            case "vibrationleft":
                filtered.setVviblTp1(dto.getVviblTp1());
                break;
            case "vibrationright":
                filtered.setVvibrTp1(dto.getVvibrTp1());
                break;
            case "lateralforceleft":
                filtered.setLfrclTp1(dto.getLfrclTp1());
                break;
            case "lateralforceright":
                filtered.setLfrcrTp1(dto.getLfrcrTp1());
                break;
            case "lateralvibrationleft":
                filtered.setLviblTp1(dto.getLviblTp1());
                break;
            case "lateralvibrationright":
                filtered.setLvibrTp1(dto.getLvibrTp1());
                break;
            case "longitudinalleft":
                filtered.setLnglTp1(dto.getLnglTp1());
                break;
            case "longitudinalright":
                filtered.setLngrTp1(dto.getLngrTp1());
                break;
            default:
                return dto;
        }
        return filtered;
    }
}
