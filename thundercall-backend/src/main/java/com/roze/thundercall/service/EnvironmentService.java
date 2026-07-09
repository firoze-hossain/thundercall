package com.roze.thundercall.service;

import com.roze.thundercall.dto.EnvironmentRequest;
import com.roze.thundercall.dto.EnvironmentResponse;
import com.roze.thundercall.entity.User;

import java.util.List;

public interface EnvironmentService {
    EnvironmentResponse createEnvironment(EnvironmentRequest request, User user);
    
    List<EnvironmentResponse> getUserEnvironments(User user);
    
    EnvironmentResponse getEnvironmentById(Long id, User user);
    
    EnvironmentResponse updateEnvironment(Long id, EnvironmentRequest request, User user);
    
    void deleteEnvironment(Long id, User user);
    
    List<EnvironmentResponse> getActiveEnvironments(User user);
    
    EnvironmentResponse toggleEnvironmentStatus(Long id, User user, Boolean isActive);
}