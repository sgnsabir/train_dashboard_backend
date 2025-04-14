package com.banenor.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AnalysisHeaderDTO {
    private Integer trainNo;
    private String mstation;
    private String mplace;
    private Double cooLat;
    private Double cooLong;
    private Integer trackKm;
    private Integer trackM;
    private String allTpsInfo;
    private LocalDateTime mstartTime;
    private LocalDateTime mstopTime;
    private String aversion;
    private String rversion;
    private LocalDateTime astartTime;
    private LocalDateTime astopTime;
    private String td;
    private String rfidDevs;
    private Double rTemp;
    private Double aTemp;
    private Double aPress;
    private Double aHum;
    private LocalDateTime createdAt;
}
