package com.roze.thundercall.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roze.thundercall.api.entity.MockRoute;
import com.roze.thundercall.api.enums.HttpMethod;
import com.roze.thundercall.api.service.MockServerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/** The actual "live" side of a mock server — this is what an external
 * tool, curl, or another app hits, not something Thundercall itself
 * calls. Deliberately public (see WebSecurityConfig): a mock server is
 * only useful if anyone can hit it without a Thundercall login. */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/mock")
public class MockRuntimeController {
    private final MockServerService mockServerService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @RequestMapping(value = "/{mockServerId}/**",
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
                    RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
    public ResponseEntity<byte[]> handleMockRequest(@PathVariable Long mockServerId, HttpServletRequest request) {
        String routePath = extractRoutePath(mockServerId, request);
        HttpMethod method;
        try {
            method = HttpMethod.valueOf(request.getMethod());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }

        Optional<MockRoute> matched = mockServerService.findMatchingRoute(mockServerId, method, routePath);
        if (matched.isEmpty()) {
            String message = "No mock route configured for " + method + " " + routePath;
            log.info(message + " (mock server {})", mockServerId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(message.getBytes(StandardCharsets.UTF_8));
        }

        MockRoute route = matched.get();
        if (route.getDelayMs() > 0) {
            try {
                Thread.sleep(route.getDelayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        HttpHeaders headers = new HttpHeaders();
        if (route.getResponseHeaders() != null && !route.getResponseHeaders().isBlank()) {
            try {
                Map<String, String> map = objectMapper.readValue(
                        route.getResponseHeaders(), new TypeReference<Map<String, String>>() {
                        });
                map.forEach((k, v) -> {
                    if (k != null && !k.isBlank() && v != null) {
                        headers.add(k.trim(), v.trim());
                    }
                });
            } catch (Exception ignored) {
                // malformed headers JSON — serve the response anyway, just without those headers
            }
        }
        if (headers.getContentType() == null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }

        byte[] body = route.getResponseBody() != null
                ? route.getResponseBody().getBytes(StandardCharsets.UTF_8) : new byte[0];
        return ResponseEntity.status(route.getResponseStatus()).headers(headers).body(body);
    }

    /** Everything after "/mock/{mockServerId}" — e.g. a request to
     * ".../mock/5/users/1" yields "/users/1". Uses Spring's own
     * path-within-handler attribute rather than manually parsing
     * getRequestURI(), so this works correctly regardless of the app's
     * context-path. */
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