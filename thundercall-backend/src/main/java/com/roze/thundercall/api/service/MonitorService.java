package com.roze.thundercall.api.service;

import com.roze.thundercall.api.dto.MonitorRequest;
import com.roze.thundercall.api.dto.MonitorResponse;
import com.roze.thundercall.api.dto.MonitorRunResponse;
import com.roze.thundercall.api.entity.User;

import java.util.List;

public interface MonitorService {
    MonitorResponse createMonitor(MonitorRequest request, User owner);

    List<MonitorResponse> getMyMonitors(User owner);

    MonitorResponse getMonitor(Long id, User owner);

    MonitorResponse updateMonitor(Long id, MonitorRequest request, User owner);

    void deleteMonitor(Long id, User owner);

    List<MonitorRunResponse> getRuns(Long id, User owner);

    /** Runs the monitor immediately, outside its schedule — the same
     * execution path a scheduled firing uses, just triggered by a
     * button instead of a timer. Runs synchronously so the caller gets
     * the result straight back; a whole collection is expected to
     * finish in seconds, not minutes. */
    MonitorRunResponse runNow(Long id, User owner);

    /** Called once at application startup to rebuild the live schedule
     * for every monitor that should be running — the in-memory
     * schedule map doesn't survive a restart. */
    void rescheduleAllEnabledMonitors();
}