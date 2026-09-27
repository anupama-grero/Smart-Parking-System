package com.smartparking.backend.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "drivers")
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String driverId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String vehicleNumber;

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("driver")
    private List<ParkingSession> sessions = new ArrayList<>();

    public Driver() {}

    public Driver(String driverId, String name, String vehicleNumber) {
        this.driverId = driverId;
        this.name = name;
        this.vehicleNumber = vehicleNumber;
    }

    public Driver(Long id, String driverId, String name, String vehicleNumber) {
        this.id = id;
        this.driverId = driverId;
        this.name = name;
        this.vehicleNumber = vehicleNumber;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public List<ParkingSession> getSessions() { return sessions; }
    public void setSessions(List<ParkingSession> sessions) { this.sessions = sessions; }
}