package com.banenor.mapper;

import com.banenor.dto.TrainHealthDTO;
import com.banenor.model.TrainHealth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Maps between TrainHealth entity and TrainHealthDTO.
 */
@Slf4j
@Component
public class TrainHealthMapper {

    public TrainHealthDTO toDto(TrainHealth e) {
        if (e == null) return null;
        return TrainHealthDTO.builder()
                .id(e.getId())
                .trainNo(e.getTrainNo())
                .healthScore(e.getHealthScore())
                .faultCount(e.getFaultCount())
                .timestamp(e.getTimestamp())
                .build();
    }

    public TrainHealth toEntity(TrainHealthDTO dto) {
        if (dto == null) return null;
        return TrainHealth.builder()
                .id(dto.getId())
                .trainNo(dto.getTrainNo())
                .healthScore(dto.getHealthScore())
                .faultCount(dto.getFaultCount())
                .timestamp(dto.getTimestamp())
                .build();
    }
}
