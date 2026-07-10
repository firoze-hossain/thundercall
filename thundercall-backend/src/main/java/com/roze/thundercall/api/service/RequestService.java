package com.roze.thundercall.api.service;

import com.roze.thundercall.api.dto.ApiRequest;
import com.roze.thundercall.api.dto.ApiResponse;
import com.roze.thundercall.api.dto.RequestResponse;
import com.roze.thundercall.api.entity.User;

public interface RequestService {
    ApiResponse executeRequest(ApiRequest apiRequest, User user);

    RequestResponse saveRequestToCollection(ApiRequest apiRequest, User user);

    RequestResponse getRequestById(Long id, User user);

    void deleteRequest(Long id, User user);
}
