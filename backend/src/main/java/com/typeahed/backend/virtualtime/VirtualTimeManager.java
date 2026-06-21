package com.typeahed.backend.virtualtime;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class VirtualTimeManager {

    private final VirtualTimePersistence virtualTimePersistence;
    private final Clock clock;
    private LocalDateTime savedVirtualTime;
    private LocalDateTime appStartRealTime;

    public VirtualTimeManager(VirtualTimePersistence virtualTimePersistence, Clock clock) {
        this.virtualTimePersistence = virtualTimePersistence;
        this.clock = clock;
    }

    @PostConstruct
    public void init() {
        this.savedVirtualTime = virtualTimePersistence.load();
        this.appStartRealTime = LocalDateTime.now(clock);
    }

    public synchronized LocalDateTime getVirtualTime() {
        Duration elapsed = Duration.between(appStartRealTime, LocalDateTime.now(clock));
        return savedVirtualTime.plus(elapsed);
    }

    public synchronized LocalDateTime getSavedVirtualTime() {
        return savedVirtualTime;
    }

    public synchronized LocalDateTime getAppStartRealTime() {
        return appStartRealTime;
    }

    public synchronized void advanceVirtualTime(Duration duration) {
        if (duration != null) {
            this.savedVirtualTime = this.savedVirtualTime.plus(duration);
        }
    }
}
