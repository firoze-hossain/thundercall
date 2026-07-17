package com.roze.thundercall.api.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roze.thundercall.api.dto.MonitorRequest;
import com.roze.thundercall.api.dto.MonitorResponse;
import com.roze.thundercall.api.dto.MonitorRunResponse;
import com.roze.thundercall.api.entity.*;
import com.roze.thundercall.api.entity.Collection;
import com.roze.thundercall.api.exception.ResourceNotFoundException;
import com.roze.thundercall.api.repository.CollectionRepository;
import com.roze.thundercall.api.repository.EnvironmentRepository;
import com.roze.thundercall.api.repository.MonitorRepository;
import com.roze.thundercall.api.repository.MonitorRunRepository;
import com.roze.thundercall.api.service.EmailService;
import com.roze.thundercall.api.service.MonitorService;
import com.roze.thundercall.api.utils.MonitorVariableResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/** Monitors run a whole collection on a schedule and keep a history of
 * pass/fail results — Postman's Monitors feature. Scheduling is dynamic
 * (created/edited/deleted/toggled at runtime by users), which is why
 * this drives a ThreadPoolTaskScheduler directly rather than using
 * Spring's @Scheduled (fixed, compile-time schedules only). */
@Service
@RequiredArgsConstructor
@Slf4j
public class MonitorServiceImpl implements MonitorService {
    private final MonitorRepository monitorRepository;
    private final MonitorRunRepository monitorRunRepository;
    private final CollectionRepository collectionRepository;
    private final EnvironmentRepository environmentRepository;
    private final EmailService emailService;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<Long, ScheduledFuture<?>> activeSchedules = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public MonitorResponse createMonitor(MonitorRequest request, User owner) {
        Collection collection = collectionRepository.findByIdAndWorkspaceOwner(request.collectionId(), owner)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found"));
        Environment environment = resolveEnvironment(request.environmentId());

        Monitor monitor = Monitor.builder()
                .name(request.name())
                .owner(owner)
                .collection(collection)
                .environment(environment)
                .intervalMinutes(request.intervalMinutes())
                .enabled(request.enabled())
                .notifyOnFailure(request.notifyOnFailure())
                .notifyEmail(request.notifyEmail())
                .build();
        monitor = monitorRepository.save(monitor);

        if (monitor.isEnabled()) {
            scheduleMonitor(monitor);
        }
        return toResponse(monitor);
    }

