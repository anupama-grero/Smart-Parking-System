package com.smartparking.backend.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.smartparking.backend.model.ParkingSlot;
import com.smartparking.backend.repository.ParkingSlotRepository;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final ParkingSlotRepository parkingSlotRepository;

    public DatabaseInitializer(ParkingSlotRepository parkingSlotRepository) {
        this.parkingSlotRepository = parkingSlotRepository;
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
    }
}
