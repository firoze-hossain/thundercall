package com.roze.thundercall.api.controller;

import com.roze.thundercall.api.dto.MonitorRequest;
import com.roze.thundercall.api.dto.MonitorResponse;
import com.roze.thundercall.api.dto.MonitorRunResponse;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.service.AuthService;
import com.roze.thundercall.api.service.MonitorService;
import com.roze.thundercall.api.utils.BaseController;
import com.roze.thundercall.api.utils.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/monitors")
public class MonitorController extends BaseController {
    private final MonitorService monitorService;
    private final AuthService authService;

    @PostMapping("")
    public ResponseEntity<BaseResponse<MonitorResponse>> createMonitor(
            @Valid @RequestBody MonitorRequest request, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return created(monitorService.createMonitor(request, user), "Monitor created");
    }

    @GetMapping("")
    public ResponseEntity<BaseResponse<List<MonitorResponse>>> getMyMonitors(Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(monitorService.getMyMonitors(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<MonitorResponse>> getMonitor(
            @PathVariable Long id, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(monitorService.getMonitor(id, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<MonitorResponse>> updateMonitor(
            @PathVariable Long id, @Valid @RequestBody MonitorRequest request, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(monitorService.updateMonitor(id, request, user), "Monitor updated");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteMonitor(
            @PathVariable Long id, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        monitorService.deleteMonitor(id, user);
        return noContent("Monitor deleted");
    }

    @GetMapping("/{id}/runs")
    public ResponseEntity<BaseResponse<List<MonitorRunResponse>>> getRuns(
            @PathVariable Long id, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(monitorService.getRuns(id, user));
    }

    @PostMapping("/{id}/run-now")
    public ResponseEntity<BaseResponse<MonitorRunResponse>> runNow(
            @PathVariable Long id, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(monitorService.runNow(id, user), "Run complete");
    }
}