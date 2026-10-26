package com.smartparking.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.smartparking.backend.dto.SensorOccupancyRequest;
import com.smartparking.backend.dto.SensorOccupancyResponse;
import com.smartparking.backend.exception.GlobalExceptionHandler;
import com.smartparking.backend.service.SensorOccupancyService;

class SensorOccupancyControllerTest {

    private MockMvc mockMvc;
    private SensorOccupancyService sensorOccupancyService;

    @BeforeEach
    void setUp() {
        sensorOccupancyService = Mockito.mock(SensorOccupancyService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SensorOccupancyController(sensorOccupancyService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void databaseFailureReturnsInternalServerErrorWithoutStackTrace() throws Exception {
        Mockito.when(sensorOccupancyService.processReading(Mockito.any(SensorOccupancyRequest.class)))
                .thenThrow(new DataAccessResourceFailureException("connection refused"));

        mockMvc.perform(post("/api/sensors/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slotId\":1,\"distance\":20.0}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Database error while processing the request"));
    }

    @Test
    void batchOccupancyAcceptsFirmwarePayload() throws Exception {
        Mockito.when(sensorOccupancyService.processBatchReading(Mockito.any()))
                .thenReturn(List.of(new SensorOccupancyResponse(1, 1, 120.0, false, "AVAILABLE", false, false, null)));

        // Exact key names emitted by the unchanged four-slot firmware.
        String firmwareJson = "{\"availableSlots\":3,\"occupiedSlots\":1,"
                + "\"slots\":[{\"slot\":1,\"occupied\":false,\"distance\":120.0},"
                + "{\"slot\":2,\"occupied\":true,\"distance\":4.5},"
                + "{\"slot\":3,\"occupied\":false,\"distance\":-1},"
                + "{\"slot\":4,\"occupied\":false,\"distance\":150.0}]}";

        mockMvc.perform(post("/api/sensors/occupancy/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(firmwareJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slotId").value(1))
                .andExpect(jsonPath("$[0].availability").value("AVAILABLE"));

        Mockito.verify(sensorOccupancyService).processBatchReading(Mockito.any());
    }
}
