package com.smartparking.backend.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.smartparking.backend.model.ParkingSession;
import com.smartparking.backend.model.SessionStatus;

@Repository
public class ParkingSessionRepository {
    private final List<ParkingSession> sessionDatabase = new ArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    public ParkingSession save(ParkingSession session) {
        if (session.getId() == null) {
            session.setId(idCounter.getAndIncrement());
            sessionDatabase.add(session);
        }
        return session;
    }

    public Optional<ParkingSession> findById(Long id) {
        return sessionDatabase.stream()
                .filter(s -> s.getId().equals(id))
                .findFirst();
    }

    public List<ParkingSession> findByStatus(SessionStatus status) {
        return sessionDatabase.stream()
                .filter(s -> s.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<ParkingSession> findAll() {
        return new ArrayList<>(sessionDatabase);
    }
}