package com.banenor.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Provides frontend navigation entries aligned with Next.js pages.
 */
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
     * GET /api/v1/navigation
     * Returns the list of navigation items matching the Next.js pages/routes.
     */
    @GetMapping(value = "/api/v1/navigation", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<NavigationItem> getNavigationItems() {
        log.info("GET /api/v1/navigation → fetching navigation items");
        try {
            var items = Flux.just(
                    new NavigationItem("1", "Dashboard", "/"),
                    new NavigationItem("2", "Performance", "/performance"),
                    new NavigationItem("3", "Maintenance", "/maintenance"),
                    new NavigationItem("4", "Analytics", "/analytics"),
                    new NavigationItem("5", "Health", "/health"),
                    new NavigationItem("6", "Settings", "/settings"),
                    new NavigationItem("7", "Users", "/admin/users")
            );
            log.debug("Returning {} navigation items", 7);
            return items;
        } catch (Exception ex) {
            log.error("Failed to fetch navigation items", ex);
            return Flux.error(new RuntimeException("Navigation fetch failed"));
        }
    }
}
