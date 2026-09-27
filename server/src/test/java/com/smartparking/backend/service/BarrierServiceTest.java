package com.smartparking.backend.service;

import com.smartparking.backend.dto.GateStatusResponse;
import com.smartparking.backend.exception.Esp32UnavailableException;
import com.smartparking.backend.exception.InvalidGateException;
import com.smartparking.backend.model.GateAction;
import com.smartparking.backend.model.GateStatus;
import com.smartparking.backend.model.GateType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BarrierServiceTest {

    private BarrierService barrierService;

    @BeforeEach
    void setUp() {
        barrierService = new BarrierService(
                "http://localhost:9999",
                1000,
                1000,
                true // Mock mode enabled for fast unit testing
        );
    }

    @Test
    void testGetInitialGateStatus() {
        GateStatusResponse response = barrierService.getGateStatus();
        assertNotNull(response);
        assertEquals("CLOSED", response.getEntryGate());
        assertEquals("CLOSED", response.getExitGate());
        assertTrue(response.getEsp32Status().contains("Online"));
    }

    @Test
    void testOpenEntranceBarrier() {
        GateStatusResponse response = barrierService.openEntranceBarrier();
        assertEquals("OPEN", response.getEntryGate());
        assertEquals("CLOSED", response.getExitGate());
        assertTrue(response.getMessage().toLowerCase().contains("entrance barrier opened"));
    }

    @Test
    void testCloseEntranceBarrier() {
        barrierService.openEntranceBarrier();
        GateStatusResponse response = barrierService.closeEntranceBarrier();
        assertEquals("CLOSED", response.getEntryGate());
        assertTrue(response.getMessage().toLowerCase().contains("entrance barrier closed"));
    }

    @Test
    void testOpenExitBarrier() {
        GateStatusResponse response = barrierService.openExitBarrier();
        assertEquals("OPEN", response.getExitGate());
        assertEquals("CLOSED", response.getEntryGate());
        assertTrue(response.getMessage().toLowerCase().contains("exit barrier opened"));
    }

    @Test
    void testCloseExitBarrier() {
        barrierService.openExitBarrier();
        GateStatusResponse response = barrierService.closeExitBarrier();
        assertEquals("CLOSED", response.getExitGate());
        assertTrue(response.getMessage().toLowerCase().contains("exit barrier closed"));
    }

    @Test
    void testControlBarrierWithNullGate() {
        assertThrows(InvalidGateException.class, () -> {
            barrierService.controlBarrier(null, GateAction.OPEN);
        });
    }

    @Test
    void testControlBarrierWithNullAction() {
        assertThrows(InvalidGateException.class, () -> {
            barrierService.controlBarrier(GateType.ENTRANCE, null);
        });
    }

    @Test
    void testEsp32UnavailableWhenMockModeDisabled() {
        BarrierService realHardwareService = new BarrierService(
                "http://127.0.0.1:59999", // Unreachable port
                100,
                100,
                false // Mock mode disabled
        );

        assertThrows(Esp32UnavailableException.class, () -> {
            realHardwareService.openEntranceBarrier();
        });
    }

    @Test
    void testRepeatedGateOpenRequestHandledSafely() {
        GateStatusResponse first = barrierService.openEntranceBarrier();
        assertEquals("OPEN", first.getEntryGate());

        GateStatusResponse second = barrierService.openEntranceBarrier();
        assertEquals("OPEN", second.getEntryGate());
        assertTrue(second.getMessage().toLowerCase().contains("opened"));
    }

    @Test
    void testEntranceAndExitBarrierIndependence() {
        barrierService.openEntranceBarrier();
        assertEquals(GateStatus.OPEN, barrierService.getEntranceGateState());
        assertEquals(GateStatus.CLOSED, barrierService.getExitGateState());

        barrierService.openExitBarrier();
        assertEquals(GateStatus.OPEN, barrierService.getEntranceGateState());
        assertEquals(GateStatus.OPEN, barrierService.getExitGateState());

        barrierService.closeEntranceBarrier();
        assertEquals(GateStatus.CLOSED, barrierService.getEntranceGateState());
        assertEquals(GateStatus.OPEN, barrierService.getExitGateState());
    }

    @Test
    void testBarrierServicePersistsGateStateToRepository() {
        com.smartparking.backend.repository.GateRepository mockRepo = 
                org.mockito.Mockito.mock(com.smartparking.backend.repository.GateRepository.class);
        com.smartparking.backend.model.Gate mockGate = 
                new com.smartparking.backend.model.Gate("Main Entrance Gate", GateType.ENTRANCE, GateStatus.CLOSED);

        org.mockito.Mockito.when(mockRepo.findByGateType(GateType.ENTRANCE))
                .thenReturn(java.util.Optional.of(mockGate));

        BarrierService serviceWithRepo = new BarrierService(
                mockRepo,
                "http://localhost:9999",
                1000,
                1000,
                true
        );

        GateStatusResponse response = serviceWithRepo.openEntranceBarrier();
        assertEquals("OPEN", response.getEntryGate());
        org.mockito.Mockito.verify(mockRepo, org.mockito.Mockito.atLeastOnce())
                .save(org.mockito.ArgumentMatchers.any(com.smartparking.backend.model.Gate.class));
        assertEquals(GateStatus.OPEN, mockGate.getStatus());
    }

    @Test
    void testBarrierServiceFetchesPersistedGateState() {
        com.smartparking.backend.repository.GateRepository mockRepo = 
                org.mockito.Mockito.mock(com.smartparking.backend.repository.GateRepository.class);
        com.smartparking.backend.model.Gate openEntranceGate = 
                new com.smartparking.backend.model.Gate("Main Entrance Gate", GateType.ENTRANCE, GateStatus.OPEN);
        com.smartparking.backend.model.Gate closedExitGate = 
                new com.smartparking.backend.model.Gate("Main Exit Gate", GateType.EXIT, GateStatus.CLOSED);

        org.mockito.Mockito.when(mockRepo.findByGateType(GateType.ENTRANCE))
                .thenReturn(java.util.Optional.of(openEntranceGate));
        org.mockito.Mockito.when(mockRepo.findByGateType(GateType.EXIT))
                .thenReturn(java.util.Optional.of(closedExitGate));

        BarrierService serviceWithRepo = new BarrierService(
                mockRepo,
                "http://localhost:9999",
                1000,
                1000,
                true
        );

        GateStatusResponse response = serviceWithRepo.getGateStatus();
        assertEquals("OPEN", response.getEntryGate());
        assertEquals("CLOSED", response.getExitGate());
    }
}
