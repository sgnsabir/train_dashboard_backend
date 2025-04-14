package com.banenor.model;

import java.sql.Timestamp;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public abstract class AbstractHeader {

    @Id
    @Column("train_no")
    private Integer trainNo;

    @Column("mstation")
    private String mstation;

    @Column("mplace")
    private String mplace;

    @Column("coo_lat")
    private Double cooLat;

    @Column("coo_long")
    private Double cooLong;

    @Column("track_km")
    private Integer trackKm;

    @Column("track_m")
    private Integer trackM;

    @Column("all_tps_info")
    private String allTpsInfo;

    @Column("mstart_time")
    private Timestamp mstartTime;

    @Column("mstop_time")
    private Timestamp mstopTime;

    @Column("aversion")
    private String aversion;

    @Column("rversion")
    private String rversion;

    @Column("astart_time")
    private Timestamp astartTime;

    @Column("astop_time")
    private Timestamp astopTime;

    @Column("td")
    private String td;

    @Column("rfid_devs")
    private String rfidDevs;

    @Column("r_temp")
    private Double rTemp;

    @Column("a_temp")
    private Double aTemp;

    @Column("a_press")
    private Double aPress;

    @Column("a_hum")
    private Double aHum;

    @CreatedDate
    @Column("created_at")
    private Timestamp createdAt;
}
