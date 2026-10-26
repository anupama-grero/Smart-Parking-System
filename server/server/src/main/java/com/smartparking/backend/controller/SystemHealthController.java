package com.smartparking.backend.controller;

import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartparking.backend.service.Esp32WifiService;
import com.smartparking.backend.service.SensorOccupancyService;

@RestController
public class SystemHealthController {

    private static final Logger log = LoggerFactory.getLogger(SystemHealthController.class);

    private final DataSource dataSource;
    private final Esp32WifiService esp32Service;
    private final SensorOccupancyService occupancyService;
    private final int staleAfterSeconds;

    public SystemHealthController(
            DataSource dataSource,
            Esp32WifiService esp32Service,
            SensorOccupancyService occupancyService,
            @Value("${parking.sensor.stale-after-seconds:10}") int staleAfterSeconds) {
        this.dataSource = dataSource;
        this.esp32Service = esp32Service;
        this.occupancyService = occupancyService;
        this.staleAfterSeconds = staleAfterSeconds;
    }

    @GetMapping("/api/system/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        boolean databaseConnected = isDatabaseConnected();
        boolean telemetryConnected = occupancyService.isTelemetryConnected(staleAfterSeconds);
        Instant lastUpdate = occupancyService.getLastSuccessfulBatchAt();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("backend", "UP");
        result.put("database", databaseConnected ? "UP" : "DOWN");
        result.put("esp32Telemetry", telemetryConnected ? "CONNECTED" : "STALE_OR_DISCONNECTED");
        result.put("esp32GateControl", esp32Service.isConnected() ? "CONNECTED"
                : esp32Service.isConfigured() ? "DISCONNECTED" : "NOT_CONFIGURED");
        result.put("lastSuccessfulSensorUpdate", lastUpdate);
        result.put("checkedAt", Instant.now());

        return ResponseEntity.status(databaseConnected ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(result);
    }

    private boolean isDatabaseConnected() {
        try (var connection = dataSource.getConnection()) {
            return connection.isValid(1);
        } catch (SQLException exception) {
            log.warn("Database health check failed: {}", exception.getMessage());
            return false;
        }
    }
}
