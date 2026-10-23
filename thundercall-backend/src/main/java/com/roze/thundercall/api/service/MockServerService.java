package com.roze.thundercall.api.service;

import com.roze.thundercall.api.dto.MockCallLogResponse;
import com.roze.thundercall.api.dto.MockRouteRequest;
import com.roze.thundercall.api.dto.MockRouteResponse;
import com.roze.thundercall.api.dto.MockServerRequest;
import com.roze.thundercall.api.dto.MockServerResponse;
import com.roze.thundercall.api.entity.MockRoute;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.enums.HttpMethod;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MockServerService {
    MockServerResponse createMockServer(MockServerRequest request, User owner);

    List<MockServerResponse> getMyMockServers(User owner);

    MockServerResponse getMockServer(Long id, User owner);

    MockServerResponse updateMockServer(Long id, MockServerRequest request, User owner);

    void setEnabled(Long id, boolean enabled, User owner);

    void deleteMockServer(Long id, User owner);

    MockRouteResponse addRoute(Long mockServerId, MockRouteRequest request, User owner);

    List<MockRouteResponse> getRoutes(Long mockServerId, User owner);

    MockRouteResponse updateRoute(Long mockServerId, Long routeId, MockRouteRequest request, User owner);

    void deleteRoute(Long mockServerId, Long routeId, User owner);

    record MatchedRoute(MockRoute route, Map<String, String> pathVariables) {}

    /** Public lookup used by the runtime endpoint — matches exact, params (:id, {id}), and wildcards */
    Optional<MatchedRoute> findMatchingRoute(Long mockServerId, HttpMethod method, String path);

    /** Records an incoming mock server hit into the live call log */
    void recordCallLog(Long mockServerId, HttpMethod method, String path, String queryParams,
                       String requestHeaders, String requestBody, int responseStatus,
                       String responseHeaders, String responseBody, long durationMs,
                       boolean matched, String matchedRoutePattern);

    List<MockCallLogResponse> getCallLogs(Long mockServerId, int limit, User owner);

    void clearCallLogs(Long mockServerId, User owner);

    List<MockRouteResponse> generateRoutesFromCollection(Long mockServerId, Long collectionId, User owner);
}