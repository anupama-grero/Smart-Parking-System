package com.smartparking.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartparking.backend.dto.SensorOccupancyRequest;
import com.smartparking.backend.dto.SensorOccupancyResponse;
import com.smartparking.backend.service.SensorOccupancyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sensors")
public class SensorOccupancyController {

    private final SensorOccupancyService sensorOccupancyService;

    public SensorOccupancyController(SensorOccupancyService sensorOccupancyService) {
        this.sensorOccupancyService = sensorOccupancyService;
    }

    @PostMapping("/occupancy")
    public ResponseEntity<SensorOccupancyResponse> receiveOccupancy(
            @Valid @RequestBody SensorOccupancyRequest request) {
        return ResponseEntity.ok(sensorOccupancyService.processReading(request));
    }
}
