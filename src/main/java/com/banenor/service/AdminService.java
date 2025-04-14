package com.banenor.service;

import com.banenor.dto.AdminDashboardDTO;
import reactor.core.publisher.Mono;

public interface AdminService {
    Mono<AdminDashboardDTO> getAdminDashboard();
}
