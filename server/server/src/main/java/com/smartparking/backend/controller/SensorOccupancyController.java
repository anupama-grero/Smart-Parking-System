package com.smartparking.backend.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartparking.backend.dto.BatchOccupancyRequest;
import com.smartparking.backend.dto.SensorOccupancyRequest;
import com.smartparking.backend.dto.SensorOccupancyResponse;
import com.smartparking.backend.service.SensorOccupancyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sensors")
public class SensorOccupancyController {

    private static final Logger log = LoggerFactory.getLogger(SensorOccupancyController.class);

    private final SensorOccupancyService sensorOccupancyService;

    public SensorOccupancyController(SensorOccupancyService sensorOccupancyService) {
        this.sensorOccupancyService = sensorOccupancyService;
    }

    /**
     * Single HC-SR04 reading, used by the serial bridge and manual testing.
     *
     * POST /api/sensors/occupancy
     * Body: {"slotId": 1, "distance": 12.5}
     */
    @PostMapping("/occupancy")
    public ResponseEntity<SensorOccupancyResponse> receiveOccupancy(
            @Valid @RequestBody SensorOccupancyRequest request) {
        return ResponseEntity.ok(sensorOccupancyService.processReading(request));
    }

    /**
     * Batch occupancy payload posted by the ESP32 firmware.
     *
     * The active firmware posts to this route with {@code slotNumber} and
     * {@code distanceCm}; {@code slot} and {@code distance} remain accepted
     * aliases for existing clients.
     *
     * <p>Unreadable slots ({@code distance = -1} or {@code null}) preserve
     * their last state. Invalid or duplicate slots reject the batch.
     *
     * <p>Body includes all four slots with {@code slotNumber}, {@code occupied},
     * and {@code distanceCm}.
     *
     * @return one entry per processed reading, in payload order.
     */
    @PostMapping("/occupancy/batch")
    public ResponseEntity<List<SensorOccupancyResponse>> receiveBatchOccupancy(
            @RequestBody BatchOccupancyRequest request) {
        int readingCount = request == null || request.getSlots() == null ? 0 : request.getSlots().size();
        log.info("Received ESP32 occupancy batch with {} slot readings", readingCount);
        try {
            List<SensorOccupancyResponse> readings = sensorOccupancyService.processBatchReading(request);
            log.info("Accepted ESP32 occupancy batch: {} valid readings persisted", readings.size());
            return ResponseEntity.ok(readings);
        } catch (IllegalArgumentException exception) {
            log.warn("Rejected ESP32 occupancy batch: {}", exception.getMessage());
            throw exception;
        }
    }
}
