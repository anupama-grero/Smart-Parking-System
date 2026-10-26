package com.smartparking.backend.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.smartparking.backend.model.Gate;
import com.smartparking.backend.model.GateStatus;
import com.smartparking.backend.model.GateType;
import com.smartparking.backend.model.ParkingSlot;
import com.smartparking.backend.repository.GateRepository;
import com.smartparking.backend.repository.ParkingSlotRepository;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final ParkingSlotRepository parkingSlotRepository;
    private final GateRepository gateRepository;

    public DatabaseInitializer(ParkingSlotRepository parkingSlotRepository, GateRepository gateRepository) {
        this.parkingSlotRepository = parkingSlotRepository;
        this.gateRepository = gateRepository;
    }

    @Override
    public void run(String... args) {
        if (parkingSlotRepository.count() == 0) {
            parkingSlotRepository.saveAll(List.of(
                    new ParkingSlot(1, false),
                    new ParkingSlot(2, false),
                    new ParkingSlot(3, false),
                    new ParkingSlot(4, false)
            ));
        }

        if (gateRepository.count() == 0) {
            gateRepository.saveAll(List.of(
                    new Gate("Main Entrance Gate", GateType.ENTRANCE, GateStatus.CLOSED),
                    new Gate("Main Exit Gate", GateType.EXIT, GateStatus.CLOSED)
            ));
        }
    }
}
