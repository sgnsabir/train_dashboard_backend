// src/main/java/com/banenor/model/TrainHealth.java
package com.banenor.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("train_health")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainHealth {
    @Id
    private Integer id;

    @Column("train_no")
    private Integer trainNo;

    @Column("health_score")
    private Double healthScore;

    @Column("fault_count")
    private Integer faultCount;

    @Column("timestamp")
    private LocalDateTime timestamp;
}
