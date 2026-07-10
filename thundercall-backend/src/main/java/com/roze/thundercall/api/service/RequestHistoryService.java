package com.roze.thundercall.api.service;

import com.roze.thundercall.api.dto.RequestHistoryResponse;
import com.roze.thundercall.api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface RequestHistoryService {
    List<RequestHistoryResponse> getUserRequestHistory(User user);
    
    Page<RequestHistoryResponse> getUserRequestHistory(User user, Pageable pageable);
    
    List<RequestHistoryResponse> getRequestHistory(Long requestId, User user);
    
    Page<RequestHistoryResponse> getRequestHistory(Long requestId, User user, Pageable pageable);
    
    List<RequestHistoryResponse> getRequestHistoryByDateRange(User user, LocalDateTime startDate, LocalDateTime endDate);
    
    void clearUserHistory(User user);
    
    void clearRequestHistory(Long requestId, User user);
    
    void clearOldHistory(User user, LocalDateTime beforeDate);
    
    Long getUserHistoryCount(User user);
}