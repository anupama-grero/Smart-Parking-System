package com.smartparking.backend;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.smartparking.backend.model.ParkingSlot;
import com.smartparking.backend.repository.ParkingSlotRepository;

@SpringBootTest
class ParkingSlotIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ParkingSlotRepository slotRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        for (int number = 1; number <= 4; number++) {
            ParkingSlot slot = slotRepository.findBySlotNumber(number).orElseThrow();
            slot.setOccupied(false);
            slot.setLastDistanceCm(null);
            slot.setLastSensorUpdate(null);
            slotRepository.save(slot);
        }
    }

    @Test
    void onlyFourPhysicalSlotsAreExposed() throws Exception {
        mockMvc.perform(get("/api/parking/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(4)))
                .andExpect(jsonPath("$[0].slotNumber", is(1)))
                .andExpect(jsonPath("$[3].slotNumber", is(4)));

        mockMvc.perform(get("/api/parking/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots", org.hamcrest.Matchers.hasSize(4)))
                .andExpect(jsonPath("$.availableSlots", is(0)))
                .andExpect(jsonPath("$.occupiedSlots", is(0)))
                .andExpect(jsonPath("$.unknownSlots", is(4)));
    }

    @Test
    void creatingAnObsoleteFifthSlotIsRejected() throws Exception {
        mockMvc.perform(post("/api/parking/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotNumber\":5,\"category\":\"Legacy\",\"isOccupied\":false}"))
                .andExpect(status().isBadRequest());
    }
}
