package com.smartparking.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.smartparking.backend.dto.SensorOccupancyRequest;
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
}
