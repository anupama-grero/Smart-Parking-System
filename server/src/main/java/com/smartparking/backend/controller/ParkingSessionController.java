package com.smartparking.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartparking.backend.model.Driver;
import com.smartparking.backend.model.ParkingSession;
import com.smartparking.backend.service.ParkingSessionService;

@RestController
@RequestMapping("/api/parking-sessions")
@CrossOrigin(origins = "*")
public class ParkingSessionController {

    private final ParkingSessionService service;

    public ParkingSessionController(ParkingSessionService service) {
        this.service = service;
    }

    // Entry: POST /api/parking-sessions/entry?assignedSlot=1
    @PostMapping("/entry")
    public ParkingSession createSession(@RequestBody Driver driver, @RequestParam Integer assignedSlot) {
        return service.createSession(driver, assignedSlot);
    }

    // Exit: PUT /api/parking-sessions/exit/1
    @PutMapping("/exit/{sessionId}")
    public ParkingSession completeSession(@PathVariable Long sessionId) {
        return service.completeSession(sessionId);
    }

    // Get Active Sessions: GET /api/parking-sessions/active
    @GetMapping("/active")
    public List<ParkingSession> getActiveSessions() {
        return service.getActiveSessions();
    }

    // Get Parking History: GET /api/parking-sessions/history
    @GetMapping("/history")
    public List<ParkingSession> getParkingHistory() {
        return service.getParkingHistory();
    }
}