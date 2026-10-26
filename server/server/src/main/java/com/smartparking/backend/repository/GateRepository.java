package com.smartparking.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartparking.backend.model.Gate;
import com.smartparking.backend.model.GateType;
import com.smartparking.backend.model.GateStatus;

@Repository
public interface GateRepository extends JpaRepository<Gate, Long> {
    Optional<Gate> findByGateName(String gateName);
    Optional<Gate> findByGateType(GateType gateType);
}
