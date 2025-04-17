package com.banenor.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
public class NavigationController {

    @Data
    @AllArgsConstructor
    public static class NavigationItem {
        private String id;
        private String label;
        private String url;
    }

    /**
     * Canonical endpoint: GET /api/v1/navigation
     */
    @GetMapping(value = "/api/v1/navigation", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<NavigationItem> getNavigationItems() {
        log.info("Fetching navigation items");
        List<NavigationItem> items = Arrays.asList(
                new NavigationItem("1", "Dashboard", "/"),
                new NavigationItem("2", "Train Performance", "/dashboard/performance"),
                new NavigationItem("3", "Track & Infrastructure Health", "/dashboard/track"),
                new NavigationItem("4", "Load & Weight Distribution", "/dashboard/load"),
                new NavigationItem("5", "Wheel Condition", "/dashboard/wheel"),
                new NavigationItem("6", "Train Tracking & Safety", "/dashboard/tracking"),
                new NavigationItem("7", "Predictive Maintenance", "/dashboard/maintenance"),
                new NavigationItem("8", "Alerts & Anomalies", "/dashboard/alerts/alerts"),
                new NavigationItem("9", "Settings & Configurations", "/settings"),
                new NavigationItem("10", "Historical Data", "/dashboard/historical"),
                new NavigationItem("11", "Dynamic Visualizations", "/dashboard/dynamic-visuals"),
                new NavigationItem("12", "Sensor Aggregations", "/dashboard/sensor-aggregations"),
                new NavigationItem("13", "Segment Analysis", "/dashboard/segment")
        );
        return Flux.fromIterable(items);
    }
}
