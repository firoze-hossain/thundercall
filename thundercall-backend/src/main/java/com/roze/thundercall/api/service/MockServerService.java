package com.roze.thundercall.api.service;

import com.roze.thundercall.api.dto.MockRouteRequest;
import com.roze.thundercall.api.dto.MockRouteResponse;
import com.roze.thundercall.api.dto.MockServerRequest;
import com.roze.thundercall.api.dto.MockServerResponse;
import com.roze.thundercall.api.entity.MockRoute;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.enums.HttpMethod;

import java.util.List;
import java.util.Optional;

public interface MockServerService {
    MockServerResponse createMockServer(MockServerRequest request, User owner);

    List<MockServerResponse> getMyMockServers(User owner);

    MockServerResponse getMockServer(Long id, User owner);

    void setEnabled(Long id, boolean enabled, User owner);

    void deleteMockServer(Long id, User owner);

    MockRouteResponse addRoute(Long mockServerId, MockRouteRequest request, User owner);

    List<MockRouteResponse> getRoutes(Long mockServerId, User owner);

    MockRouteResponse updateRoute(Long mockServerId, Long routeId, MockRouteRequest request, User owner);

    void deleteRoute(Long mockServerId, Long routeId, User owner);

    /** Public lookup used by the unauthenticated runtime endpoint — no
     * owner check, since anyone hitting a live mock URL is the point. */
    Optional<MockRoute> findMatchingRoute(Long mockServerId, HttpMethod method, String path);
}