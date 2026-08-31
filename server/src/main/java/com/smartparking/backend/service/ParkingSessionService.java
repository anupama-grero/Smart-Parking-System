package com.smartparking.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smartparking.backend.model.Driver;
import com.smartparking.backend.model.ParkingSession;
import com.smartparking.backend.model.SessionStatus;
import com.smartparking.backend.repository.ParkingSessionRepository;

@Service
public class ParkingSessionService {

    private final ParkingSessionRepository repository;

    public ParkingSessionService(ParkingSessionRepository repository) {
        this.repository = repository;
    }

    // 1. Driver Arrival & Entry (Assign Slot & Set Status ACTIVE)
    public ParkingSession createSession(Driver driver, Integer assignedSlot) {
        ParkingSession session = new ParkingSession(
                null,
                driver,
                assignedSlot,
                LocalDateTime.now(),
                SessionStatus.ACTIVE
        );
        return repository.save(session);
    }

    // 2. Driver Exit (Set Exit Time & Update Status to COMPLETED)
    public ParkingSession completeSession(Long sessionId) {
        ParkingSession session = repository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Parking Session not found: " + sessionId));

        if (session.getStatus() == SessionStatus.ACTIVE) {
            session.setExitTime(LocalDateTime.now());
            session.setStatus(SessionStatus.COMPLETED);
        }
        return session;
    }

    // 3. Active Sessions List
    public List<ParkingSession> getActiveSessions() {
        return repository.findByStatus(SessionStatus.ACTIVE);
    }

    // 4. Parking History
    public List<ParkingSession> getParkingHistory() {
        return repository.findAll();
    }
}