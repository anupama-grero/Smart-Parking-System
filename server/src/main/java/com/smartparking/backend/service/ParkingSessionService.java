package com.smartparking.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartparking.backend.dto.CreateSessionRequest;
import com.smartparking.backend.exception.ResourceNotFoundException;
import com.smartparking.backend.model.Driver;
import com.smartparking.backend.model.ParkingSession;
import com.smartparking.backend.model.ParkingSlot;
import com.smartparking.backend.model.SessionStatus;
import com.smartparking.backend.repository.DriverRepository;
import com.smartparking.backend.repository.ParkingSessionRepository;
import com.smartparking.backend.repository.ParkingSlotRepository;

@Service
public class ParkingSessionService {

    private final ParkingSessionRepository sessionRepository;
    private final DriverRepository driverRepository;
    private final ParkingSlotRepository slotRepository;

    public ParkingSessionService(ParkingSessionRepository sessionRepository,
                                 DriverRepository driverRepository,
                                 ParkingSlotRepository slotRepository) {
        this.sessionRepository = sessionRepository;
        this.driverRepository = driverRepository;
        this.slotRepository = slotRepository;
    }

    /**
     * Entry Flow: Validates driver & slot, creates an active parking session, and occupies the slot.
     */
    @Transactional
    public ParkingSession createSession(CreateSessionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Session request payload cannot be null");
        }

        String driverId = request.getDriverId();
        if (driverId == null || driverId.trim().isEmpty()) {
            throw new IllegalArgumentException("Driver ID is required");
        }

        Integer slotNumber = request.getSlotNumber();
        if (slotNumber == null) {
            throw new IllegalArgumentException("Assigned parking slot number is required");
        }

        // 1. Validate or register Driver entity
        Driver driver = driverRepository.findByDriverId(driverId)
                .orElseGet(() -> {
                    String name = request.getName() != null ? request.getName() : "Driver " + driverId;
                    String vehicleNumber = request.getVehicleNumber() != null ? request.getVehicleNumber() : "UNKNOWN";
                    return driverRepository.save(new Driver(driverId, name, vehicleNumber));
                });

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            driver.setName(request.getName());
        }
        if (request.getVehicleNumber() != null && !request.getVehicleNumber().trim().isEmpty()) {
            driver.setVehicleNumber(request.getVehicleNumber());
        }
        driverRepository.save(driver);

        // 2. Business Rule: Driver must not have an active parking session
        sessionRepository.findByDriverAndStatus(driver, SessionStatus.ACTIVE)
                .ifPresent(existingSession -> {
                    throw new IllegalStateException("Driver " + driverId + " already has an active parking session");
                });

        // 3. Find and validate ParkingSlot entity
        ParkingSlot slot = slotRepository.findBySlotNumber(slotNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Parking slot not found: " + slotNumber));

        // 4. Business Rule: Slot must be AVAILABLE
        if (slot.isOccupied() || sessionRepository.findByParkingSlotAndStatus(slot, SessionStatus.ACTIVE).isPresent()) {
            throw new IllegalStateException("Parking slot " + slotNumber + " is currently occupied or assigned to an active session");
        }

        // 5. Update slot occupancy state
        slot.setOccupied(true);
        slotRepository.save(slot);

        // 6. Create and persist new ACTIVE ParkingSession
        ParkingSession session = new ParkingSession(
                driver,
                slot,
                LocalDateTime.now(),
                SessionStatus.ACTIVE
        );
        return sessionRepository.save(session);
    }

    /**
     * Alternative entry helper for backwards compatibility with Driver object + Integer slotNumber parameters.
     */
    @Transactional
    public ParkingSession createSession(Driver driver, Integer assignedSlot) {
        if (driver == null) {
            throw new IllegalArgumentException("Driver details are required");
        }
        CreateSessionRequest req = new CreateSessionRequest(
                driver.getDriverId(),
                driver.getName(),
                driver.getVehicleNumber(),
                assignedSlot
        );
        return createSession(req);
    }

    /**
     * Exit Flow: Validates session, marks exit time & COMPLETED status, and releases the parking slot.
     */
    @Transactional
    public ParkingSession completeSession(Long sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID cannot be null");
        }

        ParkingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Parking session not found: " + sessionId));

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new IllegalArgumentException("Parking session " + sessionId + " is already completed");
        }

        // Update session state
        session.setExitTime(LocalDateTime.now());
        session.setStatus(SessionStatus.COMPLETED);

        // Release parking slot
        ParkingSlot slot = session.getParkingSlot();
        if (slot != null) {
            slot.setOccupied(false);
            slotRepository.save(slot);
        }

        return sessionRepository.save(session);
    }

    /**
     * Retrieves session by ID.
     */
    @Transactional(readOnly = true)
    public ParkingSession getSessionById(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parking session not found: " + id));
    }

    /**
     * Active Sessions List
     */
    @Transactional(readOnly = true)
    public List<ParkingSession> getActiveSessions() {
        return sessionRepository.findByStatus(SessionStatus.ACTIVE);
    }

    /**
     * Parking History List
     */
    @Transactional(readOnly = true)
    public List<ParkingSession> getParkingHistory() {
        return sessionRepository.findAll();
    }
}