    @Override
    public List<MonitorResponse> getMyMonitors(User owner) {
        return monitorRepository.findByOwnerOrderByCreatedAtDesc(owner).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public MonitorResponse getMonitor(Long id, User owner) {
        return toResponse(findOwned(id, owner));
    }

    @Override
    @Transactional
    public MonitorResponse updateMonitor(Long id, MonitorRequest request, User owner) {
        Monitor monitor = findOwned(id, owner);
        Collection collection = collectionRepository.findByIdAndWorkspaceOwner(request.collectionId(), owner)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found"));
        Environment environment = resolveEnvironment(request.environmentId());

        monitor.setName(request.name());
        monitor.setCollection(collection);
        monitor.setEnvironment(environment);
        monitor.setIntervalMinutes(request.intervalMinutes());
        monitor.setEnabled(request.enabled());
        monitor.setNotifyOnFailure(request.notifyOnFailure());
        monitor.setNotifyEmail(request.notifyEmail());
        monitor = monitorRepository.save(monitor);

        // Always re-schedule from scratch — simplest way to guarantee the
        // interval and enabled state are never stale after an edit.
        unscheduleMonitor(monitor.getId());
        if (monitor.isEnabled()) {
            scheduleMonitor(monitor);
        }
        return toResponse(monitor);
    }

    @Override
    @Transactional
    public void deleteMonitor(Long id, User owner) {
        Monitor monitor = findOwned(id, owner);
        unscheduleMonitor(monitor.getId());
        monitorRepository.delete(monitor);
    }

    @Override
    public List<MonitorRunResponse> getRuns(Long id, User owner) {
        Monitor monitor = findOwned(id, owner); // ownership check
        return monitorRunRepository.findByMonitorIdOrderByStartedAtDesc(monitor.getId(), PageRequest.of(0, 50))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public MonitorRunResponse runNow(Long id, User owner) {
        Monitor monitor = findOwned(id, owner);
        MonitorRun run = executeMonitorRun(monitor.getId());
        return run != null ? toResponse(run) : null;
    }

    @Override
    @EventListener(ApplicationReadyEvent.class)
    public void rescheduleAllEnabledMonitors() {
        List<Monitor> enabled = monitorRepository.findByEnabledTrue();
        for (Monitor monitor : enabled) {
            scheduleMonitor(monitor);
        }
        if (!enabled.isEmpty()) {
            log.info("Rescheduled {} enabled monitor(s) on startup", enabled.size());
        }
    }

    private void scheduleMonitor(Monitor monitor) {
        unscheduleMonitor(monitor.getId()); // avoid ever double-scheduling the same monitor
        Long monitorId = monitor.getId();
        Duration period = Duration.ofMinutes(monitor.getIntervalMinutes());
        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(
                () -> executeMonitorRun(monitorId), Instant.now().plus(period), period);
        activeSchedules.put(monitorId, future);
    }

    private void unscheduleMonitor(Long monitorId) {
        ScheduledFuture<?> existing = activeSchedules.remove(monitorId);
        if (existing != null) {
            existing.cancel(false);
        }
    }

    /** The actual collection run — fetches the monitor fresh (schedules
     * fire long after the original request that created them, so
     * nothing from that request context can be relied on), runs every
     * request in the collection (including nested folders), and saves
     * the aggregate result. Deliberately does NOT go through
     * RequestService.executeRequest() — that also writes to the user's
     * manual request history, which would flood it with automated
     * firings on any reasonably short interval. */
    @Transactional
    public MonitorRun executeMonitorRun(Long monitorId) {
        Monitor monitor = monitorRepository.findById(monitorId).orElse(null);
        if (monitor == null || !monitor.isEnabled()) {
            return null; // deleted or disabled since this fired — nothing to do
        }

        LocalDateTime startedAt = LocalDateTime.now();
        List<Request> requests = collectAllRequests(monitor.getCollection());
        Map<String, String> vars = monitor.getEnvironment() != null
                ? monitor.getEnvironment().getVariables() : Collections.emptyMap();

        List<Map<String, Object>> details = new ArrayList<>();
        int passed = 0;
        int failed = 0;
        long totalDuration = 0;

        for (Request request : requests) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", request.getName());
            entry.put("method", request.getMethod().name());
            entry.put("url", request.getUrl());
            long requestStart = System.currentTimeMillis();
            try {
                int statusCode = executeOne(request, vars);
                long duration = System.currentTimeMillis() - requestStart;
                boolean ok = statusCode < 400;
                entry.put("statusCode", statusCode);
                entry.put("durationMs", duration);
                entry.put("success", ok);
                totalDuration += duration;
                if (ok) {
                    passed++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - requestStart;
                entry.put("statusCode", 0);
                entry.put("durationMs", duration);
                entry.put("success", false);
                entry.put("error", e.getMessage());
                totalDuration += duration;
                failed++;
            }
            details.add(entry);
        }

        boolean overallSuccess = failed == 0 && !requests.isEmpty();
        String detailsJson;
        try {
            detailsJson = objectMapper.writeValueAsString(details);
        } catch (Exception e) {
            detailsJson = "[]";
        }

        MonitorRun run = MonitorRun.builder()
                .monitor(monitor)
                .startedAt(startedAt)
                .completedAt(LocalDateTime.now())
                .totalRequests(requests.size())
                .passedRequests(passed)
                .failedRequests(failed)
                .avgResponseTimeMs(requests.isEmpty() ? 0 : totalDuration / requests.size())
                .success(overallSuccess)
                .details(detailsJson)
                .build();
        run = monitorRunRepository.save(run);

        monitor.setLastRunAt(LocalDateTime.now());
        monitor.setLastRunStatus(requests.isEmpty() ? "error" : (overallSuccess ? "passed" : "failed"));
        monitorRepository.save(monitor);

        if (!overallSuccess && monitor.isNotifyOnFailure()) {
            sendFailureNotification(monitor, run);
        }

        return run;
    }

    private int executeOne(Request request, Map<String, String> vars) throws Exception {
        String url = MonitorVariableResolver.resolve(request.getUrl(), vars);
        String body = MonitorVariableResolver.resolve(request.getBody(), vars);

        HttpHeaders headers = new HttpHeaders();
        if (request.getHeaders() != null && !request.getHeaders().isBlank()) {
            try {
                Map<String, String> headerMap = objectMapper.readValue(
                        request.getHeaders(), new TypeReference<Map<String, String>>() {
                        });
                headerMap.forEach((k, v) -> {
                    if (k != null && !k.isBlank() && v != null) {
                        headers.add(k.trim(), MonitorVariableResolver.resolve(v.trim(), vars));
                    }
                });
            } catch (Exception ignored) {
                // malformed headers JSON — proceed without them rather than failing the whole request
            }
        }

        HttpEntity<String> entity = new HttpEntity<>(body != null && !body.isBlank() ? body : null, headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(
                url, org.springframework.http.HttpMethod.valueOf(request.getMethod().name()), entity, byte[].class);
        return response.getStatusCode().value();
    }

    /** Top-level requests plus every request in every folder,
     * recursively — a collection's requests aren't stored in one flat
     * list (see Collection/Folder), so this walks the whole tree. */
    private List<Request> collectAllRequests(Collection collection) {
        List<Request> all = new ArrayList<>(collection.getRequests());
        for (Folder folder : collection.getFolders()) {
            collectFromFolder(folder, all);
        }
        return all;
    }

    private void collectFromFolder(Folder folder, List<Request> out) {
        out.addAll(folder.getRequests());
        for (Folder child : folder.getChildFolders()) {
            collectFromFolder(child, out);
        }
    }

    private void sendFailureNotification(Monitor monitor, MonitorRun run) {
        String to = monitor.getNotifyEmail() != null && !monitor.getNotifyEmail().isBlank()
                ? monitor.getNotifyEmail() : monitor.getOwner().getEmail();
        try {
            String subject = "Thundercall Monitor \"" + monitor.getName() + "\" failed";
            String body = "Monitor: " + monitor.getName() + "\n"
                    + "Collection: " + monitor.getCollection().getName() + "\n"
                    + "Run at: " + run.getStartedAt() + "\n"
                    + "Results: " + run.getPassedRequests() + " passed, " + run.getFailedRequests() + " failed"
                    + " (of " + run.getTotalRequests() + " total)\n\n"
                    + "Open Thundercall to see the full details of what failed.";
            emailService.send(to, subject, body);
        } catch (Exception e) {
            log.warn("Couldn't send monitor failure notification for monitor {}: {}", monitor.getId(), e.getMessage());
        }
    }

    private Environment resolveEnvironment(Long environmentId) {
        if (environmentId == null) {
            return null;
        }
        return environmentRepository.findById(environmentId).orElse(null);
    }

    private Monitor findOwned(Long id, User owner) {
        return monitorRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found"));
    }

    private MonitorResponse toResponse(Monitor monitor) {
        return new MonitorResponse(
                monitor.getId(), monitor.getName(),
                monitor.getCollection().getId(), monitor.getCollection().getName(),
                monitor.getEnvironment() != null ? monitor.getEnvironment().getId() : null,
                monitor.getEnvironment() != null ? monitor.getEnvironment().getName() : null,
                monitor.getIntervalMinutes(), monitor.isEnabled(), monitor.isNotifyOnFailure(),
                monitor.getNotifyEmail(), monitor.getLastRunAt(), monitor.getLastRunStatus(),
                monitor.getCreatedAt());
    }

    private MonitorRunResponse toResponse(MonitorRun run) {
        return new MonitorRunResponse(
                run.getId(), run.getStartedAt(), run.getCompletedAt(),
                run.getTotalRequests(), run.getPassedRequests(), run.getFailedRequests(),
                run.getAvgResponseTimeMs(), run.isSuccess(), run.getDetails());
    }
}