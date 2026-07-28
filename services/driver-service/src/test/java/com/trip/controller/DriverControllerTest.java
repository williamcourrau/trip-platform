package com.trip.controller;

import com.trip.model.Driver;
import com.trip.repository.DriverRepository;
import com.trip.service.DriverService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DriverController.class)
class DriverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DriverRepository driverRepository;

    @MockitoBean
    private DriverService driverService;

    @Test
    void getAllDrivers_shouldReturnList() throws Exception {
        Driver driver = new Driver();
        driver.setId("driver-1");
        driver.setName("John");

        when(driverRepository.findAll()).thenReturn(List.of(driver));

        mockMvc.perform(get("/api/drivers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("driver-1"));
    }

    @Test
    void getAvailableDrivers_shouldReturnList() throws Exception {
        Driver driver = new Driver();
        driver.setId("driver-1");
        driver.setAvailable(true);

        when(driverRepository.findByAvailableTrue()).thenReturn(List.of(driver));

        mockMvc.perform(get("/api/drivers/available"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("driver-1"));
    }

    @Test
    void createDriver_shouldReturnDriver() throws Exception {
        Driver driver = new Driver();
        driver.setId("driver-1");
        driver.setName("John");

        when(driverRepository.save(any())).thenReturn(driver);

        mockMvc.perform(post("/api/drivers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(driver)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("driver-1"));
    }

    @Test
    void respondToTrip_shouldReturnOk() throws Exception {
        doNothing().when(driverService).handleDriverResponse(anyString(), anyString(), anyString());

        mockMvc.perform(post("/api/drivers/driver-1/respond")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("tripId", "trip-1", "action", "accept"))))
            .andExpect(status().isOk());
    }
}
