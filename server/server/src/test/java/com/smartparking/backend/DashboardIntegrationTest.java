package com.smartparking.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class DashboardIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void driverDashboardContainsExactlyFourInitiallyUnknownSlots() throws Exception {
        mockMvc.perform(get("/driver"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("id=\"slot-1-status\""),
                        org.hamcrest.Matchers.containsString("id=\"slot-2-status\""),
                        org.hamcrest.Matchers.containsString("id=\"slot-3-status\""),
                        org.hamcrest.Matchers.containsString("id=\"slot-4-status\""),
                        org.hamcrest.Matchers.containsString("PARKING FULL"),
                        org.hamcrest.Matchers.containsString("Waiting for sensor data"))))
                .andDo(result -> {
                    String html = result.getResponse().getContentAsString();
                    org.junit.jupiter.api.Assertions.assertEquals(4,
                            html.split("class=\"parking-slot card h-100\"", -1).length - 1);
                });
    }

    @Test
    void faviconRedirectsToExistingSvgIcon() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/favicon.svg"));

        mockMvc.perform(get("/favicon.svg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/svg+xml"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<svg")));
    }

    @Test
    void securityDashboardRequiresControlTokenAndStartsWithControlsDisabled() throws Exception {
        mockMvc.perform(get("/security-guard"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("id=\"gateControlToken\""),
                        org.hamcrest.Matchers.containsString("id=\"openActiveBtn\" class=\"btn-action btn-primary\" disabled"),
                        org.hamcrest.Matchers.containsString("id=\"guard-slot-1\""),
                        org.hamcrest.Matchers.containsString("id=\"guard-slot-2\""),
                        org.hamcrest.Matchers.containsString("id=\"guard-slot-3\""),
                        org.hamcrest.Matchers.containsString("id=\"guard-slot-4\""),
                        org.hamcrest.Matchers.containsString("id=\"espStatus\""),
                        org.hamcrest.Matchers.containsString("id=\"espPulse\""),
                        org.hamcrest.Matchers.containsString("id=\"lastHardwareUpdate\""),
                        org.hamcrest.Matchers.containsString("id=\"rwStatus\""),
                        org.hamcrest.Matchers.containsString("id=\"rwPulse\""),
                        org.hamcrest.Matchers.containsString("/js/security-gate.js?v=5"))));
    }

    @Test
    void parkingStatusApiReturnsDirectFourSlotResponseForDashboard() throws Exception {
        mockMvc.perform(get("/api/parking/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots.length()", is(4)))
                .andExpect(jsonPath("$.slots[0].slotNumber").exists())
                .andExpect(jsonPath("$.slots[0].occupied").isBoolean())
                .andExpect(jsonPath("$.slots[0].known").isBoolean())
                .andExpect(jsonPath("$.slots[0].sensorFresh").isBoolean())
                .andExpect(jsonPath("$.slots[3].slotNumber").exists())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void gateStatusApiReturnsDirectResponseWithDashboardFields() throws Exception {
        mockMvc.perform(get("/api/gates/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esp32Status").exists())
                .andExpect(jsonPath("$.entryGate").exists())
                .andExpect(jsonPath("$.hardwareConnected").isBoolean())
                .andExpect(jsonPath("$.gateStateConfirmed").isBoolean())
                .andExpect(jsonPath("$.gateControlEnabled").isBoolean())
                .andExpect(jsonPath("$.entryOpenAllowed").isBoolean())
                .andExpect(jsonPath("$.commandState").exists())
                .andExpect(jsonPath("$.lastHardwareUpdate").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void systemHealthSeparatesBackendDatabaseAndHardwareStatus() throws Exception {
        mockMvc.perform(get("/api/system/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.backend", is("UP")))
                .andExpect(jsonPath("$.database", is("UP")))
                .andExpect(jsonPath("$.esp32Telemetry").exists())
                .andExpect(jsonPath("$.esp32GateControl").exists());
    }
}
