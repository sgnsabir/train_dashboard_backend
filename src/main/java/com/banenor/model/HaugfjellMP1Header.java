package com.banenor.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

@Table("haugfjell_mp1_header")
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Data
public class HaugfjellMP1Header extends AbstractHeader {
    // Inherits all fields from AbstractHeader.
}
