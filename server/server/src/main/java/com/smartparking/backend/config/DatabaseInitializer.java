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
                    new ParkingSlot(1, "Undergraduates", false),
                    new ParkingSlot(2, "Short Courses Students", false),
                    new ParkingSlot(3, "Visiting Lecturers", false),
                    new ParkingSlot(4, "Visitors", false),
                    new ParkingSlot(5, "Lecturers", false),
                    new ParkingSlot(6, "Short Course Teachers", false),
                    new ParkingSlot(7, "Academic Staff", false),
                    new ParkingSlot(8, "Non-Academic Staff", false)
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
