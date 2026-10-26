package com.smartparking.backend.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.smartparking.backend.model.ParkingSlot;
import com.smartparking.backend.service.ParkingSlotService;

class ParkingSlotControllerTest {

    private MockMvc mockMvc;
    private ParkingSlotService parkingSlotService;

    @BeforeEach
    void setUp() {
        parkingSlotService = Mockito.mock(ParkingSlotService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ParkingSlotController(parkingSlotService)).build();
    }

    @Test
    void parkingStatusIncludesPersistedSensorReadings() throws Exception {
        ParkingSlot slot = new ParkingSlot(1L, 1, "Undergraduates", true);
        slot.setLastDistanceCm(24.5);
        slot.setLastSensorUpdate(LocalDateTime.of(2026, 10, 22, 14, 30));
        when(parkingSlotService.getAllSlots()).thenReturn(List.of(slot));

        mockMvc.perform(get("/api/parking/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slotId").value(1))
                .andExpect(jsonPath("$[0].isOccupied").value(true))
                .andExpect(jsonPath("$[0].lastDistanceCm").value(24.5))
                .andExpect(jsonPath("$[0].lastSensorUpdate").value("2026-10-22T14:30:00"));
    }
}