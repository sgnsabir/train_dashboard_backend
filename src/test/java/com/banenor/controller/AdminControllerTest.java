package com.banenor.controller;

import com.banenor.config.TestSecurityConfig;  // <--- your simple "permitAll" config
import com.banenor.dto.AdminDashboardDTO;
import com.banenor.service.AdminService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = AdminController.class)
@Import({AdminControllerTest.TestConfig.class, TestSecurityConfig.class})
class AdminControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private AdminService adminService;

    /**
     * Provide the AdminService mock as a normal Spring bean
     * so that AdminController can autowire it.
     */
    @Configuration
    static class TestConfig {
        @Bean
        AdminService adminService() {
            return Mockito.mock(AdminService.class);
        }
    }

    @Test
    void testGetAdminDashboardSuccess() {
        // Setup mock behavior
        AdminDashboardDTO fakeDashboard = new AdminDashboardDTO();
        fakeDashboard.setSystemStatus("SYSTEM OK");
        Mockito.when(adminService.getAdminDashboard())
                .thenReturn(Mono.just(fakeDashboard));

        // Perform GET request
        webTestClient.get()
                .uri("/api/v1/admin/dashboard")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                // Expect a 200 OK
                .expectStatus().isOk()
                // Optionally check JSON body
                .expectBody(AdminDashboardDTO.class)
                .value(dto -> {
                    // Validate some fields
                    Assertions.assertThat(dto.getSystemStatus())
                            .isEqualTo("SYSTEM OK");
                });
    }

    @Test
    void testGetAdminDashboardError() {
        // Simulate an error from the service
        Mockito.when(adminService.getAdminDashboard())
                .thenReturn(Mono.error(new RuntimeException("Simulated failure")));

        // Perform request expecting 5xx
        webTestClient.get()
                .uri("/api/v1/admin/dashboard")
                .exchange()
                .expectStatus().is5xxServerError();
    }
}
