package com.roze.thundercall.service.impl;

import com.roze.thundercall.dto.RequestHistoryResponse;
import com.roze.thundercall.entity.RequestHistory;
import com.roze.thundercall.entity.User;
import com.roze.thundercall.exception.ResourceNotFoundException;
import com.roze.thundercall.mapper.RequestHistoryMapper;
import com.roze.thundercall.repository.RequestHistoryRepository;
import com.roze.thundercall.service.RequestHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestHistoryServiceImpl implements RequestHistoryService {
    private final RequestHistoryRepository requestHistoryRepository;
    private final RequestHistoryMapper requestHistoryMapper;

    @Override
    public List<RequestHistoryResponse> getUserRequestHistory(User user) {
        List<RequestHistory> history = requestHistoryRepository.findByRequestCollectionWorkspaceOwnerOrderByTimestampDesc(user);
        return history.stream()
                .map(requestHistoryMapper::toResponse)
                .toList();
    }

    @Override
    public Page<RequestHistoryResponse> getUserRequestHistory(User user, Pageable pageable) {
        Page<RequestHistory> historyPage = requestHistoryRepository.findByRequestCollectionWorkspaceOwnerOrderByTimestampDesc(user, pageable);
        return historyPage.map(requestHistoryMapper::toResponse);
    }

    @Override
    public List<RequestHistoryResponse> getRequestHistory(Long requestId, User user) {
        List<RequestHistory> history = requestHistoryRepository.findByRequestIdAndRequestCollectionWorkspaceOwnerOrderByTimestampDesc(requestId, user);
        return history.stream()
                .map(requestHistoryMapper::toResponse)
                .toList();
    }

    @Override
    public Page<RequestHistoryResponse> getRequestHistory(Long requestId, User user, Pageable pageable) {
        Page<RequestHistory> historyPage = requestHistoryRepository.findByRequestIdAndRequestCollectionWorkspaceOwnerOrderByTimestampDesc(requestId, user, pageable);
        return historyPage.map(requestHistoryMapper::toResponse);
    }

    @Override
    public List<RequestHistoryResponse> getRequestHistoryByDateRange(User user, LocalDateTime startDate, LocalDateTime endDate) {
        List<RequestHistory> history = requestHistoryRepository.findByUserAndDateRange(user, startDate, endDate);
        return history.stream()
                .map(requestHistoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void clearUserHistory(User user) {
        requestHistoryRepository.deleteByRequestCollectionWorkspaceOwner(user);
    }

    @Override
    @Transactional
    public void clearRequestHistory(Long requestId, User user) {
        // Verify the request belongs to the user
        List<RequestHistory> history = requestHistoryRepository.findByRequestIdAndRequestCollectionWorkspaceOwnerOrderByTimestampDesc(requestId, user);
        if (history.isEmpty()) {
            throw new ResourceNotFoundException("Request history not found or you don't have permission");
        }
        requestHistoryRepository.deleteByRequestIdAndRequestCollectionWorkspaceOwner(requestId, user);
    }

    @Override
    @Transactional
    public void clearOldHistory(User user, LocalDateTime beforeDate) {
        requestHistoryRepository.deleteByUserAndTimestampBefore(user, beforeDate);
    }

    @Override
    public Long getUserHistoryCount(User user) {
        return requestHistoryRepository.countByUser(user);
    }
}