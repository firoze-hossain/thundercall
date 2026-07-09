package com.roze.thundercall.controller;

import com.roze.thundercall.dto.RequestHistoryResponse;
import com.roze.thundercall.entity.User;
import com.roze.thundercall.service.AuthService;
import com.roze.thundercall.service.RequestHistoryService;
import com.roze.thundercall.utils.BaseController;
import com.roze.thundercall.utils.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
public class RequestHistoryController extends BaseController {
    private final RequestHistoryService requestHistoryService;
    private final AuthService authService;

    @GetMapping("")
    public ResponseEntity<BaseResponse<List<RequestHistoryResponse>>> getUserRequestHistory(Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        List<RequestHistoryResponse> responses = requestHistoryService.getUserRequestHistory(user);
        return ok(responses);
    }

    @GetMapping("/paged")
    public ResponseEntity<BaseResponse<Page<RequestHistoryResponse>>> getUserRequestHistoryPaged(
            Authentication authentication,
            Pageable pageable) {
        User user = authService.getUserFromAuthentication(authentication);
        Page<RequestHistoryResponse> responses = requestHistoryService.getUserRequestHistory(user, pageable);
        return ok(responses);
    }

    @GetMapping("/request/{requestId}")
    public ResponseEntity<BaseResponse<List<RequestHistoryResponse>>> getRequestHistory(
            @PathVariable Long requestId,
            Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        List<RequestHistoryResponse> responses = requestHistoryService.getRequestHistory(requestId, user);
        return ok(responses);
    }

    @GetMapping("/request/{requestId}/paged")
    public ResponseEntity<BaseResponse<Page<RequestHistoryResponse>>> getRequestHistoryPaged(
            @PathVariable Long requestId,
            Authentication authentication,
            Pageable pageable) {
        User user = authService.getUserFromAuthentication(authentication);
        Page<RequestHistoryResponse> responses = requestHistoryService.getRequestHistory(requestId, user, pageable);
        return ok(responses);
    }

    @GetMapping("/date-range")
    public ResponseEntity<BaseResponse<List<RequestHistoryResponse>>> getRequestHistoryByDateRange(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        User user = authService.getUserFromAuthentication(authentication);
        List<RequestHistoryResponse> responses = requestHistoryService.getRequestHistoryByDateRange(user, startDate, endDate);
        return ok(responses);
    }

    @GetMapping("/count")
    public ResponseEntity<BaseResponse<Long>> getUserHistoryCount(Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        Long count = requestHistoryService.getUserHistoryCount(user);
        return ok(count);
    }

    @DeleteMapping("")
    public ResponseEntity<BaseResponse<Void>> clearUserHistory(Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        requestHistoryService.clearUserHistory(user);
        return noContent("History cleared successfully");
    }

    @DeleteMapping("/request/{requestId}")
    public ResponseEntity<BaseResponse<Void>> clearRequestHistory(
            @PathVariable Long requestId,
            Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        requestHistoryService.clearRequestHistory(requestId, user);
        return noContent("Request history cleared successfully");
    }

    @DeleteMapping("/old")
    public ResponseEntity<BaseResponse<Void>> clearOldHistory(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime beforeDate) {
        User user = authService.getUserFromAuthentication(authentication);
        requestHistoryService.clearOldHistory(user, beforeDate);
        return noContent("Old history cleared successfully");
    }
}