package com.smartparking.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartparking.backend.dto.SensorOccupancyRequest;
import com.smartparking.backend.dto.SensorOccupancyResponse;
import com.smartparking.backend.exception.ResourceNotFoundException;
import com.smartparking.backend.model.Driver;
import com.smartparking.backend.model.ParkingSession;
import com.smartparking.backend.model.ParkingSlot;
import com.smartparking.backend.model.SessionStatus;
import com.smartparking.backend.repository.ParkingSessionRepository;
import com.smartparking.backend.repository.ParkingSlotRepository;

@ExtendWith(MockitoExtension.class)
class SensorOccupancyServiceTest {

    @Mock
    private ParkingSlotRepository slotRepository;

    @Mock
    private ParkingSessionRepository sessionRepository;

    private SensorOccupancyService service;
    private ParkingSlot slot1;

    @BeforeEach
    void setUp() {
        SensorSlotMapper mapper = new SensorSlotMapper(1, 8);
        service = new SensorOccupancyService(slotRepository, sessionRepository, mapper, 50.0, 400.0);
        slot1 = new ParkingSlot(1L, 1, "Undergraduates", false);
    }

    @Test
    void distanceAboveThresholdMarksAvailable() {
        when(slotRepository.findBySlotNumber(1)).thenReturn(Optional.of(slot1));
        when(sessionRepository.findByParkingSlotAndStatus(slot1, SessionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(slotRepository.save(any(ParkingSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SensorOccupancyResponse response = service.processReading(new SensorOccupancyRequest(1, 120.0));

        assertFalse(response.isOccupied());
        assertEquals("AVAILABLE", response.getAvailability());
        assertFalse(slot1.isOccupied());
        assertEquals(120.0, slot1.getLastDistanceCm());
    }

    @Test
    void distanceAtOrBelowThresholdMarksOccupied() {
        when(slotRepository.findBySlotNumber(1)).thenReturn(Optional.of(slot1));
        when(sessionRepository.findByParkingSlotAndStatus(slot1, SessionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(slotRepository.save(any(ParkingSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SensorOccupancyResponse response = service.processReading(new SensorOccupancyRequest(1, 12.5));

        assertTrue(response.isOccupied());
        assertEquals("OCCUPIED", response.getAvailability());
        assertTrue(slot1.isOccupied());
    }

    @Test
    void unknownSlotThrowsNotFound() {
        when(slotRepository.findBySlotNumber(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.processReading(new SensorOccupancyRequest(1, 20.0)));
        verify(slotRepository, never()).save(any());
    }

    @Test
    void negativeDistanceIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.processReading(new SensorOccupancyRequest(1, -1.0)));
        verify(slotRepository, never()).save(any());
    }

    @Test
    void missingDistanceIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.processReading(new SensorOccupancyRequest(1, null)));
        verify(slotRepository, never()).save(any());
    }

    @Test
    void outOfRangeDistanceIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.processReading(new SensorOccupancyRequest(1, 401.0)));
        verify(slotRepository, never()).save(any());
    }

    @Test
    void unknownSensorIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.processReading(new SensorOccupancyRequest(null, 9, 20.0)));
        verify(slotRepository, never()).save(any());
    }

    @Test
    void sensorSlotMismatchIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.processReading(new SensorOccupancyRequest(1, 2, 20.0)));
        verify(slotRepository, never()).save(any());
    }

    @Test
    void sensorIdMapsOneToOneToSlot() {
        when(slotRepository.findBySlotNumber(3)).thenReturn(Optional.of(new ParkingSlot(3L, 3, "Visiting Lecturers", false)));
        when(sessionRepository.findByParkingSlotAndStatus(any(), any())).thenReturn(Optional.empty());
        when(slotRepository.save(any(ParkingSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SensorOccupancyResponse response = service.processReading(new SensorOccupancyRequest(null, 3, 10.0));

        assertEquals(3, response.getSlotId());
        assertEquals(3, response.getSensorId());
        assertTrue(response.isOccupied());
    }

    @Test
    void activeSessionPreventsSensorFromClearingOccupancy() {
        when(slotRepository.findBySlotNumber(1)).thenReturn(Optional.of(slot1));
        ParkingSession active = new ParkingSession(
                new Driver("D1", "Test", "ABC"),
                slot1,
                java.time.LocalDateTime.now(),
                SessionStatus.ACTIVE
        );
        when(sessionRepository.findByParkingSlotAndStatus(slot1, SessionStatus.ACTIVE))
                .thenReturn(Optional.of(active));
        when(slotRepository.save(any(ParkingSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SensorOccupancyResponse response = service.processReading(new SensorOccupancyRequest(1, 200.0));

        assertTrue(response.isOccupied());
        assertTrue(response.isHeldByActiveSession());
        assertEquals("OCCUPIED", response.getAvailability());
        assertTrue(slot1.isOccupied());
    }

    @Test
    void repeatedReadingsUpdateSameSlot() {
        when(slotRepository.findBySlotNumber(1)).thenReturn(Optional.of(slot1));
        when(sessionRepository.findByParkingSlotAndStatus(slot1, SessionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(slotRepository.save(any(ParkingSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.processReading(new SensorOccupancyRequest(1, 10.0));
        SensorOccupancyResponse second = service.processReading(new SensorOccupancyRequest(1, 180.0));

        assertFalse(second.isOccupied());
        assertEquals(180.0, slot1.getLastDistanceCm());
        verify(slotRepository, org.mockito.Mockito.times(2)).save(slot1);
    }
}
