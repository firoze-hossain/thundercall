package com.roze.thundercall.service.impl;

import com.roze.thundercall.dto.ApiRequest;
import com.roze.thundercall.dto.ApiResponse;
import com.roze.thundercall.dto.RequestResponse;
import com.roze.thundercall.entity.*;
import com.roze.thundercall.enums.HttpMethod;
import com.roze.thundercall.exception.ResourceNotFoundException;
import com.roze.thundercall.mapper.RequestMapper;
import com.roze.thundercall.repository.CollectionRepository;
import com.roze.thundercall.repository.FolderRepository;
import com.roze.thundercall.repository.RequestHistoryRepository;
import com.roze.thundercall.repository.RequestRepository;
import com.roze.thundercall.service.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {
    private final RestTemplate restTemplate;
    private final RequestRepository requestRepository;
    private final RequestHistoryRepository requestHistoryRepository;
    private final CollectionRepository collectionRepository;
    private final RequestMapper requestMapper;
    private final FolderRepository folderRepository;

    @Override
    @Transactional
    public ApiResponse executeRequest(ApiRequest apiRequest, User user) {
        Instant startTime = Instant.now();
        try {
            HttpMethod method = apiRequest.method();
            String url = apiRequest.url();
            HttpHeaders headers = prepareHeaders(apiRequest.headers());
            String body = apiRequest.body();
            ResponseEntity<String> response = executeHttpRequest(method, url, headers, body);
            Instant endTime = Instant.now();
            long duration = Duration.between(startTime, endTime).toMillis();
            saveRequestHistory(apiRequest, response, duration, user, true);
            return ApiResponse.builder()
                    .statusCode(response.getStatusCode().value())
                    .response(response.getBody())
                    .responseHeaders(convertHeadersToString(response.getHeaders()))
                    .duration(duration)
                    .success(true)
                    .build();
        } catch (Exception e) {
            Instant endTime = Instant.now();
            long duration = Duration.between(startTime, endTime).toMillis();
            saveRequestHistory(apiRequest, null, duration, user, false);
            return handleException(e, duration);
        }
    }

    @Override
    @Transactional
    public RequestResponse saveRequestToCollection(ApiRequest apiRequest, User user) {
        Collection collection = collectionRepository.findByIdAndWorkspaceOwner(apiRequest.collectionId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found"));
        // Validate folder ownership if folderId is provided
        if (apiRequest.folderId() != null) {
            Folder folder = folderRepository.findByIdAndCollectionWorkspaceOwner(apiRequest.folderId(), user)
                    .orElseThrow(() -> new ResourceNotFoundException("Folder not found or you don't have access"));

            // Verify folder belongs to the same collection
            if (!folder.getCollection().getId().equals(collection.getId())) {
                throw new IllegalArgumentException("Folder does not belong to the specified collection");
            }
        }
        Request request = requestMapper.toEntity(apiRequest);
        request.setCollection(collection);
        Request savedRequest = requestRepository.save(request);
        return requestMapper.toResponse(savedRequest);
    }

    @Override
    public RequestResponse getRequestById(Long id, User user) {
        Request request = requestRepository.findByIdAndCollectionWorkspaceOwner(id, user).orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        return requestMapper.toResponse(request);
    }

    @Override
    @Transactional
    public void deleteRequest(Long id, User user) {
        Request request = requestRepository.findByIdAndCollectionWorkspaceOwner(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        requestRepository.delete(request);
    }

    private ResponseEntity<String> executeHttpRequest(HttpMethod method, String url, HttpHeaders headers, String body) {
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        switch (method) {
            case GET:
                return restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, String.class);
            case POST:
                return restTemplate.exchange(url, org.springframework.http.HttpMethod.POST, entity, String.class);
            case PUT:
                return restTemplate.exchange(url, org.springframework.http.HttpMethod.PUT, entity, String.class);
            case DELETE:
                return restTemplate.exchange(url, org.springframework.http.HttpMethod.DELETE, entity, String.class);
            case PATCH:
                return restTemplate.exchange(url, org.springframework.http.HttpMethod.PATCH, entity, String.class);
            case HEAD:
                return restTemplate.exchange(url, org.springframework.http.HttpMethod.HEAD, entity, String.class);
            case OPTIONS:
                return restTemplate.exchange(url, org.springframework.http.HttpMethod.OPTIONS, entity, String.class);
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
    }

    private HttpHeaders prepareHeaders(String headersString) {
        HttpHeaders headers = new HttpHeaders();
        if (headersString != null && !headersString.trim().isEmpty()) {
            try {
                headersString = headersString.replaceAll("[{}\"]", "");
                String[] headerPairs = headersString.split(",");
                for (String pair : headerPairs) {
                    String[] keyValue = pair.split(":", 2);
                    if (keyValue.length == 2) {
                        headers.add(keyValue[0].toLowerCase(Locale.ROOT), keyValue[1].trim());
                    }
                }
            } catch (Exception e) {
                headers.setContentType(MediaType.APPLICATION_JSON);
            }
        }
        if (headers.getContentType() == null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return headers;
    }

    private void saveRequestHistory(ApiRequest apiRequest, ResponseEntity<String> response, long duration, User user, boolean success) {
        RequestHistory history = RequestHistory.builder()
                .timestamp(LocalDateTime.now())
                .statusCode(response != null ? response.getStatusCode().value() : 0)
                .duration(duration)
                .response(response != null ? response.getBody() : "Request failed")
                .responseHeaders(response != null ? convertHeadersToString(response.getHeaders()) : "")
                .build();
        if (apiRequest.name() != null && !apiRequest.name().isEmpty()) {
            Request request = requestRepository.findByNameAndCollectionWorkspaceOwner(apiRequest.name(), user).orElse(null);
            if (request != null) {
                history.setRequest(request);
            }
        }
        requestHistoryRepository.save(history);
    }

    private String convertHeadersToString(HttpHeaders headers) {
        StringBuilder sb = new StringBuilder();
        headers.forEach((key, values) -> {
            values.forEach(value -> {
                sb.append(key).append(": ").append(value).append("\n");
            });
        });
        return sb.toString();
    }

    private ApiResponse handleException(Exception e, long duration) {
        int statusCode = 0;
        String errorMessage = e.getMessage();
        if (e instanceof HttpClientErrorException) {
            statusCode = ((HttpClientErrorException) e).getStatusCode().value();
            errorMessage = ((HttpClientErrorException) e).getResponseBodyAsString();
        } else if (e instanceof HttpServerErrorException) {
            statusCode = ((HttpServerErrorException) e).getStatusCode().value();
            errorMessage = ((HttpServerErrorException) e).getResponseBodyAsString();
        } else if (e instanceof ResourceAccessException) {
            statusCode = 503;
            errorMessage = "Connection failed: " + e.getMessage();
        }
        return ApiResponse.builder()

                .statusCode(statusCode)
                .response(errorMessage)
                .duration(duration)
                .success(false)
                .build();
    }
}
