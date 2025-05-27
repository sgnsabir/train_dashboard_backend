// src/main/java/com/banenor/model/AlertHistory.java
package com.banenor.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;

/**
 * Represents a single alert event stored in the alert_history table.
 */
@Table("alert_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertHistory {

    /** Primary key */
    @Id
    @Column("id")
    private Long id;

    /** Short subject/title of the alert */
    @Column("subject")
    private String subject;

    /** Detailed alert message body */
    @Column("message")
    private String message;

    /** Severity level (e.g. INFO, WARN, ERROR) */
    @Column("severity")
    private String severity;

    /** Associated train/analysis ID */
    @Column("train_no")
    private Integer trainNo;

    /** When the alert was generated */
    @Column("timestamp")
    private LocalDateTime timestamp;

    /** Whether the alert has been acknowledged */
    @Column("acknowledged")
    private Boolean acknowledged;

    /** Who acknowledged (read) this alert, null if still unacknowledged */
    @Column("acknowledged_by")
    private String acknowledgedBy;
}
