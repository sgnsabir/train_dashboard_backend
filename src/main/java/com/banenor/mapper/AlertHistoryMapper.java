package com.banenor.mapper;

import com.banenor.dto.AlertHistoryDTO;
import com.banenor.model.AlertHistory;
import org.springframework.stereotype.Component;

@Component
public class AlertHistoryMapper {

    public AlertHistoryDTO toDto(AlertHistory entity) {
        AlertHistoryDTO dto = new AlertHistoryDTO();
        dto.setId(entity.getId());
        dto.setSubject(entity.getSubject());
        dto.setMessage(entity.getMessage());
        dto.setTimestamp(entity.getTimestamp());
        dto.setAcknowledged(entity.getAcknowledged());
        return dto;
    }

    public AlertHistory toEntity(AlertHistoryDTO dto) {
        return AlertHistory.builder()
                .id(dto.getId())
                .subject(dto.getSubject())
                .message(dto.getMessage())
                .timestamp(dto.getTimestamp())
                .acknowledged(dto.getAcknowledged())
                .build();
    }
}
