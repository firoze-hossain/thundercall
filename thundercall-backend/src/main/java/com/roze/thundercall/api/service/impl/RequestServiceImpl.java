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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Includes the earlier fixes (JSON header parsing, friendly connection
 * errors, updateRequest for Ctrl+S) plus BINARY RESPONSE SUPPORT: the
 * response is now fetched as raw bytes and classified as text or binary
 * from its Content-Type, so a generated PDF/Excel file downloads
 * byte-for-byte instead of being corrupted by forced String decoding.
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

    private static final Pattern FILENAME_PATTERN =
            Pattern.compile("filename\\*?=\"?([^\";]+)\"?", Pattern.CASE_INSENSITIVE);

    @Override
    @Transactional
    public ApiResponse executeRequest(ApiRequest apiRequest, User user) {
        Instant startTime = Instant.now();
        try {
            HttpHeaders headers = prepareHeaders(apiRequest.headers(), apiRequest.body());
            HttpEntity<String> entity = new HttpEntity<>(emptyToNull(apiRequest.body()), headers);

            // Fetch as raw bytes — safe for text AND binary payloads alike.
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    apiRequest.url(),
                    toSpringMethod(apiRequest.method()),
                    entity,
                    byte[].class);

            long duration = Duration.between(startTime, Instant.now()).toMillis();
            boolean success = !response.getStatusCode().isError();
            byte[] body = response.getBody() != null ? response.getBody() : new byte[0];
            String contentType = response.getHeaders().getContentType() != null
                    ? response.getHeaders().getContentType().toString() : null;
            boolean binary = isBinary(contentType);

            ApiResponse apiResponse = ApiResponse.builder()
                    .statusCode(response.getStatusCode().value())
                    .response(binary ? Base64.getEncoder().encodeToString(body) : decodeText(body, contentType))
                    .responseHeaders(convertHeadersToString(response.getHeaders()))
                    .duration(duration)
                    .success(success)
                    .binary(binary)
                    .contentType(contentType)
                    .fileName(guessFileName(response.getHeaders(), apiRequest.url(), contentType))
                    .sizeBytes(body.length)
                    .build();

            saveRequestHistory(apiRequest, apiResponse, user);
            return apiResponse;
        } catch (ResourceAccessException e) {
            long duration = Duration.between(startTime, Instant.now()).toMillis();
            ApiResponse apiResponse = ApiResponse.builder()
                    .statusCode(0)
                    .response("Could not connect: " + rootMessage(e))
                    .duration(duration)
                    .success(false)
                    .build();
            saveRequestHistory(apiRequest, apiResponse, user);
            return apiResponse;
        } catch (Exception e) {
            long duration = Duration.between(startTime, Instant.now()).toMillis();
            log.warn("Request execution failed: {}", e.getMessage());
            ApiResponse apiResponse = ApiResponse.builder()
                    .statusCode(0)
                    .response("Request failed: " + rootMessage(e))
                    .duration(duration)
                    .success(false)
                    .build();
            saveRequestHistory(apiRequest, apiResponse, user);
            return apiResponse;
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
        request.setPreRequestScript(apiRequest.preRequestScript());
        request.setTestsScript(apiRequest.testsScript());
        request.setAuthType(apiRequest.authType());
        request.setAuthToken(apiRequest.authToken());
        request.setAuthUsername(apiRequest.authUsername());
        request.setAuthPassword(apiRequest.authPassword());
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

    /** Anything that isn't recognizably text is treated as a binary download. */
    private boolean isBinary(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false; // most simple REST APIs omit it; assume text (JSON/plain)
        }
        String ct = contentType.toLowerCase(Locale.ROOT);
        if (ct.contains("json") || ct.contains("xml") || ct.contains("text")
                || ct.contains("javascript") || ct.contains("html") || ct.contains("csv")
                || ct.contains("yaml") || ct.contains("x-www-form-urlencoded")) {
            return false;
        }
        return ct.contains("pdf") || ct.contains("excel") || ct.contains("spreadsheet")
                || ct.contains("octet-stream") || ct.contains("zip") || ct.contains("image")
                || ct.contains("audio") || ct.contains("video") || ct.contains("msword")
                || ct.contains("officedocument");
    }

    private String decodeText(byte[] body, String contentType) {
        Charset charset = StandardCharsets.UTF_8;
        if (contentType != null) {
            try {
                MediaType mt = MediaType.parseMediaType(contentType);
                if (mt.getCharset() != null) {
                    charset = mt.getCharset();
                }
            } catch (Exception ignored) {
            }
        }
        return new String(body, charset);
    }

    private String guessFileName(HttpHeaders headers, String url, String contentType) {
        String disposition = headers.getFirst(HttpHeaders.CONTENT_DISPOSITION);
        if (disposition != null) {
            Matcher m = FILENAME_PATTERN.matcher(disposition);
            if (m.find()) {
                return m.group(1).replace("UTF-8''", "");
            }
        }
        String lastSegment = url;
        int q = lastSegment.indexOf('?');
        if (q >= 0) {
            lastSegment = lastSegment.substring(0, q);
        }
        int slash = lastSegment.lastIndexOf('/');
        if (slash >= 0 && slash < lastSegment.length() - 1) {
            String candidate = lastSegment.substring(slash + 1);
            if (candidate.contains(".")) {
                return candidate;
            }
        }
        return "response" + guessExtension(contentType);
    }

    private String guessExtension(String contentType) {
        if (contentType == null) {
            return ".bin";
        }
        String ct = contentType.toLowerCase(Locale.ROOT);
        if (ct.contains("pdf")) return ".pdf";
        if (ct.contains("spreadsheet") || ct.contains("excel")) return ".xlsx";
        if (ct.contains("msword") || ct.contains("wordprocessingml")) return ".docx";
        if (ct.contains("zip")) return ".zip";
        if (ct.contains("png")) return ".png";
        if (ct.contains("jpeg") || ct.contains("jpg")) return ".jpg";
        if (ct.contains("csv")) return ".csv";
        return ".bin";
    }

    private void saveRequestHistory(ApiRequest apiRequest, ApiResponse apiResponse, User user) {
        try {
            RequestHistory history = RequestHistory.builder()
                    .timestamp(LocalDateTime.now())
                    .statusCode(apiResponse.getStatusCode())
                    .duration(apiResponse.getDuration())
                    .response(apiResponse.isBinary()
                            ? "[binary response: " + apiResponse.getContentType()
                                    + ", " + apiResponse.getSizeBytes() + " bytes — not stored]"
                            : apiResponse.getResponse())
                    .responseHeaders(apiResponse.getResponseHeaders() != null ? apiResponse.getResponseHeaders() : "")
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