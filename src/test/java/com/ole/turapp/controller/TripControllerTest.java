package com.ole.turapp.controller;

import tools.jackson.databind.ObjectMapper;
import com.ole.turapp.dto.TripCreateRequest;
import com.ole.turapp.dto.TripEndRequest;
import com.ole.turapp.dto.TripResponse;
import com.ole.turapp.dto.TripUpdateRequest;
import com.ole.turapp.exception.NotFoundException;
import com.ole.turapp.config.JwtAuthFilter;
import com.ole.turapp.service.TripService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Filters are disabled (addFilters = false); ownership is exercised directly by
 * pushing the same kind of Authentication JwtAuthFilter would set, then asserting
 * on how AuthUtils.requireOwner() reacts.
 */
@WebMvcTest(controllers = TripController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TripService tripService;

    private void authenticateAs(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private TripResponse sampleTripResponse(Long tripId) {
        return new TripResponse(tripId, "Sunset hike", "Notes", 1000.0, 3600L, "PRIVATE", Instant.now(), null);
    }

    @Test
    void createTrip_Owner_ReturnsCreated() throws Exception {
        authenticateAs(1L);
        TripCreateRequest request = new TripCreateRequest("Sunset hike", "Notes", "PRIVATE");
        when(tripService.createTrip(eq(1L), any())).thenReturn(sampleTripResponse(10L));

        mockMvc.perform(post("/api/users/1/trips")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Sunset hike"));
    }

    @Test
    void createTrip_NotOwner_ReturnsForbidden() throws Exception {
        authenticateAs(2L);
        TripCreateRequest request = new TripCreateRequest("Sunset hike", "Notes", "PRIVATE");

        mockMvc.perform(post("/api/users/1/trips")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTrips_Owner_ReturnsList() throws Exception {
        authenticateAs(1L);
        when(tripService.getTripsForUser(1L)).thenReturn(List.of(sampleTripResponse(10L)));

        mockMvc.perform(get("/api/users/1/trips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    void getTrip_Owner_ReturnsTrip() throws Exception {
        authenticateAs(1L);
        when(tripService.getTrip(10L)).thenReturn(sampleTripResponse(10L));

        mockMvc.perform(get("/api/users/1/trips/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void getTrip_NotFound_ReturnsNotFound() throws Exception {
        authenticateAs(1L);
        when(tripService.getTrip(999L)).thenThrow(new NotFoundException("Did not find a trip"));

        mockMvc.perform(get("/api/users/1/trips/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Did not find a trip"));
    }

    @Test
    void updateTrip_Owner_ReturnsUpdatedTrip() throws Exception {
        authenticateAs(1L);
        TripUpdateRequest request = new TripUpdateRequest("New name", "New notes");
        when(tripService.updateTrip(eq(10L), any())).thenReturn(sampleTripResponse(10L));

        mockMvc.perform(patch("/api/users/1/trips/10")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void deleteTrip_Owner_ReturnsNoContent() throws Exception {
        authenticateAs(1L);

        mockMvc.perform(delete("/api/users/1/trips/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTrip_NotOwner_ReturnsForbidden() throws Exception {
        authenticateAs(2L);

        mockMvc.perform(delete("/api/users/1/trips/10"))
                .andExpect(status().isForbidden());
    }

    @Test
    void endTrip_Owner_ReturnsEndedTrip() throws Exception {
        authenticateAs(1L);
        TripEndRequest request = new TripEndRequest(5000.0, 3600L);
        when(tripService.endTrip(eq(10L), any())).thenReturn(sampleTripResponse(10L));

        mockMvc.perform(post("/api/users/1/trips/10/end")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }
}
