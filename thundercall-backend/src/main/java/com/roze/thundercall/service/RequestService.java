package com.roze.thundercall.service;

import com.roze.thundercall.dto.ApiRequest;
import com.roze.thundercall.dto.ApiResponse;
import com.roze.thundercall.dto.RequestResponse;
import com.roze.thundercall.entity.User;

public interface RequestService {
    ApiResponse executeRequest(ApiRequest apiRequest, User user);

    RequestResponse saveRequestToCollection(ApiRequest apiRequest, User user);

    RequestResponse getRequestById(Long id, User user);

    void deleteRequest(Long id, User user);
}
