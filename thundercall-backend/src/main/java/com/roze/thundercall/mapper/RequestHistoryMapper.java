package com.roze.thundercall.mapper;

import com.roze.thundercall.dto.RequestHistoryResponse;
import com.roze.thundercall.entity.RequestHistory;
import org.springframework.stereotype.Component;

@Component
public class RequestHistoryMapper {

    public RequestHistoryResponse toResponse(RequestHistory history) {
        return RequestHistoryResponse.builder()
                .id(history.getId())
                .requestId(history.getRequest() != null ? history.getRequest().getId() : null)
                .requestName(history.getRequest() != null ? history.getRequest().getName() : null)
                .timestamp(history.getTimestamp())
                .statusCode(history.getStatusCode())
                .duration(history.getDuration())
                .response(history.getResponse())
                .responseHeaders(history.getResponseHeaders())
                .success(history.getStatusCode() != null && history.getStatusCode() >= 200 && history.getStatusCode() < 300)
                .build();
    }
}
