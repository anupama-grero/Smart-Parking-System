package com.smartparking.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartparking.backend.model.Driver;
import com.smartparking.backend.model.ParkingSession;
import com.smartparking.backend.model.ParkingSlot;
import com.smartparking.backend.model.SessionStatus;

@Repository
public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {
    List<ParkingSession> findByStatus(SessionStatus status);
    Optional<ParkingSession> findByDriverAndStatus(Driver driver, SessionStatus status);
    Optional<ParkingSession> findByParkingSlotAndStatus(ParkingSlot parkingSlot, SessionStatus status);
}