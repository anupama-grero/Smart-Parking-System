package com.smartparking.backend.controller;

import com.smartparking.backend.dto.GateStatusResponse;
import com.smartparking.backend.exception.Esp32UnavailableException;
import com.smartparking.backend.exception.GlobalExceptionHandler;
import com.smartparking.backend.service.BarrierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BarrierControllerTest {

    private MockMvc mockMvc;
    private BarrierService barrierService;

    @BeforeEach
    void setUp() {
        barrierService = Mockito.mock(BarrierService.class);
        BarrierController controller = new BarrierController(barrierService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetGateStatus() throws Exception {
        GateStatusResponse mockResponse =
                new GateStatusResponse("Online (Mock)", "CLOSED", "CLOSED", "Success");
        when(barrierService.getGateStatus()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/gates/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esp32Status").value("Online (Mock)"))
                .andExpect(jsonPath("$.entryGate").value("CLOSED"))
                .andExpect(jsonPath("$.exitGate").value("CLOSED"));
    }

    @Test
    void testOpenEntranceBarrier() throws Exception {
        GateStatusResponse mockResponse =
                new GateStatusResponse("Online (Mock)", "OPEN", "CLOSED", "Entrance barrier opened successfully!");
        when(barrierService.openEntranceBarrier()).thenReturn(mockResponse);

        mockMvc.perform(post("/api/gates/entry/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryGate").value("OPEN"))
                .andExpect(jsonPath("$.message").value(containsString("opened")));
    }

    @Test
    void testOpenExitBarrier() throws Exception {
        GateStatusResponse mockResponse =
                new GateStatusResponse("Online (Mock)", "CLOSED", "OPEN", "Exit barrier opened successfully!");
        when(barrierService.openExitBarrier()).thenReturn(mockResponse);

        mockMvc.perform(post("/api/gates/exit/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitGate").value("OPEN"))
                .andExpect(jsonPath("$.message").value(containsString("opened")));
    }

    @Test
    void testControlGateWithJsonBody() throws Exception {
        GateStatusResponse mockResponse =
                new GateStatusResponse("Online (Mock)", "OPEN", "CLOSED", "Entrance barrier opened successfully!");
        when(barrierService.controlBarrier(any(), any())).thenReturn(mockResponse);

        String jsonBody = "{\"gate\": \"ENTRANCE\", \"action\": \"OPEN\"}";

        mockMvc.perform(post("/api/gates/control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryGate").value("OPEN"));
    }

    @Test
    void testInvalidGateControlRequest() throws Exception {
        String jsonBody = "{\"gate\": \"INVALID_GATE\", \"action\": \"OPEN\"}";

        mockMvc.perform(post("/api/gates/control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("Invalid request")));
    }

    @Test
    void testEsp32UnavailableReturnsBadGateway() throws Exception {
        when(barrierService.openEntranceBarrier()).thenThrow(new Esp32UnavailableException("ESP32 device is offline"));

        mockMvc.perform(post("/api/gates/entry/open"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("ESP32 device is offline")));
    }
}
