package com.banenor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CameraPose {
    private double x;
    private double y;
    private double z;
    private double rx;
    private double ry;
    private double rz;
}
