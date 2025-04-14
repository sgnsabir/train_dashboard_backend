package com.banenor.controller;

import com.banenor.dto.AdminDashboardDTO;
import com.banenor.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Admin-only endpoints")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get Admin Dashboard",
            description = "Returns administrative dashboard data including aggregated sensor metrics, alert history, and system status."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dashboard data retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/dashboard")
    public Mono<ResponseEntity<AdminDashboardDTO>> getAdminDashboard() {
        logger.info("Admin dashboard requested");
        return adminService.getAdminDashboard()
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> logger.info("Admin dashboard retrieved successfully"))
                .doOnError(error -> logger.error("Error retrieving admin dashboard", error))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
