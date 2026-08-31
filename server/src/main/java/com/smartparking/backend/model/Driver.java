package com.smartparking.backend.model;

public class Driver {
    private String driverId;
    private String name;
    private String vehicleNumber;

    public Driver() {}

    public Driver(String driverId, String name, String vehicleNumber) {
        this.driverId = driverId;
        this.name = name;
        this.vehicleNumber = vehicleNumber;
    }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }
}