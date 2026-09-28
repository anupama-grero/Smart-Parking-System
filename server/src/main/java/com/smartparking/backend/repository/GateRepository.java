package com.smartparking.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartparking.backend.model.Gate;
import com.smartparking.backend.model.GateType;

@Repository
public interface GateRepository extends JpaRepository<Gate, Long> {
    Optional<Gate> findByGateType(GateType gateType);
    Optional<Gate> findByGateName(String gateName);
}
