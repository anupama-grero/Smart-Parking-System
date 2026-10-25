package com.smartparking.backend.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "gates")
public class Gate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String gateName;

    @Column(nullable = false)
    private GateStatus status = GateStatus.CLOSED;

    @Column(nullable = false)
    private GateType gateType = GateType.ENTRANCE;

    @Column
    private LocalDateTime updatedAt;

    public Gate(String gateName, GateType gateType, GateStatus status) {
        this.gateName = gateName;
        this.gateType = gateType;
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    // Required by JPA
    public Gate() {
    }

    public Gate(String gateName, GateStatus status) {
        this.gateName = gateName;
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public Gate(Long id, String gateName, GateStatus status, LocalDateTime updatedAt) {
        this.id = id;
        this.gateName = gateName;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGateName() {
        return gateName;
    }

    public void setGateName(String gateName) {
        this.gateName = gateName;
    }

    public GateType getGateType() {
        return gateType;
    }

    public void setGateType(GateType gateType) {
        this.gateType = gateType;
    }

    public GateStatus getStatus() {
        return status;
    }

    public void setStatus(GateStatus status) {
        this.status = status;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
