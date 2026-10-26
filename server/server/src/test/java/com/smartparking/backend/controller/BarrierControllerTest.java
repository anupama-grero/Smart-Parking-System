package com.smartparking.backend.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.smartparking.backend.dto.GateStatusResponse;
import com.smartparking.backend.exception.Esp32UnavailableException;
import com.smartparking.backend.exception.GlobalExceptionHandler;
import com.smartparking.backend.model.GateStatus;
import com.smartparking.backend.model.GateType;
import com.smartparking.backend.service.BarrierService;

class BarrierControllerTest {

    private static final String TOKEN = "test-token";
    private MockMvc mockMvc;
    private BarrierService barrierService;

    @BeforeEach
    void setUp() {
        barrierService = Mockito.mock(BarrierService.class);
        when(barrierService.isGateControlConfigured()).thenReturn(true);
        when(barrierService.isGateControlTokenValid(TOKEN)).thenReturn(true);
        mockMvc = MockMvcBuilders.standaloneSetup(new BarrierController(barrierService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void statusReportsWifiConnectionAndConfirmationState() throws Exception {
        GateStatusResponse response = new GateStatusResponse(
                "Disconnected", "UNKNOWN", "UNSUPPORTED", "No hardware status");
        response.setHardwareConnected(false);
        response.setGateStateConfirmed(false);
        when(barrierService.getGateStatus()).thenReturn(response);

        mockMvc.perform(get("/api/gates/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryGate").value("UNKNOWN"))
                .andExpect(jsonPath("$.exitGate").value("UNSUPPORTED"))
                .andExpect(jsonPath("$.hardwareConnected").value(false))
                .andExpect(jsonPath("$.gateStateConfirmed").value(false));
    }

    @Test
    void openRequestReturnsPendingNotPhysicalSuccess() throws Exception {
        GateStatusResponse response = new GateStatusResponse(
                "Connected", "UNKNOWN", "UNSUPPORTED", "Command awaiting confirmation");
        response.setCommandState("PENDING");
        response.setGateStateConfirmed(false);
        when(barrierService.openEntranceBarrier()).thenReturn(response);

        mockMvc.perform(post("/api/gates/entry/open")
                        .header("X-Gate-Control-Token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryGate").value("UNKNOWN"))
                .andExpect(jsonPath("$.commandState").value("PENDING"))
                .andExpect(jsonPath("$.gateStateConfirmed").value(false));
    }

    @Test
    void missingTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/gates/entry/open"))
                .andExpect(status().isForbidden());
    }

    @Test
    void disabledTokenConfigurationFailsClosed() throws Exception {
        when(barrierService.isGateControlConfigured()).thenReturn(false);

        mockMvc.perform(post("/api/gates/entry/open")
                        .header("X-Gate-Control-Token", TOKEN))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void disconnectedEsp32IsReportedAsGatewayFailure() throws Exception {
        when(barrierService.closeEntranceBarrier())
                .thenThrow(new Esp32UnavailableException("ESP32 disconnected from Wi-Fi"));

        mockMvc.perform(post("/api/gates/entry/close")
                        .header("X-Gate-Control-Token", TOKEN))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value(containsString("disconnected from Wi-Fi")));
    }

    @Test
    void malformedGateControlRequestIsRejectedAfterAuthorization() throws Exception {
        mockMvc.perform(post("/api/gates/control")
                        .header("X-Gate-Control-Token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gate\":\"INVALID_GATE\",\"action\":\"OPEN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Invalid request")));
    }

    @Test
    void commandPollingIsReadOnlyAndDoesNotOpenTheGate() throws Exception {
        when(barrierService.getEntryGateCommand()).thenReturn("CLOSED");

        mockMvc.perform(get("/api/gate/entry/command"))
                .andExpect(status().isOk())
                .andExpect(content().string("CLOSED"));

        Mockito.verify(barrierService, Mockito.never()).openEntranceBarrier();
    }

    @Test
    void firmwareGateStatusReportIsAccepted() throws Exception {
        when(barrierService.recordGateStatus(GateType.ENTRANCE, GateStatus.OPEN))
                .thenReturn(new GateStatusResponse("Connected", "OPEN", "UNSUPPORTED", "Reported"));

        mockMvc.perform(post("/api/gates/entry/status").param("state", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryGate").value("OPEN"));
    }
}
