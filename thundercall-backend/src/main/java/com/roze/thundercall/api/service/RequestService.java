package com.roze.thundercall.api.service;

import com.roze.thundercall.api.dto.ApiRequest;
import com.roze.thundercall.api.dto.ApiResponse;
import com.roze.thundercall.api.dto.RequestResponse;
import com.roze.thundercall.api.entity.User;

public interface RequestService {
    ApiResponse executeRequest(ApiRequest apiRequest, User user);

    /** Records a request/response pair to history WITHOUT executing
     * anything — used when the actual HTTP call already happened on
     * the client (see ClientHttpExecutor), so the backend's only job
     * here is the same history bookkeeping executeRequest() already
     * does internally. */
    void recordHistory(ApiRequest apiRequest, ApiResponse apiResponse, User user);

    RequestResponse saveRequestToCollection(ApiRequest apiRequest, User user);

    RequestResponse getRequestById(Long id, User user);

    RequestResponse updateRequest(Long id, ApiRequest apiRequest, User user);

    void deleteRequest(Long id, User user);
}