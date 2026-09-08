package com.smartparking.backend.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "parking_slots")
public class ParkingSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer slotNumber;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private boolean isOccupied = false;

    @Column
    private Double lastDistanceCm;

    @Column
    private LocalDateTime lastSensorUpdate;

    // Required by JPA
    public ParkingSlot() {
    }

    // Used when creating a new parking slot
    public ParkingSlot(Integer slotNumber, String category, boolean isOccupied) {
        this.slotNumber = slotNumber;
        this.category = category;
        this.isOccupied = isOccupied;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(Integer slotNumber) {
        this.slotNumber = slotNumber;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    public void setOccupied(boolean occupied) {
        isOccupied = occupied;
    }

    public Double getLastDistanceCm() {
        return lastDistanceCm;
    }

    public void setLastDistanceCm(Double lastDistanceCm) {
        this.lastDistanceCm = lastDistanceCm;
    }

    public LocalDateTime getLastSensorUpdate() {
        return lastSensorUpdate;
    }

    public void setLastSensorUpdate(LocalDateTime lastSensorUpdate) {
        this.lastSensorUpdate = lastSensorUpdate;
    }
}