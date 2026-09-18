package com.ole.turapp.service;

import com.ole.turapp.dto.GpxImportResponse;
import com.ole.turapp.dto.RouteCreateRequest;
import com.ole.turapp.dto.RoutePointListResponse;
import com.ole.turapp.dto.RouteResponse;
import com.ole.turapp.dto.RouteUpdateRequest;
import com.ole.turapp.dto.RouteWithPointsResponse;
import com.ole.turapp.exception.NotFoundException;
import com.ole.turapp.model.Role;
import com.ole.turapp.model.Route;
import com.ole.turapp.model.RoutePoint;
import com.ole.turapp.model.User;
import com.ole.turapp.repository.RoutePointRepository;
import com.ole.turapp.repository.RouteRepository;
import com.ole.turapp.repository.UserRepository;
import com.ole.turapp.service.GpxRouteParser.Coordinate;
import com.ole.turapp.service.GpxRouteParser.ParsedRoute;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private RoutePointRepository routePointRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GpxRouteParser gpxRouteParser;

    @InjectMocks
    private RouteService routeService;

    private User existingUser(Long id) {
        User user = new User("hiker@example.com", "hash", "Hiker", Role.USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Route existingRoute(Long id, User owner) {
        Route route = new Route("Trail A", owner);
        ReflectionTestUtils.setField(route, "id", id);
        return route;
    }

    private RouteCreateRequest validCreateRequest() {
        return new RouteCreateRequest("Trail A", "desc", List.of(
                new RouteCreateRequest.PointData(59.9, 10.7),
                new RouteCreateRequest.PointData(59.91, 10.71)));
    }

    @Test
    void createRoute_Success() {
        User user = existingUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> {
            Route r = invocation.getArgument(0);
            ReflectionTestUtils.setField(r, "id", 5L);
            return r;
        });
        when(routePointRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        RouteResponse response = routeService.createRoute(1L, validCreateRequest());

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.name()).isEqualTo("Trail A");
        assertThat(response.pointCount()).isEqualTo(2);
    }

    @Test
    void createRoute_UserNotFound_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.createRoute(1L, validCreateRequest()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createRoute_BlankName_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser(1L)));
        RouteCreateRequest request = new RouteCreateRequest("  ", "desc", List.of(
                new RouteCreateRequest.PointData(59.9, 10.7),
                new RouteCreateRequest.PointData(59.91, 10.71)));

        assertThatThrownBy(() -> routeService.createRoute(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Route name is required");
    }

    @Test
    void createRoute_TooFewPoints_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser(1L)));
        RouteCreateRequest request = new RouteCreateRequest("Trail A", "desc",
                List.of(new RouteCreateRequest.PointData(59.9, 10.7)));

        assertThatThrownBy(() -> routeService.createRoute(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A route must have at least 2 points");
    }

    @Test
    void updateRoute_Owner_UpdatesNameAndDescription() {
        User owner = existingUser(1L);
        Route route = existingRoute(5L, owner);
        when(routeRepository.findById(5L)).thenReturn(Optional.of(route));
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(routePointRepository.countByRouteId(5L)).thenReturn(2);

        RouteResponse response = routeService.updateRoute(1L, 5L, new RouteUpdateRequest("New name", "New desc"));

        assertThat(response.name()).isEqualTo("New name");
        assertThat(response.description()).isEqualTo("New desc");
    }

    @Test
    void updateRoute_NotOwner_ThrowsNotFound() {
        User owner = existingUser(1L);
        Route route = existingRoute(5L, owner);
        when(routeRepository.findById(5L)).thenReturn(Optional.of(route));

        assertThatThrownBy(() -> routeService.updateRoute(2L, 5L, new RouteUpdateRequest("New name", null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateRoute_DoesNotExist_ThrowsNotFound() {
        when(routeRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.updateRoute(1L, 5L, new RouteUpdateRequest("New name", null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteRoute_Owner_DeletesRouteAndPoints() {
        User owner = existingUser(1L);
        Route route = existingRoute(5L, owner);
        when(routeRepository.findById(5L)).thenReturn(Optional.of(route));

        routeService.deleteRoute(1L, 5L);

        verify(routePointRepository).deleteByRouteId(5L);
        verify(routeRepository).delete(route);
    }

    @Test
    void deleteRoute_NotOwner_ThrowsNotFound_AndDeletesNothing() {
        User owner = existingUser(1L);
        Route route = existingRoute(5L, owner);
        when(routeRepository.findById(5L)).thenReturn(Optional.of(route));

        assertThatThrownBy(() -> routeService.deleteRoute(2L, 5L))
                .isInstanceOf(NotFoundException.class);

        verify(routePointRepository, never()).deleteByRouteId(any());
        verify(routeRepository, never()).delete(any());
    }

    @Test
    void getRoutesForUser_ReturnsMappedResponses() {
        User owner = existingUser(1L);
        Route route = existingRoute(5L, owner);
        when(routeRepository.findByUserId(1L)).thenReturn(List.of(route));
        when(routePointRepository.countByRouteId(5L)).thenReturn(4);

        List<RouteResponse> responses = routeService.getRoutesForUser(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).pointCount()).isEqualTo(4);
    }

    @Test
    void getPointsForRoute_Success() {
        when(routeRepository.existsById(5L)).thenReturn(true);
        when(routePointRepository.findByRouteIdOrderByPointOrderAsc(5L))
                .thenReturn(List.of(new RoutePoint(0, 59.9, 10.7)));

        RoutePointListResponse response = routeService.getPointsForRoute(5L);

        assertThat(response.routeId()).isEqualTo(5L);
        assertThat(response.points()).hasSize(1);
    }

    @Test
    void getPointsForRoute_DoesNotExist_ThrowsNotFound() {
        when(routeRepository.existsById(5L)).thenReturn(false);

        assertThatThrownBy(() -> routeService.getPointsForRoute(5L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getRoutesWithPointsForUser_GroupsPointsByRoute() {
        User owner = existingUser(1L);
        Route route = existingRoute(5L, owner);
        when(routeRepository.findByUserId(1L)).thenReturn(List.of(route));

        RoutePoint point = new RoutePoint(0, 59.9, 10.7);
        point.setRoute(route);
        when(routePointRepository.findByRouteUserIdOrderByRouteIdAscPointOrderAsc(1L)).thenReturn(List.of(point));

        List<RouteWithPointsResponse> responses = routeService.getRoutesWithPointsForUser(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).points()).hasSize(1);
    }

    @Test
    void importGpx_Success_CreatesRouteFromParsedGpx() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser(1L)));
        when(routeRepository.findByUserId(1L)).thenReturn(List.of());
        when(routePointRepository.findByRouteUserIdOrderByRouteIdAscPointOrderAsc(1L)).thenReturn(List.of());

        ParsedRoute parsed = new ParsedRoute("Imported trail", "desc",
                List.of(new Coordinate(59.9, 10.7), new Coordinate(59.91, 10.71)));
        when(gpxRouteParser.parse(any())).thenReturn(List.of(parsed));

        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> {
            Route r = invocation.getArgument(0);
            ReflectionTestUtils.setField(r, "id", 7L);
            return r;
        });
        when(routePointRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        GpxImportResponse response = routeService.importGpx(1L, new ByteArrayInputStream("<gpx/>".getBytes()));

        assertThat(response.routeCount()).isEqualTo(1);
        assertThat(response.pointCount()).isEqualTo(2);
        assertThat(response.routes().get(0).name()).isEqualTo("Imported trail");
        assertThat(response.routes().get(0).source()).isEqualTo("GPX_IMPORT");
    }

    @Test
    void importGpx_UserNotFound_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.importGpx(1L, new ByteArrayInputStream("<gpx/>".getBytes())))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void importGpx_NoRoutesInFile_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser(1L)));
        when(gpxRouteParser.parse(any())).thenReturn(List.of());

        assertThatThrownBy(() -> routeService.importGpx(1L, new ByteArrayInputStream("<gpx/>".getBytes())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The GPX file contains no routes (<rte>)");
    }

    @Test
    void importGpx_OnlyRouteHasNoPoints_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser(1L)));
        when(routeRepository.findByUserId(1L)).thenReturn(List.of());
        when(routePointRepository.findByRouteUserIdOrderByRouteIdAscPointOrderAsc(1L)).thenReturn(List.of());

        ParsedRoute emptyRoute = new ParsedRoute("Empty", null, List.of());
        when(gpxRouteParser.parse(any())).thenReturn(List.of(emptyRoute));

        assertThatThrownBy(() -> routeService.importGpx(1L, new ByteArrayInputStream("<gpx/>".getBytes())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No new routes to import — all routes in the file already exist (or are missing points)");
    }
}
