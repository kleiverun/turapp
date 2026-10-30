package com.ole.turapp.controller;

import com.ole.turapp.config.JwtAuthFilter;
import com.ole.turapp.dto.GarminImportResponse;
import com.ole.turapp.dto.TripResponse;
import com.ole.turapp.exception.GarminActivityAlreadyImportedException;
import com.ole.turapp.exception.GarminUnavailableException;
import com.ole.turapp.exception.NotFoundException;
import com.ole.turapp.service.GarminService;
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

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GarminController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class GarminControllerTest {

    private static final String URL = "/api/users/1/garmin/activities/24431089654/import";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GarminService garminService;

    private void authenticateAs(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void importActivity_Owner_ReturnsCreated() throws Exception {
        authenticateAs(1L);
        Instant start = Instant.parse("2026-09-19T16:09:38Z");
        TripResponse trip = new TripResponse(7L, "Hiking 2026-09-19", null, 2713.3, 5886L, "PRIVATE",
                start, start.plusSeconds(5886));
        when(garminService.importActivity(1L, 24431089654L))
                .thenReturn(new GarminImportResponse(24431089654L, 1425, trip));

        mockMvc.perform(post(URL))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.garminActivityId").value(24431089654L))
                .andExpect(jsonPath("$.pointCount").value(1425))
                .andExpect(jsonPath("$.trip.id").value(7))
                .andExpect(jsonPath("$.trip.name").value("Hiking 2026-09-19"));
    }

    @Test
    void importActivity_NotOwner_ReturnsForbidden() throws Exception {
        authenticateAs(2L);

        mockMvc.perform(post(URL))
                .andExpect(status().isForbidden());

        verifyNoInteractions(garminService);
    }

    @Test
    void importActivity_UnknownActivity_ReturnsNotFound() throws Exception {
        authenticateAs(1L);
        when(garminService.importActivity(1L, 24431089654L))
                .thenThrow(new NotFoundException("Did not find Garmin activity 24431089654"));

        mockMvc.perform(post(URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Did not find Garmin activity 24431089654"));
    }

    @Test
    void importActivity_AlreadyImported_ReturnsConflict() throws Exception {
        authenticateAs(1L);
        when(garminService.importActivity(1L, 24431089654L))
                .thenThrow(new GarminActivityAlreadyImportedException(24431089654L));

        mockMvc.perform(post(URL))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Garmin activity 24431089654 has already been imported"));
    }

    @Test
    void importActivity_GarminDown_ReturnsServiceUnavailable() throws Exception {
        authenticateAs(1L);
        when(garminService.importActivity(1L, 24431089654L))
                .thenThrow(new GarminUnavailableException("Could not download the activity from Garmin"));

        mockMvc.perform(post(URL))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void importActivity_NonNumericActivityId_ReturnsBadRequest() throws Exception {
        authenticateAs(1L);

        mockMvc.perform(post("/api/users/1/garmin/activities/abc/import"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(garminService);
    }
}
