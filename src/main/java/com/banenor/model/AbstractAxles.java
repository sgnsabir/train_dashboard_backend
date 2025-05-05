package com.banenor.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for axle sensor measurements.
 * This class consolidates common sensor fields for both MP1 and MP3 axle data,
 * including speed, forces, vibrations, time delays, angle-of-attack, and longitudinal values.
 * Additionally, the {@code segmentId} field has been added to support track segmentation
 * for advanced analysis (such as hot-spot detection in specific track sections).
 */
@Data
@SuperBuilder
@NoArgsConstructor(force = true)
public abstract class AbstractAxles {

    @Transient
    private Map<String,Object> dynamicProperties = new HashMap<>();

    @Id
    @Column("axle_id")
    private Integer axleId;

    @Column("ait")
    private String ait;

    @Column("vty")
    private String vty;

    @Column("vit")
    private String vit;

    @Column("aiv")
    private String aiv;

    @Column("fe")
    private String fe;

    @Column("id_rf2_r")
    private String idRf2R;

    // --- Speed Measurements ---
    @Column("spd_tp1")
    private Double spdTp1;
    @Column("spd_tp2")
    private Double spdTp2;
    @Column("spd_tp3")
    private Double spdTp3;
    @Column("spd_tp5")
    private Double spdTp5;
    @Column("spd_tp6")
    private Double spdTp6;
    @Column("spd_tp8")
    private Double spdTp8;

    // --- Vertical Forces (Left) ---
    @Column("vfrcl_tp1")
    private Double vfrclTp1;
    @Column("vfrcl_tp2")
    private Double vfrclTp2;
    @Column("vfrcl_tp3")
    private Double vfrclTp3;
    @Column("vfrcl_tp5")
    private Double vfrclTp5;
    @Column("vfrcl_tp6")
    private Double vfrclTp6;
    @Column("vfrcl_tp8")
    private Double vfrclTp8;

    // --- Vertical Forces (Right) ---
    @Column("vfrcr_tp1")
    private Double vfrcrTp1;
    @Column("vfrcr_tp2")
    private Double vfrcrTp2;
    @Column("vfrcr_tp3")
    private Double vfrcrTp3;
    @Column("vfrcr_tp5")
    private Double vfrcrTp5;
    @Column("vfrcr_tp6")
    private Double vfrcrTp6;
    @Column("vfrcr_tp8")
    private Double vfrcrTp8;

    // --- Angle of Attack Measurements ---
    @Column("aoa_tp1")
    private Double aoaTp1;
    @Column("aoa_tp2")
    private Double aoaTp2;
    @Column("aoa_tp3")
    private Double aoaTp3;
    @Column("aoa_tp5")
    private Double aoaTp5;
    @Column("aoa_tp6")
    private Double aoaTp6;
    @Column("aoa_tp8")
    private Double aoaTp8;

    // --- Vertical Vibration (Left) ---
    @Column("vvibl_tp1")
    private Double vviblTp1;
    @Column("vvibl_tp2")
    private Double vviblTp2;
    @Column("vvibl_tp3")
    private Double vviblTp3;
    @Column("vvibl_tp5")
    private Double vviblTp5;
    @Column("vvibl_tp6")
    private Double vviblTp6;
    @Column("vvibl_tp8")
    private Double vviblTp8;

    // --- Vertical Vibration (Right) ---
    @Column("vvibr_tp1")
    private Double vvibrTp1;
    @Column("vvibr_tp2")
    private Double vvibrTp2;
    @Column("vvibr_tp3")
    private Double vvibrTp3;
    @Column("vvibr_tp5")
    private Double vvibrTp5;
    @Column("vvibr_tp6")
    private Double vvibrTp6;
    @Column("vvibr_tp8")
    private Double vvibrTp8;

    // --- Time Delay Measurements (Left) ---
    @Column("dtl_tp1")
    private Double dtlTp1;
    @Column("dtl_tp2")
    private Double dtlTp2;
    @Column("dtl_tp3")
    private Double dtlTp3;
    @Column("dtl_tp5")
    private Double dtlTp5;
    @Column("dtl_tp6")
    private Double dtlTp6;
    @Column("dtl_tp8")
    private Double dtlTp8;

    // --- Time Delay Measurements (Right) ---
    @Column("dtr_tp1")
    private Double dtrTp1;
    @Column("dtr_tp2")
    private Double dtrTp2;
    @Column("dtr_tp3")
    private Double dtrTp3;
    @Column("dtr_tp5")
    private Double dtrTp5;
    @Column("dtr_tp6")
    private Double dtrTp6;
    @Column("dtr_tp8")
    private Double dtrTp8;

    // --- Lateral Forces (Left) ---
    @Column("lfrcl_tp1")
    private Double lfrclTp1;
    @Column("lfrcl_tp2")
    private Double lfrclTp2;
    @Column("lfrcl_tp3")
    private Double lfrclTp3;
    @Column("lfrcl_tp5")
    private Double lfrclTp5;
    @Column("lfrcl_tp6")
    private Double lfrclTp6;

    // --- Lateral Forces (Right) ---
    @Column("lfrcr_tp1")
    private Double lfrcrTp1;
    @Column("lfrcr_tp2")
    private Double lfrcrTp2;
    @Column("lfrcr_tp3")
    private Double lfrcrTp3;
    @Column("lfrcr_tp5")
    private Double lfrcrTp5;
    @Column("lfrcr_tp6")
    private Double lfrcrTp6;

    // --- Lateral Vibration (Left) ---
    @Column("lvibl_tp1")
    private Double lviblTp1;
    @Column("lvibl_tp2")
    private Double lviblTp2;
    @Column("lvibl_tp3")
    private Double lviblTp3;
    @Column("lvibl_tp5")
    private Double lviblTp5;
    @Column("lvibl_tp6")
    private Double lviblTp6;

    // --- Lateral Vibration (Right) ---
    @Column("lvibr_tp1")
    private Double lvibrTp1;
    @Column("lvibr_tp2")
    private Double lvibrTp2;
    @Column("lvibr_tp3")
    private Double lvibrTp3;
    @Column("lvibr_tp5")
    private Double lvibrTp5;
    @Column("lvibr_tp6")
    private Double lvibrTp6;

    // --- Longitudinal Measurements ---
    @Column("lngl_tp1")
    private Double lnglTp1;
    @Column("lngl_tp8")
    private Double lnglTp8;
    @Column("lngr_tp1")
    private Double lngrTp1;
    @Column("lngr_tp8")
    private Double lngrTp8;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    /**
     * The segment ID used for grouping sensor data by track section.
     * This column enables segment-based queries to detect repeated anomalies (hot spots).
     */
    @Column("segment_id")
    private Integer segmentId;

    /**
     * Retrieve the associated header record.
     * Each concrete axle entity (e.g., HaugfjellMP1Axles, HaugfjellMP3Axles)
     * must implement this method to return its corresponding header.
     *
     * @return the AbstractHeader linked to this axle measurement.
     */
    public abstract AbstractHeader getHeader();
}
