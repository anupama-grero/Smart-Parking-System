package com.smartparking.backend.model;

import java.time.LocalDateTime;

public class ParkingSession {
    private Long id;
    private Driver driver;
    private Integer assignedSlot;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private SessionStatus status;

    public ParkingSession() {}

    public ParkingSession(Long id, Driver driver, Integer assignedSlot, LocalDateTime entryTime, SessionStatus status) {
        this.id = id;
        this.driver = driver;
        this.assignedSlot = assignedSlot;
        this.entryTime = entryTime;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Driver getDriver() { return driver; }
    public void setDriver(Driver driver) { this.driver = driver; }

    public Integer getAssignedSlot() { return assignedSlot; }
    public void setAssignedSlot(Integer assignedSlot) { this.assignedSlot = assignedSlot; }

    public LocalDateTime getEntryTime() { return entryTime; }
    public void setEntryTime(LocalDateTime entryTime) { this.entryTime = entryTime; }

    public LocalDateTime getExitTime() { return exitTime; }
    public void setExitTime(LocalDateTime exitTime) { this.exitTime = exitTime; }

    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
}