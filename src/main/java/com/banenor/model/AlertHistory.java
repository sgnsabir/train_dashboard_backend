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

    /** Alert subject/title */
    @Column("subject")
    private String subject;

    /** Alert message body */
    @Column("text")
    private String text;

    /** When the alert was generated */
    @Column("timestamp")
    private LocalDateTime timestamp;

    /** Whether the alert has been acknowledged */
    @Column("acknowledged")
    private Boolean acknowledged;
}
