package com.roze.thundercall.api.service.impl;

import com.roze.thundercall.api.dto.MockRouteRequest;
import com.roze.thundercall.api.dto.MockRouteResponse;
import com.roze.thundercall.api.dto.MockServerRequest;
import com.roze.thundercall.api.dto.MockServerResponse;
import com.roze.thundercall.api.entity.MockRoute;
import com.roze.thundercall.api.entity.MockServer;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.enums.HttpMethod;
import com.roze.thundercall.api.exception.ResourceNotFoundException;
import com.roze.thundercall.api.repository.MockRouteRepository;
import com.roze.thundercall.api.repository.MockServerRepository;
import com.roze.thundercall.api.service.MockServerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MockServerServiceImpl implements MockServerService {
    private final MockServerRepository mockServerRepository;
    private final MockRouteRepository mockRouteRepository;

    @Value("${server.port}")
    private String serverPort;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Override
    @Transactional
    public MockServerResponse createMockServer(MockServerRequest request, User owner) {
        MockServer server = MockServer.builder()
                .name(request.name())
                .description(request.description())
                .owner(owner)
                .enabled(true)
                .build();
        return toResponse(mockServerRepository.save(server));
    }

    @Override
    public List<MockServerResponse> getMyMockServers(User owner) {
        return mockServerRepository.findByOwnerOrderByCreatedAtDesc(owner).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public MockServerResponse getMockServer(Long id, User owner) {
        return toResponse(findOwned(id, owner));
    }

    @Override
    @Transactional
    public void setEnabled(Long id, boolean enabled, User owner) {
        MockServer server = findOwned(id, owner);
        server.setEnabled(enabled);
        mockServerRepository.save(server);
    }

    @Override
    @Transactional
    public void deleteMockServer(Long id, User owner) {
        MockServer server = findOwned(id, owner);
        mockServerRepository.delete(server);
    }

    @Override
    @Transactional
    public MockRouteResponse addRoute(Long mockServerId, MockRouteRequest request, User owner) {
        MockServer server = findOwned(mockServerId, owner);
        MockRoute route = MockRoute.builder()
                .mockServer(server)
                .method(request.method())
                .path(normalizePath(request.path()))
                .responseStatus(request.responseStatus())
                .responseBody(request.responseBody())
                .responseHeaders(request.responseHeaders())
                .delayMs(request.delayMs() != null ? request.delayMs() : 0)
                .build();
        return toResponse(mockRouteRepository.save(route));
    }

    @Override
    public List<MockRouteResponse> getRoutes(Long mockServerId, User owner) {
        findOwned(mockServerId, owner); // ownership check
        return mockRouteRepository.findByMockServerIdOrderByCreatedAtAsc(mockServerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public MockRouteResponse updateRoute(Long mockServerId, Long routeId, MockRouteRequest request, User owner) {
        findOwned(mockServerId, owner); // ownership check
        MockRoute route = mockRouteRepository.findByIdAndMockServerId(routeId, mockServerId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));
        route.setMethod(request.method());
        route.setPath(normalizePath(request.path()));
        route.setResponseStatus(request.responseStatus());
        route.setResponseBody(request.responseBody());
        route.setResponseHeaders(request.responseHeaders());
        route.setDelayMs(request.delayMs() != null ? request.delayMs() : 0);
        return toResponse(mockRouteRepository.save(route));
    }

    @Override
    @Transactional
    public void deleteRoute(Long mockServerId, Long routeId, User owner) {
        findOwned(mockServerId, owner); // ownership check
        MockRoute route = mockRouteRepository.findByIdAndMockServerId(routeId, mockServerId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));
        mockRouteRepository.delete(route);
    }

    @Override
    public Optional<MockRoute> findMatchingRoute(Long mockServerId, HttpMethod method, String path) {
        Optional<MockServer> server = mockServerRepository.findById(mockServerId);
        if (server.isEmpty() || !server.get().isEnabled()) {
            return Optional.empty();
        }
        return mockRouteRepository.findFirstByMockServerIdAndMethodAndPath(mockServerId, method, normalizePath(path));
    }

    private MockServer findOwned(Long id, User owner) {
        return mockServerRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Mock server not found"));
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String trimmed = path.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    private MockServerResponse toResponse(MockServer server) {
        String baseUrl = "http://localhost:" + serverPort + contextPath + "/mock/" + server.getId();
        long routeCount = server.getRoutes() != null ? server.getRoutes().size() : 0;
        return new MockServerResponse(
                server.getId(), server.getName(), server.getDescription(), server.isEnabled(),
                baseUrl, routeCount, server.getCreatedAt());
    }

    private MockRouteResponse toResponse(MockRoute route) {
        return new MockRouteResponse(
                route.getId(), route.getMockServer().getId(), route.getMethod(), route.getPath(),
                route.getResponseStatus(), route.getResponseBody(), route.getResponseHeaders(),
                route.getDelayMs(), route.getCreatedAt());
    }
}