package com.banenor.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Standard API error response.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiError {
    private String errorCode;      // e.g., ERR-400, ERR-401, ERR-404, ERR-500, etc.
    private String message;
    private LocalDateTime timestamp;
    private String correlationId;
}
