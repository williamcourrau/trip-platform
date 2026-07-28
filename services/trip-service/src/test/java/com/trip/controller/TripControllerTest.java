package com.trip.controller;

import com.trip.model.Trip;
import com.trip.service.TripService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TripController.class)
class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TripService tripService;

    @Test
    void createTrip_shouldReturnTrip() throws Exception {
        Trip trip = new Trip();
        trip.setId("trip-1");
        trip.setUserId("user-1");
        trip.setStatus(Trip.TripStatus.PENDING);

        when(tripService.createTrip(any(), any(), any(), any(), any(), any(), any())).thenReturn(trip);

        Map<String, Object> request = Map.of(
            "pickupLat", 37.7749, "pickupLng", -122.4194,
            "dropoffLat", 37.7852, "dropoffLng", -122.4312,
            "pickupAddress", "Pickup St", "dropoffAddress", "Dropoff Ave",
            "userId", "user-1"
        );

        mockMvc.perform(post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("trip-1"));
    }

    @Test
    void getTrip_shouldReturnTrip() throws Exception {
        Trip trip = new Trip();
        trip.setId("trip-1");
        trip.setUserId("user-1");

        when(tripService.getTrip("trip-1")).thenReturn(trip);

        mockMvc.perform(get("/api/trips/trip-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("trip-1"));
    }

    @Test
    void cancelTrip_shouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/trips/trip-1/cancel"))
            .andExpect(status().isOk());
    }
}
