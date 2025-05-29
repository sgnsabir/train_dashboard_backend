package com.banenor.service;

import com.banenor.dto.RawDataResponse;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public enum SensorField {

    speed (RawDataResponse::getSpdTp1, RawDataResponse::getSpdTp2, RawDataResponse::getSpdTp3,
            RawDataResponse::getSpdTp5, RawDataResponse::getSpdTp6, RawDataResponse::getSpdTp8),

    aoa   (RawDataResponse::getAoaTp1, RawDataResponse::getAoaTp2, RawDataResponse::getAoaTp3,
            RawDataResponse::getAoaTp5, RawDataResponse::getAoaTp6, RawDataResponse::getAoaTp8),

    vfrcl (RawDataResponse::getVfrclTp1, RawDataResponse::getVfrclTp2, RawDataResponse::getVfrclTp3,
            RawDataResponse::getVfrclTp5, RawDataResponse::getVfrclTp6, RawDataResponse::getVfrclTp8),

    vfrcr (RawDataResponse::getVfrcrTp1, RawDataResponse::getVfrcrTp2, RawDataResponse::getVfrcrTp3,
            RawDataResponse::getVfrcrTp5, RawDataResponse::getVfrcrTp6, RawDataResponse::getVfrcrTp8),

    vvibl (RawDataResponse::getVviblTp1, RawDataResponse::getVviblTp2, RawDataResponse::getVviblTp3,
            RawDataResponse::getVviblTp5, RawDataResponse::getVviblTp6, RawDataResponse::getVviblTp8),

    vvibr (RawDataResponse::getVvibrTp1, RawDataResponse::getVvibrTp2, RawDataResponse::getVvibrTp3,
            RawDataResponse::getVvibrTp5, RawDataResponse::getVvibrTp6, RawDataResponse::getVvibrTp8),

    lfrcl (RawDataResponse::getLfrclTp1, RawDataResponse::getLfrclTp2, RawDataResponse::getLfrclTp3,
            RawDataResponse::getLfrclTp5, RawDataResponse::getLfrclTp6, dto -> null),

    lfrcr (RawDataResponse::getLfrcrTp1, RawDataResponse::getLfrcrTp2, RawDataResponse::getLfrcrTp3,
            RawDataResponse::getLfrcrTp5, RawDataResponse::getLfrcrTp6, dto -> null),

    lvibl (RawDataResponse::getLviblTp1, RawDataResponse::getLviblTp2, RawDataResponse::getLviblTp3,
            RawDataResponse::getLviblTp5, RawDataResponse::getLviblTp6, dto -> null),

    lvibr (RawDataResponse::getLvibrTp1, RawDataResponse::getLvibrTp2, RawDataResponse::getLvibrTp3,
            RawDataResponse::getLvibrTp5, RawDataResponse::getLvibrTp6, dto -> null),

    dtl (RawDataResponse::getDtlTp1, RawDataResponse::getDtlTp2, RawDataResponse::getDtlTp3,
            RawDataResponse::getDtlTp5, RawDataResponse::getDtlTp6, RawDataResponse::getDtlTp8, dto -> null),

    dtr (RawDataResponse::getDtrTp1, RawDataResponse::getDtrTp2, RawDataResponse::getDtrTp3,
            RawDataResponse::getDtrTp5, RawDataResponse::getDtrTp6, RawDataResponse::getDtrTp8, dto -> null),

    lngl  (RawDataResponse::getLnglTp1, dto -> null, dto -> null,
            dto -> null, dto -> null, RawDataResponse::getLnglTp8),

    lngr  (RawDataResponse::getLngrTp1, dto -> null, dto -> null,
            dto -> null, dto -> null, RawDataResponse::getLngrTp8);

    private final Function<RawDataResponse, Double>[] getters;

    @SafeVarargs
    SensorField(Function<RawDataResponse, Double>... getters) {
        this.getters = getters;
    }

    public List<Double> extract(RawDataResponse dto) {
        return Arrays.stream(getters)
                .map(fn -> fn.apply(dto))
                .toList();
    }

    public static SensorField fromCode(String code) {
        return Arrays.stream(values())
                .filter(f -> f.name().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Unknown sensor: " + code)
                );
    }
}
