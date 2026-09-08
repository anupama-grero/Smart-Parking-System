package com.smartparking.backend.dto;

public class CreateSessionRequest {
    private String driverId;
    private String name;
    private String vehicleNumber;
    private Integer slotNumber;
    private Integer assignedSlot;

    public CreateSessionRequest() {}

    public CreateSessionRequest(String driverId, String name, String vehicleNumber, Integer slotNumber) {
        this.driverId = driverId;
        this.name = name;
        this.vehicleNumber = vehicleNumber;
        this.slotNumber = slotNumber;
        this.assignedSlot = slotNumber;
    }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public Integer getSlotNumber() {
        return slotNumber != null ? slotNumber : assignedSlot;
    }
    public void setSlotNumber(Integer slotNumber) { this.slotNumber = slotNumber; }

    public Integer getAssignedSlot() {
        return assignedSlot != null ? assignedSlot : slotNumber;
    }
    public void setAssignedSlot(Integer assignedSlot) { this.assignedSlot = assignedSlot; }
}
