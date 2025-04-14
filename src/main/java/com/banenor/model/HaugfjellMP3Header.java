package com.banenor.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.EqualsAndHashCode;
import org.springframework.data.relational.core.mapping.Table;

@Table("haugfjell_mp3_header")
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class HaugfjellMP3Header extends AbstractHeader {
    // Inherits all fields from AbstractHeader.
}
