package com.smartparking.backend.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartparking.backend.exception.ResourceNotFoundException;
import com.smartparking.backend.model.ParkingSlot;
import com.smartparking.backend.repository.ParkingSlotRepository;

@ExtendWith(MockitoExtension.class)
class ParkingSlotServiceTest {

    @Mock
    private ParkingSlotRepository parkingSlotRepository;

    private ParkingSlotService parkingSlotService;
    private ParkingSlot slot2;

    @BeforeEach
    void setUp() {
        parkingSlotService = new ParkingSlotService(parkingSlotRepository);
        slot2 = new ParkingSlot(2L, 2, "Reverse Parking", false);
    }

    @Test
    void setReverseWarningActivatesSlot2() {
        when(parkingSlotRepository.findBySlotNumber(2)).thenReturn(Optional.of(slot2));
        when(parkingSlotRepository.save(slot2)).thenAnswer(invocation -> invocation.getArgument(0));

        parkingSlotService.setReverseWarning(2, true);

        assertTrue(slot2.isReverseWarning());
        verify(parkingSlotRepository, times(1)).save(slot2);
    }

    @Test
    void setReverseWarningDeactivatesSlot2() {
        slot2.setReverseWarning(true);
        when(parkingSlotRepository.findBySlotNumber(2)).thenReturn(Optional.of(slot2));
        when(parkingSlotRepository.save(slot2)).thenAnswer(invocation -> invocation.getArgument(0));

        parkingSlotService.setReverseWarning(2, false);

        assertFalse(slot2.isReverseWarning());
        verify(parkingSlotRepository, times(1)).save(slot2);
    }

    @Test
    void setReverseWarningThrowsWhenSlotMissing() {
        when(parkingSlotRepository.findBySlotNumber(99)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> parkingSlotService.setReverseWarning(99, true));

        verify(parkingSlotRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}