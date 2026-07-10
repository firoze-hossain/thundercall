package com.roze.thundercall.api.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roze.thundercall.api.dto.ApiRequest;
import com.roze.thundercall.api.dto.ApiResponse;
import com.roze.thundercall.api.dto.RequestResponse;
import com.roze.thundercall.api.entity.*;
import com.roze.thundercall.api.enums.HttpMethod;
import com.roze.thundercall.api.exception.ResourceNotFoundException;
import com.roze.thundercall.api.mapper.RequestMapper;
import com.roze.thundercall.api.repository.CollectionRepository;
import com.roze.thundercall.api.repository.FolderRepository;
import com.roze.thundercall.api.repository.RequestHistoryRepository;
import com.roze.thundercall.api.repository.RequestRepository;
import com.roze.thundercall.api.service.RequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Includes the earlier fixes (JSON header parsing instead of the broken
 * regex; friendly connection errors) plus the NEW updateRequest used by
 * Ctrl+S / the Save button to save edits back into an existing request.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RequestServiceImpl implements RequestService {
    private final RestTemplate restTemplate;
    private final RequestRepository requestRepository;
    private final RequestHistoryRepository requestHistoryRepository;
    private final CollectionRepository collectionRepository;
    private final RequestMapper requestMapper;
    private final FolderRepository folderRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public ApiResponse executeRequest(ApiRequest apiRequest, User user) {
        Instant startTime = Instant.now();
        try {
            HttpHeaders headers = prepareHeaders(apiRequest.headers(), apiRequest.body());
            HttpEntity<String> entity = new HttpEntity<>(emptyToNull(apiRequest.body()), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiRequest.url(),
                    toSpringMethod(apiRequest.method()),
                    entity,
                    String.class);

            long duration = Duration.between(startTime, Instant.now()).toMillis();
            boolean success = !response.getStatusCode().isError();
            saveRequestHistory(apiRequest, response, duration, user, success);

            return ApiResponse.builder()
                    .statusCode(response.getStatusCode().value())
                    .response(response.getBody() != null ? response.getBody() : "")
                    .responseHeaders(convertHeadersToString(response.getHeaders()))
                    .duration(duration)
                    .success(success)
                    .build();
        } catch (ResourceAccessException e) {
            long duration = Duration.between(startTime, Instant.now()).toMillis();
            saveRequestHistory(apiRequest, null, duration, user, false);
            return ApiResponse.builder()
                    .statusCode(0)
                    .response("Could not connect: " + rootMessage(e))
                    .duration(duration)
                    .success(false)
                    .build();
        } catch (Exception e) {
            long duration = Duration.between(startTime, Instant.now()).toMillis();
            log.warn("Request execution failed: {}", e.getMessage());
            saveRequestHistory(apiRequest, null, duration, user, false);
            return ApiResponse.builder()
                    .statusCode(0)
                    .response("Request failed: " + rootMessage(e))
                    .duration(duration)
                    .success(false)
                    .build();
        }
    }

    @Override
    @Transactional
    public RequestResponse saveRequestToCollection(ApiRequest apiRequest, User user) {
        Collection collection = collectionRepository.findByIdAndWorkspaceOwner(apiRequest.collectionId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found"));
        Folder folder = null;
        if (apiRequest.folderId() != null) {
            folder = folderRepository.findByIdAndCollectionWorkspaceOwner(apiRequest.folderId(), user)
                    .orElseThrow(() -> new ResourceNotFoundException("Folder not found or you don't have access"));
            if (!folder.getCollection().getId().equals(collection.getId())) {
                throw new IllegalArgumentException("Folder does not belong to the specified collection");
            }
        }
        Request request = requestMapper.toEntity(apiRequest);
        request.setCollection(collection);
        if (folder != null) {
            request.setFolder(folder);
        }
        Request savedRequest = requestRepository.save(request);
        return requestMapper.toResponse(savedRequest);
    }

    @Override
    public RequestResponse getRequestById(Long id, User user) {
        Request request = requestRepository.findByIdAndCollectionWorkspaceOwner(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        return requestMapper.toResponse(request);
    }

    /**
     * NEW: saves the current editor state back into an existing request —
     * used by the Save button / Ctrl+S when a saved request tab is active.
     * The request stays in its collection/folder.
     */
    @Override
    @Transactional
    public RequestResponse updateRequest(Long id, ApiRequest apiRequest, User user) {
        Request request = requestRepository.findByIdAndCollectionWorkspaceOwner(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        if (apiRequest.name() != null && !apiRequest.name().isBlank()) {
            request.setName(apiRequest.name());
        }
        request.setMethod(apiRequest.method());
        request.setUrl(apiRequest.url());
        request.setHeaders(apiRequest.headers());
        request.setBody(apiRequest.body());
        Request saved = requestRepository.save(request);
        return requestMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteRequest(Long id, User user) {
        Request request = requestRepository.findByIdAndCollectionWorkspaceOwner(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        requestRepository.delete(request);
    }

    // ------------------------------------------------------------------

    private org.springframework.http.HttpMethod toSpringMethod(HttpMethod method) {
        return org.springframework.http.HttpMethod.valueOf(method.name());
    }

    private HttpHeaders prepareHeaders(String headersString, String body) {
        HttpHeaders headers = new HttpHeaders();
        if (headersString != null && !headersString.isBlank()) {
            try {
                Map<String, String> map = objectMapper.readValue(
                        headersString, new TypeReference<Map<String, String>>() {});
                map.forEach((k, v) -> {
                    if (k != null && !k.isBlank() && v != null) {
                        headers.add(k.trim(), v.trim());
                    }
                });
            } catch (Exception notJson) {
                for (String line : headersString.split("\\r?\\n")) {
                    int idx = line.indexOf(':');
                    if (idx > 0) {
                        headers.add(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
                    }
                }
            }
        }
        if (headers.getContentType() == null && body != null && !body.isBlank()) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return headers;
    }

    private void saveRequestHistory(ApiRequest apiRequest, ResponseEntity<String> response,
                                    long duration, User user, boolean success) {
        try {
            RequestHistory history = RequestHistory.builder()
                    .timestamp(LocalDateTime.now())
                    .statusCode(response != null ? response.getStatusCode().value() : 0)
                    .duration(duration)
                    .response(response != null ? response.getBody() : "Request failed")
                    .responseHeaders(response != null ? convertHeadersToString(response.getHeaders()) : "")
                    .build();
            if (apiRequest.name() != null && !apiRequest.name().isEmpty()) {
                requestRepository.findByNameAndCollectionWorkspaceOwner(apiRequest.name(), user)
                        .ifPresent(history::setRequest);
            }
            requestHistoryRepository.save(history);
        } catch (Exception e) {
            log.warn("Failed to save request history: {}", e.getMessage());
        }
    }

    private String convertHeadersToString(HttpHeaders headers) {
        StringBuilder sb = new StringBuilder();
        headers.forEach((key, values) ->
                values.forEach(value -> sb.append(key).append(": ").append(value).append("\n")));
        return sb.toString();
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        return cur.getMessage() != null ? cur.getMessage() : cur.getClass().getSimpleName();
    }
}