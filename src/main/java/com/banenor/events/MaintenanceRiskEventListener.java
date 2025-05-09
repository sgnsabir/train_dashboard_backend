package com.banenor.events;

import com.banenor.websocket.WebSocketBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for MaintenanceRiskEvent and forwards it over WebSocket to clients.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaintenanceRiskEventListener {

    private final WebSocketBroadcaster broadcaster;

    @EventListener
    public void onRisk(MaintenanceRiskEvent event) {
        log.debug("Received MaintenanceRiskEvent: {}", event);
        try {
            broadcaster.publish(event, "MAINTENANCE_DATA");
            log.debug("Successfully published MaintenanceRiskEvent to WebSocket");
        } catch (Exception ex) {
            log.error("Error publishing MaintenanceRiskEvent: {}", ex.getMessage(), ex);
        }
    }
}
