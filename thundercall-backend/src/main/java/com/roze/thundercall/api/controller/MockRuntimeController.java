package com.roze.thundercall.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roze.thundercall.api.entity.MockRoute;
import com.roze.thundercall.api.enums.HttpMethod;
import com.roze.thundercall.api.service.MockServerService;
import com.roze.thundercall.api.util.MockDynamicVariableResolver;
import com.roze.thundercall.api.util.MockPathMatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/mock")
public class MockRuntimeController {
    private final MockServerService mockServerService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @RequestMapping(value = "/{mockServerId}/**",
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
                    RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS, RequestMethod.HEAD})
    public ResponseEntity<byte[]> handleMockRequest(@PathVariable Long mockServerId, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();

        // Handle CORS Preflight OPTIONS request
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            HttpHeaders corsHeaders = buildCorsHeaders();
            return ResponseEntity.ok().headers(corsHeaders).build();
        }

        String rawRoutePath = extractRoutePath(mockServerId, request);
        String cleanPath = MockPathMatcher.extractPathWithoutQuery(rawRoutePath);
        String queryParams = request.getQueryString() != null ? request.getQueryString() : "";

        HttpMethod method;
        try {
            method = HttpMethod.valueOf(request.getMethod());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }

        // Read request headers
        Map<String, String> reqHeadersMap = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String hn = headerNames.nextElement();
                reqHeadersMap.put(hn, request.getHeader(hn));
            }
        }
        String reqHeadersJson = "";
        try {
            reqHeadersJson = objectMapper.writeValueAsString(reqHeadersMap);
        } catch (Exception ignored) {}

        // Read request body
        String reqBody = "";
        try (InputStream is = request.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            if (bytes.length > 0) {
                reqBody = new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}

        Optional<MockServerService.MatchedRoute> matchedOpt = mockServerService.findMatchingRoute(mockServerId, method, cleanPath);

        if (matchedOpt.isEmpty()) {
            long duration = System.currentTimeMillis() - startTime;
            String errorJson = "{\n  \"error\": {\n    \"status\": 404,\n    \"message\": \"No mock route configured for "
                    + method + " " + cleanPath + "\",\n    \"mockServerId\": " + mockServerId
                    + ",\n    \"requestedMethod\": \"" + method + "\",\n    \"requestedPath\": \"" + cleanPath + "\"\n  }\n}";

            mockServerService.recordCallLog(mockServerId, method, cleanPath, queryParams,
                    reqHeadersJson, reqBody, 404,
                    "{\"Content-Type\": \"application/json\"}", errorJson, duration, false, null);

            HttpHeaders headers = buildCorsHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .headers(headers)
                    .body(errorJson.getBytes(StandardCharsets.UTF_8));
        }

        MockRoute route = matchedOpt.get().route();
        Map<String, String> pathVariables = matchedOpt.get().pathVariables();

        if (route.getDelayMs() > 0) {
            try {
                Thread.sleep(route.getDelayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        // Resolve dynamic tokens (e.g. {{$randomUUID}}, {{$randomEmail}}) and path variables
        String resolvedBody = MockDynamicVariableResolver.resolve(route.getResponseBody(), pathVariables);

        HttpHeaders headers = buildCorsHeaders();
        if (route.getResponseHeaders() != null && !route.getResponseHeaders().isBlank()) {
            try {
                Map<String, String> map = objectMapper.readValue(
                        route.getResponseHeaders(), new TypeReference<Map<String, String>>() {});
                map.forEach((k, v) -> {
                    if (k != null && !k.isBlank() && v != null) {
                        headers.set(k.trim(), v.trim());
                    }
                });
            } catch (Exception ignored) {}
        }
        if (headers.getContentType() == null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }

        byte[] bodyBytes = resolvedBody != null ? resolvedBody.getBytes(StandardCharsets.UTF_8) : new byte[0];

        // Record successful call in live log
        mockServerService.recordCallLog(mockServerId, method, cleanPath, queryParams,
                reqHeadersJson, reqBody, route.getResponseStatus(),
                route.getResponseHeaders(), resolvedBody, duration, true, route.getPath());

        return ResponseEntity.status(route.getResponseStatus()).headers(headers).body(bodyBytes);
    }

    private HttpHeaders buildCorsHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD");
        headers.set("Access-Control-Allow-Headers", "*");
        headers.set("Access-Control-Max-Age", "86400");
        return headers;
    }

    private String extractRoutePath(Long mockServerId, HttpServletRequest request) {
        String fullPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        if (fullPath == null) {
            return "/";
        }
        String prefix = "/" + mockServerId;
        String routePath = fullPath.startsWith(prefix) ? fullPath.substring(prefix.length()) : fullPath;
        return routePath.isEmpty() ? "/" : routePath;
    }
}
