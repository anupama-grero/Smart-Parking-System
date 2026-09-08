package com.smartparking.backend.dto;

import java.time.LocalDateTime;

public class SensorOccupancyResponse {

    private Integer slotId;
    private Integer sensorId;
    private Double distance;
    private boolean occupied;
    private String availability;
    private boolean heldByActiveSession;
    private LocalDateTime updatedAt;

    public SensorOccupancyResponse() {}

    public SensorOccupancyResponse(Integer slotId, Integer sensorId, Double distance,
                                   boolean occupied, String availability,
                                   boolean heldByActiveSession, LocalDateTime updatedAt) {
        this.slotId = slotId;
        this.sensorId = sensorId;
        this.distance = distance;
        this.occupied = occupied;
        this.availability = availability;
        this.heldByActiveSession = heldByActiveSession;
        this.updatedAt = updatedAt;
    }

    public Integer getSlotId() {
        return slotId;
    }

    public void setSlotId(Integer slotId) {
        this.slotId = slotId;
    }

    public Integer getSensorId() {
        return sensorId;
    }

    public void setSensorId(Integer sensorId) {
        this.sensorId = sensorId;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public boolean isHeldByActiveSession() {
        return heldByActiveSession;
    }

    public void setHeldByActiveSession(boolean heldByActiveSession) {
        this.heldByActiveSession = heldByActiveSession;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
