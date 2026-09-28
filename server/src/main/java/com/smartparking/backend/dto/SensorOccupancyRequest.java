package com.smartparking.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Incoming HC-SR04 reading from the ESP32.
 * Provide {@code slotId} and/or {@code sensorId} (1–8 map 1:1 to campus slots).
 */
public class SensorOccupancyRequest {

    @JsonAlias({"slot_id", "slotNumber", "slot_number"})
    private Integer slotId;

    @JsonAlias({"sensor_id"})
    private Integer sensorId;

    @NotNull(message = "distance is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "distance cannot be negative")
    @JsonAlias({"distance_cm", "distanceCm"})
    private Double distance;

    public SensorOccupancyRequest() {}

    public SensorOccupancyRequest(Integer slotId, Double distance) {
        this.slotId = slotId;
        this.distance = distance;
    }

    public SensorOccupancyRequest(Integer slotId, Integer sensorId, Double distance) {
        this.slotId = slotId;
        this.sensorId = sensorId;
        this.distance = distance;
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
}
