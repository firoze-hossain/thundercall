package com.roze.thundercall.api.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roze.thundercall.api.dto.MockCallLogResponse;
import com.roze.thundercall.api.dto.MockRouteRequest;
import com.roze.thundercall.api.dto.MockRouteResponse;
import com.roze.thundercall.api.dto.MockServerRequest;
import com.roze.thundercall.api.dto.MockServerResponse;
import com.roze.thundercall.api.entity.*;
import com.roze.thundercall.api.enums.HttpMethod;
import com.roze.thundercall.api.exception.ResourceNotFoundException;
import com.roze.thundercall.api.repository.CollectionRepository;
import com.roze.thundercall.api.repository.MockCallLogRepository;
import com.roze.thundercall.api.repository.MockRouteRepository;
import com.roze.thundercall.api.repository.MockServerRepository;
import com.roze.thundercall.api.repository.RequestRepository;
import com.roze.thundercall.api.security.WorkspaceAccessGuard;
import com.roze.thundercall.api.service.MockServerService;
import com.roze.thundercall.api.util.MockPathMatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MockServerServiceImpl implements MockServerService {
    private final MockServerRepository mockServerRepository;
    private final MockRouteRepository mockRouteRepository;
    private final MockCallLogRepository mockCallLogRepository;
    private final RequestRepository requestRepository;
    private final CollectionRepository collectionRepository;
    private final WorkspaceAccessGuard workspaceAccessGuard;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
    public MockServerResponse updateMockServer(Long id, MockServerRequest request, User owner) {
        MockServer server = findOwned(id, owner);
        if (request.name() != null && !request.name().isBlank()) {
            server.setName(request.name().trim());
        }
        if (request.description() != null) {
            server.setDescription(request.description().trim());
        }
        return toResponse(mockServerRepository.save(server));
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
    public Optional<MatchedRoute> findMatchingRoute(Long mockServerId, HttpMethod method, String path) {
        Optional<MockServer> serverOpt = mockServerRepository.findById(mockServerId);
        if (serverOpt.isEmpty() || !serverOpt.get().isEnabled()) {
            return Optional.empty();
        }

        List<MockRoute> routes = mockRouteRepository.findByMockServerIdOrderByCreatedAtAsc(mockServerId);
        String cleanPath = MockPathMatcher.extractPathWithoutQuery(path);

        MockRoute bestRoute = null;
        MockPathMatcher.MatchResult bestMatch = null;

        for (MockRoute route : routes) {
            if (route.getMethod() != method) {
                continue;
            }
            MockPathMatcher.MatchResult matchResult = MockPathMatcher.match(route.getPath(), cleanPath);
            if (matchResult.matched()) {
                if (bestMatch == null || matchResult.specificity() > bestMatch.specificity()) {
                    bestMatch = matchResult;
                    bestRoute = route;
                }
            }
        }

        if (bestRoute != null && bestMatch != null) {
            return Optional.of(new MatchedRoute(bestRoute, bestMatch.pathVariables()));
        }

        return Optional.empty();
    }

    @Override
    @Transactional
    public void recordCallLog(Long mockServerId, HttpMethod method, String path, String queryParams,
                               String requestHeaders, String requestBody, int responseStatus,
                               String responseHeaders, String responseBody, long durationMs,
                               boolean matched, String matchedRoutePattern) {
        try {
            Optional<MockServer> serverOpt = mockServerRepository.findById(mockServerId);
            if (serverOpt.isEmpty()) return;

            MockCallLog callLog = MockCallLog.builder()
                    .mockServer(serverOpt.get())
                    .method(method)
                    .path(path)
                    .queryParams(queryParams)
                    .requestHeaders(requestHeaders)
                    .requestBody(requestBody)
                    .responseStatus(responseStatus)
                    .responseHeaders(responseHeaders)
                    .responseBody(responseBody)
                    .durationMs(durationMs)
                    .matched(matched)
                    .matchedRoutePattern(matchedRoutePattern)
                    .build();

            mockCallLogRepository.save(callLog);
        } catch (Exception e) {
            log.warn("Failed to record mock call log: {}", e.getMessage());
        }
    }

    @Override
    public List<MockCallLogResponse> getCallLogs(Long mockServerId, int limit, User owner) {
        findOwned(mockServerId, owner);
        int pageLimit = Math.max(1, Math.min(limit, 200));
        return mockCallLogRepository.findByMockServerIdOrderByTimestampDesc(mockServerId, PageRequest.of(0, pageLimit))
                .stream()
                .map(log -> new MockCallLogResponse(
                        log.getId(),
                        log.getMockServer().getId(),
                        log.getTimestamp(),
                        log.getMethod().name(),
                        log.getPath(),
                        log.getQueryParams(),
                        log.getRequestHeaders(),
                        log.getRequestBody(),
                        log.getResponseStatus(),
                        log.getResponseHeaders(),
                        log.getResponseBody(),
                        log.getDurationMs(),
                        log.isMatched(),
                        log.getMatchedRoutePattern()
                ))
                .toList();
    }

    @Override
    @Transactional
    public void clearCallLogs(Long mockServerId, User owner) {
        findOwned(mockServerId, owner);
        mockCallLogRepository.deleteByMockServerId(mockServerId);
    }

    @Override
    @Transactional
    public List<MockRouteResponse> generateRoutesFromCollection(Long mockServerId, Long collectionId, User owner) {
        MockServer server = findOwned(mockServerId, owner);
        com.roze.thundercall.api.entity.Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found"));
        workspaceAccessGuard.requireRead(collection.getWorkspace(), owner);

        List<Request> requests = requestRepository.findByCollectionId(collectionId);
        List<MockRoute> createdRoutes = new ArrayList<>();

        for (Request req : requests) {
            String routePath = extractRoutePathFromUrl(req.getUrl());
            if (routePath.isEmpty()) {
                routePath = "/" + req.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "-");
            }

            int status = 200;
            String defaultBody;
            HttpMethod method = req.getMethod() != null ? req.getMethod() : HttpMethod.GET;

            switch (method) {
                case POST -> {
                    status = 201;
                    defaultBody = "{\n  \"message\": \"" + escapeJson(req.getName()) + " created successfully\",\n  \"id\": \"{{$randomUUID}}\",\n  \"createdAt\": \"{{$isoTimestamp}}\"\n}";
                }
                case DELETE -> {
                    status = 200;
                    defaultBody = "{\n  \"message\": \"" + escapeJson(req.getName()) + " deleted successfully\"\n}";
                }
                case PUT, PATCH -> {
                    status = 200;
                    defaultBody = "{\n  \"message\": \"" + escapeJson(req.getName()) + " updated successfully\",\n  \"updatedAt\": \"{{$isoTimestamp}}\"\n}";
                }
                default -> {
                    status = 200;
                    defaultBody = "{\n  \"status\": \"success\",\n  \"message\": \"Mock response for " + escapeJson(req.getName()) + "\",\n  \"data\": {\n    \"id\": \"{{$randomUUID}}\",\n    \"name\": \"{{$randomFullName}}\",\n    \"email\": \"{{$randomEmail}}\"\n  }\n}";
                }
            }

            // If request had canned response or body, use it if appropriate
            if (req.getBody() != null && !req.getBody().isBlank() && (method == HttpMethod.POST || method == HttpMethod.PUT)) {
                defaultBody = "{\n  \"status\": \"success\",\n  \"received\": " + req.getBody().trim() + ",\n  \"id\": \"{{$randomUUID}}\"\n}";
            }

            MockRoute route = MockRoute.builder()
                    .mockServer(server)
                    .method(method)
                    .path(normalizePath(routePath))
                    .responseStatus(status)
                    .responseBody(defaultBody)
                    .responseHeaders("{\"Content-Type\": \"application/json\"}")
                    .delayMs(0)
                    .build();

            createdRoutes.add(mockRouteRepository.save(route));
        }

        return createdRoutes.stream().map(this::toResponse).toList();
    }

    private String extractRoutePathFromUrl(String url) {
        if (url == null || url.isBlank()) return "/";
        String s = url.trim();
        // Remove protocol and host if present
        if (s.contains("://")) {
            int slashIdx = s.indexOf('/', s.indexOf("://") + 3);
            s = slashIdx >= 0 ? s.substring(slashIdx) : "/";
        }
        // Remove template variables like {{BASE_URL}}
        s = s.replaceAll("^\\{\\{[^}]+\\}\\}", "");
        int qIdx = s.indexOf('?');
        if (qIdx >= 0) {
            s = s.substring(0, qIdx);
        }
        return normalizePath(s);
    }

    private String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
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
                route.getId(),
                route.getMockServer() != null ? route.getMockServer().getId() : null,
                route.getMethod(),
                route.getPath(),
                route.getResponseStatus(),
                route.getResponseBody(),
                route.getResponseHeaders(),
                route.getDelayMs(),
                route.getCreatedAt());
    }
}
