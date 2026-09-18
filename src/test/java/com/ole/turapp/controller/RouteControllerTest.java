package com.ole.turapp.controller;

import tools.jackson.databind.ObjectMapper;
import com.ole.turapp.dto.GpxImportResponse;
import com.ole.turapp.dto.RouteCreateRequest;
import com.ole.turapp.dto.RoutePointData;
import com.ole.turapp.dto.RoutePointListResponse;
import com.ole.turapp.dto.RouteResponse;
import com.ole.turapp.dto.RouteUpdateRequest;
import com.ole.turapp.dto.RouteWithPointsResponse;
import com.ole.turapp.config.JwtAuthFilter;
import com.ole.turapp.service.RouteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.mock.web.MockMultipartFile;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RouteController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class RouteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RouteService routeService;

    private void authenticateAs(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private RouteResponse sampleRoute(Long id) {
        return new RouteResponse(id, "Trail A", "desc", "PRIVATE", Instant.now(), 3, "PLANNED");
    }

    @Test
    void importGpx_Owner_ReturnsCreated() throws Exception {
        authenticateAs(1L);
        MockMultipartFile file = new MockMultipartFile(
                "file", "route.gpx", "application/gpx+xml", "<gpx></gpx>".getBytes());
        GpxImportResponse response = new GpxImportResponse(1, 3, List.of(sampleRoute(5L)));
        when(routeService.importGpx(eq(1L), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/users/1/routes/import").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.routeCount").value(1));
    }

    @Test
    void importGpx_EmptyFile_ReturnsBadRequest() throws Exception {
        authenticateAs(1L);
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "route.gpx", "application/gpx+xml", new byte[0]);

        mockMvc.perform(multipart("/api/users/1/routes/import").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No file was uploaded (the 'file' field is empty)"));
    }

    @Test
    void importGpx_NotOwner_ReturnsForbidden() throws Exception {
        authenticateAs(2L);
        MockMultipartFile file = new MockMultipartFile(
                "file", "route.gpx", "application/gpx+xml", "<gpx></gpx>".getBytes());

        mockMvc.perform(multipart("/api/users/1/routes/import").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    void createRoute_Owner_ReturnsCreated() throws Exception {
        authenticateAs(1L);
        RouteCreateRequest request = new RouteCreateRequest("Trail A", "desc", List.of(
                new RouteCreateRequest.PointData(59.9, 10.7),
                new RouteCreateRequest.PointData(59.91, 10.71)));
        when(routeService.createRoute(eq(1L), any())).thenReturn(sampleRoute(5L));

        mockMvc.perform(post("/api/users/1/routes")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void createRoute_NotOwner_ReturnsForbidden() throws Exception {
        authenticateAs(2L);
        RouteCreateRequest request = new RouteCreateRequest("Trail A", "desc", List.of(
                new RouteCreateRequest.PointData(59.9, 10.7),
                new RouteCreateRequest.PointData(59.91, 10.71)));

        mockMvc.perform(post("/api/users/1/routes")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateRoute_Owner_ReturnsUpdatedRoute() throws Exception {
        authenticateAs(1L);
        RouteUpdateRequest request = new RouteUpdateRequest("New name", "New desc");
        when(routeService.updateRoute(eq(1L), eq(5L), any())).thenReturn(sampleRoute(5L));

        mockMvc.perform(patch("/api/users/1/routes/5")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void deleteRoute_Owner_ReturnsNoContent() throws Exception {
        authenticateAs(1L);

        mockMvc.perform(delete("/api/users/1/routes/5"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getRoutes_Owner_ReturnsList() throws Exception {
        authenticateAs(1L);
        when(routeService.getRoutesForUser(1L)).thenReturn(List.of(sampleRoute(5L)));

        mockMvc.perform(get("/api/users/1/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    void getRoutesWithPoints_Owner_ReturnsList() throws Exception {
        authenticateAs(1L);
        RouteWithPointsResponse response = new RouteWithPointsResponse(
                5L, "Trail A", "PLANNED", List.of(new RoutePointData(0, 59.9, 10.7)));
        when(routeService.getRoutesWithPointsForUser(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/users/1/routes/with-points"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].points[0].latitude").value(59.9));
    }

    @Test
    void getRoutePoints_ReturnsPoints_NoOwnershipRequired() throws Exception {
        RoutePointListResponse response = new RoutePointListResponse(5L, List.of(new RoutePointData(0, 59.9, 10.7)));
        when(routeService.getPointsForRoute(5L)).thenReturn(response);

        mockMvc.perform(get("/api/routes/5/points"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeId").value(5));
    }
}
