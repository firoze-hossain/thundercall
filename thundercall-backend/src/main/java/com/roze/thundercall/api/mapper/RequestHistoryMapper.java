package com.roze.thundercall.api.mapper;

import com.roze.thundercall.api.dto.RequestHistoryResponse;
import com.roze.thundercall.api.entity.RequestHistory;
import org.springframework.stereotype.Component;

@Component
public class RequestHistoryMapper {

    public RequestHistoryResponse toResponse(RequestHistory history) {
        String name = history.getRequestName() != null ? history.getRequestName()
                : (history.getRequest() != null ? history.getRequest().getName() : null);
        String url = history.getUrl() != null ? history.getUrl()
                : (history.getRequest() != null ? history.getRequest().getUrl() : null);
        String method = history.getMethod() != null ? history.getMethod()
                : (history.getRequest() != null && history.getRequest().getMethod() != null
                        ? history.getRequest().getMethod().name() : "GET");

        return RequestHistoryResponse.builder()
                .id(history.getId())
                .requestId(history.getRequest() != null ? history.getRequest().getId() : null)
                .requestName(name)
                .url(url)
                .method(method)
                .requestHeaders(history.getRequestHeaders())
                .requestBody(history.getRequestBody())
                .timestamp(history.getTimestamp())
                .statusCode(history.getStatusCode())
                .duration(history.getDuration())
                .response(history.getResponse())
                .responseHeaders(history.getResponseHeaders())
                .success(history.getStatusCode() != null && history.getStatusCode() >= 200 && history.getStatusCode() < 300)
                .build();
    }
}
