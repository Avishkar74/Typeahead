package com.typeahed.backend.virtualtime;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class VirtualTimeShutdownHook {

    private static final Logger logger = LoggerFactory.getLogger(VirtualTimeShutdownHook.class);

    private final VirtualTimeManager virtualTimeManager;
    private final VirtualTimePersistence virtualTimePersistence;

    public VirtualTimeShutdownHook(VirtualTimeManager virtualTimeManager,
                                    VirtualTimePersistence virtualTimePersistence) {
        this.virtualTimeManager = virtualTimeManager;
        this.virtualTimePersistence = virtualTimePersistence;
    }

    @PreDestroy
    public void onDestroy() {
        logger.info("Application shutting down. Persisting final virtual time...");
        try {
            LocalDateTime finalVirtualTime = virtualTimeManager.getVirtualTime();
            virtualTimePersistence.save(finalVirtualTime);
            logger.info("Successfully persisted final virtual time: {}", finalVirtualTime);
        } catch (Exception e) {
            logger.error("Failed to persist virtual time on shutdown: {}", e.getMessage(), e);
        }
    }
}
