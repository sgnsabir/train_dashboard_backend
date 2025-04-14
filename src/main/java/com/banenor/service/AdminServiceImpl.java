package com.banenor.service;

import com.banenor.dto.AdminDashboardDTO;
import com.banenor.dto.AlertResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuple8;

import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminServiceImpl.class);
    private final CacheService cacheService;
    private final AlertService alertService;
    private final SystemHealthService systemHealthService;

    public AdminServiceImpl(CacheService cacheService, AlertService alertService, SystemHealthService systemHealthService) {
        this.cacheService = cacheService;
        this.alertService = alertService;
        this.systemHealthService = systemHealthService;
    }

    @Override
    public Mono<AdminDashboardDTO> getAdminDashboard() {
        // Group the first eight cached averages into a Tuple8.
        Mono<Tuple8<Double, Double, Double, Double, Double, Double, Double, Double>> group1 =
                Mono.zip(
                        cacheService.getCachedAverage("avgSpeed"),
                        cacheService.getCachedAverage("avgAoa"),
                        cacheService.getCachedAverage("avgVibration"),
                        cacheService.getCachedAverage("avgVerticalForceLeft"),
                        cacheService.getCachedAverage("avgVerticalForceRight"),
                        cacheService.getCachedAverage("avgLateralForceLeft"),
                        cacheService.getCachedAverage("avgLateralForceRight"),
                        cacheService.getCachedAverage("avgLateralVibrationLeft")
                );

        // Group the remaining three publishers into a Tuple3.
        Mono<Tuple3<Double, List<AlertResponse>, String>> group2 =
                Mono.zip(
                        cacheService.getCachedAverage("avgLateralVibrationRight"),
                        alertService.getAlertHistory().collectList(),
                        systemHealthService.getSystemStatus()
                );

        // Combine the two groups.
        return Mono.zip(group1, group2)
                .map(tuple -> {
                    Tuple8<Double, Double, Double, Double, Double, Double, Double, Double> t1 = tuple.getT1();
                    Tuple3<Double, List<AlertResponse>, String> t2 = tuple.getT2();

                    AdminDashboardDTO dashboard = new AdminDashboardDTO();
                    dashboard.setAverageSpeed(t1.getT1());
                    dashboard.setAverageAoa(t1.getT2());
                    dashboard.setAverageVibration(t1.getT3());
                    dashboard.setAverageVerticalForceLeft(t1.getT4());
                    dashboard.setAverageVerticalForceRight(t1.getT5());
                    dashboard.setAverageLateralForceLeft(t1.getT6());
                    dashboard.setAverageLateralForceRight(t1.getT7());
                    dashboard.setAverageLateralVibrationLeft(t1.getT8());
                    dashboard.setAverageLateralVibrationRight(t2.getT1());
                    dashboard.setAlertHistory(t2.getT2());
                    dashboard.setSystemStatus(t2.getT3());
                    return dashboard;
                })
                .doOnError(e -> logger.error("Error aggregating admin dashboard metrics", e));
    }
}
