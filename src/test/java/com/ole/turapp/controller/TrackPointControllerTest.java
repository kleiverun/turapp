package com.ole.turapp.controller;

import tools.jackson.databind.ObjectMapper;
import com.ole.turapp.dto.TrackPointCreateRequest;
import com.ole.turapp.dto.TrackPointData;
import com.ole.turapp.dto.TrackPointListResponse;
import com.ole.turapp.config.JwtAuthFilter;
import com.ole.turapp.service.TrackPointService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TrackPointController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class TrackPointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TrackPointService trackPointService;

    @Test
    void addPoints_ReturnsCreated() throws Exception {
        TrackPointCreateRequest request = new TrackPointCreateRequest(10L,
                List.of(new TrackPointData(59.9, 10.7, Instant.now(), 120.0)));
        TrackPointListResponse response = new TrackPointListResponse(10L, request.points());
        when(trackPointService.addPoints(any())).thenReturn(response);

        mockMvc.perform(post("/api/trackpoints")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tripId").value(10));
    }

    @Test
    void addPoints_MissingPoints_ReturnsBadRequest() throws Exception {
        TrackPointCreateRequest request = new TrackPointCreateRequest(10L, List.of());
        when(trackPointService.addPoints(any()))
                .thenThrow(new IllegalArgumentException("Request must contain at least one trackpoint"));

        mockMvc.perform(post("/api/trackpoints")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request must contain at least one trackpoint"));
    }

    @Test
    void getPoints_ReturnsPointsForTrip() throws Exception {
        TrackPointListResponse response = new TrackPointListResponse(10L,
                List.of(new TrackPointData(59.9, 10.7, Instant.now(), 120.0)));
        when(trackPointService.getPointsForTrip(10L)).thenReturn(response);

        mockMvc.perform(get("/api/trips/10/trackpoints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points[0].latitude").value(59.9));
    }
}
