package com.smartparking.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartparking.backend.dto.CreateSessionRequest;
import com.smartparking.backend.model.Driver;
import com.smartparking.backend.model.ParkingSession;
import com.smartparking.backend.service.ParkingSessionService;

@RestController
@RequestMapping("/api/parking-sessions")
public class ParkingSessionController {

    private final ParkingSessionService service;

    public ParkingSessionController(ParkingSessionService service) {
        this.service = service;
    }

    // Entry: POST /api/parking-sessions/entry
    @PostMapping("/entry")
    public ResponseEntity<ParkingSession> createSession(
            @RequestBody(required = false) CreateSessionRequest requestBody,
            @RequestBody(required = false) Driver driverBody,
            @RequestParam(required = false) Integer assignedSlot,
            @RequestParam(required = false) Integer slotNumber) {

        CreateSessionRequest req;
        if (requestBody != null && (requestBody.getDriverId() != null || requestBody.getSlotNumber() != null)) {
            req = requestBody;
        } else if (driverBody != null) {
            Integer targetSlot = assignedSlot != null ? assignedSlot : slotNumber;
            req = new CreateSessionRequest(
                    driverBody.getDriverId(),
                    driverBody.getName(),
                    driverBody.getVehicleNumber(),
                    targetSlot
            );
        } else {
            throw new IllegalArgumentException("Driver details and slot number are required");
        }

        ParkingSession createdSession = service.createSession(req);
        return new ResponseEntity<>(createdSession, HttpStatus.CREATED);
    }

    // Exit: PUT /api/parking-sessions/exit/{sessionId}
    @PutMapping("/exit/{sessionId}")
    public ResponseEntity<ParkingSession> completeSession(@PathVariable Long sessionId) {
        ParkingSession session = service.completeSession(sessionId);
        return ResponseEntity.ok(session);
    }

    // Get Session By ID: GET /api/parking-sessions/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ParkingSession> getSessionById(@PathVariable Long id) {
        ParkingSession session = service.getSessionById(id);
        return ResponseEntity.ok(session);
    }

    // Get Active Sessions: GET /api/parking-sessions/active
    @GetMapping("/active")
    public ResponseEntity<List<ParkingSession>> getActiveSessions() {
        return ResponseEntity.ok(service.getActiveSessions());
    }

    // Get Parking History: GET /api/parking-sessions/history
    @GetMapping("/history")
    public ResponseEntity<List<ParkingSession>> getParkingHistory() {
        return ResponseEntity.ok(service.getParkingHistory());
    }
}