package com.banenor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("digital_twins")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DigitalTwin {

    @Id
    @Column("id")
    private Long id;

    @Column("asset_id")
    private Integer assetId;

    @Column("recorded_at")
    private LocalDateTime recordedAt;

    @Column("metric_value")
    private Double metricValue;

    @Column("metric_type")
    private String metricType;

    @Column("component_name")
    private String componentName;

    @Column("location")
    private String location;

    @Column("status")
    private String status;

    @Column("risk_score")
    private Double riskScore;
}
