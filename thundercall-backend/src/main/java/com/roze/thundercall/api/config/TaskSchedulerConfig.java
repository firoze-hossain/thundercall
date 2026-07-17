package com.roze.thundercall.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** Monitors need schedules created and cancelled at runtime (created,
 * edited, deleted, enabled/disabled by users) — Spring's @Scheduled
 * annotation only covers fixed, compile-time schedules, so this exposes
 * a real TaskScheduler bean that MonitorServiceImpl can call
 * scheduleAtFixedRate()/cancel() on directly per monitor. */
@Configuration
@EnableScheduling
public class TaskSchedulerConfig {
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("monitor-scheduler-");
        scheduler.setDaemon(true);
        scheduler.initialize();
        return scheduler;
    }
}