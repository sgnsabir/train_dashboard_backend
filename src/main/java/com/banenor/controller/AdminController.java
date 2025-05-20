// src/main/java/com/banenor/controller/AdminController.java
package com.banenor.controller;

import com.banenor.dto.AdminDashboardDTO;
import com.banenor.dto.AlertResponse;
import com.banenor.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Collections;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Admin-only endpoints")
@Slf4j
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get Admin Dashboard",
            description = "Aggregated sensor metrics, alert history, and system status."
    )
    public Mono<ResponseEntity<AdminDashboardDTO>> getAdminDashboard() {
        log.info("Admin dashboard requested");
        return adminService.getAdminDashboard()
                .map(dto -> {
                    log.info("Admin dashboard retrieved successfully");
                    return ResponseEntity.ok(dto);
                })
                .doOnError(e -> log.error("Error retrieving admin dashboard", e))
                // On error, return a well-formed DTO with defaults and 500 status
                .onErrorResume(e -> {
                    AdminDashboardDTO fallback = new AdminDashboardDTO();
                    fallback.setAverageSpeed(0.0);
                    fallback.setAverageAoa(0.0);
                    fallback.setAverageVibration(0.0);
                    fallback.setAverageVerticalForceLeft(0.0);
                    fallback.setAverageVerticalForceRight(0.0);
                    fallback.setAverageLateralForceLeft(0.0);
                    fallback.setAverageLateralForceRight(0.0);
                    fallback.setAverageLateralVibrationLeft(0.0);
                    fallback.setAverageLateralVibrationRight(0.0);
                    fallback.setAlertHistory(Collections.emptyList());
                    fallback.setSystemStatus("Unavailable");
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(fallback)
                    );
                });
    }
}
