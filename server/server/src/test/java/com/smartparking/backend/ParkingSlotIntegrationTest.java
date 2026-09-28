package com.smartparking.backend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.smartparking.backend.repository.ParkingSlotRepository;

@SpringBootTest
class ParkingSlotIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ParkingSlotRepository parkingSlotRepository;

    private MockMvc mockMvc;
    private int slotNumber;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        slotNumber = 1_000_000 + (int) Math.floorMod(System.nanoTime(), 1_000_000_000L);
    }

    @AfterEach
    void cleanUp() {
        parkingSlotRepository.findBySlotNumber(slotNumber)
                .ifPresent(parkingSlotRepository::delete);
    }

    @Test
    void slotCrudPersistsThroughH2BackedApi() throws Exception {
        String createResponse = mockMvc.perform(post("/api/parking/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotJson("Test category")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slotNumber", is(slotNumber)))
                .andExpect(jsonPath("$.category", is("Test category")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = ((Number) JsonPath.read(createResponse, "$.id")).longValue();
        assertTrue(parkingSlotRepository.findBySlotNumber(slotNumber).isPresent());

        mockMvc.perform(get("/api/parking/slots/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotNumber", is(slotNumber)));

        mockMvc.perform(put("/api/parking/slots/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotJson("Updated category")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category", is("Updated category")));

        mockMvc.perform(delete("/api/parking/slots/{id}", id))
                .andExpect(status().isNoContent());
        assertFalse(parkingSlotRepository.findBySlotNumber(slotNumber).isPresent());
    }

    @Test
    void createRejectsNonPositiveSlotNumberAndBlankCategory() throws Exception {
        mockMvc.perform(post("/api/parking/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotNumber\":0,\"category\":\" \"}"))
                .andExpect(status().isBadRequest());

        assertFalse(parkingSlotRepository.findBySlotNumber(0).isPresent());
    }

    private String slotJson(String category) {
        return String.format(
                "{\"slotNumber\":%d,\"category\":\"%s\",\"isOccupied\":false}",
                slotNumber,
                category);
    }
}