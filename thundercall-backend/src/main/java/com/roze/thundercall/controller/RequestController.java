package com.roze.thundercall.controller;

import com.roze.thundercall.dto.ApiRequest;
import com.roze.thundercall.dto.ApiResponse;
import com.roze.thundercall.dto.RequestResponse;
import com.roze.thundercall.entity.User;
import com.roze.thundercall.service.AuthService;
import com.roze.thundercall.service.RequestService;
import com.roze.thundercall.utils.BaseController;
import com.roze.thundercall.utils.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
public class RequestController extends BaseController {
    private final RequestService requestService;
    private final AuthService authService;

    @PostMapping("/execute")
    public ResponseEntity<BaseResponse<ApiResponse>> executeRequest(@Valid @RequestBody ApiRequest request, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        ApiResponse responseDTO = requestService.executeRequest(request, user);
        return ok(responseDTO);
    }

    @PostMapping("")
    public ResponseEntity<BaseResponse<RequestResponse>> saveRequest(@Valid @RequestBody ApiRequest apiRequest, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        RequestResponse savedRequest = requestService.saveRequestToCollection(apiRequest, user);
        return created(savedRequest, "Request saved successfully");
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<RequestResponse>> getRequest(@PathVariable Long id, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        RequestResponse requestResponse = requestService.getRequestById(id, user);
        return ok(requestResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteRequest(@PathVariable Long id, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        requestService.deleteRequest(id, user);
        return noContent("Request deleted successfully");
    }
}
