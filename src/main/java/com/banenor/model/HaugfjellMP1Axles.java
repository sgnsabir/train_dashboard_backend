package com.banenor.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("haugfjell_mp1_axles")
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Data
public class HaugfjellMP1Axles extends AbstractAxles {

    // Persist the train number (foreign key from header)
    @Column("train_no")
    private Integer trainNo;

    // The header is used only in memory (transient) for mapping purposes.
    @Transient
    private HaugfjellMP1Header header;

    /**
     * Sets the header and updates the trainNo field accordingly.
     *
     * @param header the HaugfjellMP1Header entity associated with this axle.
     */
    public void setHeader(HaugfjellMP1Header header) {
        this.header = header;
        if (header != null) {
            this.trainNo = header.getTrainNo();
        }
    }
}
