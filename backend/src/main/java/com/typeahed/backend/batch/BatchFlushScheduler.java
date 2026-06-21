package com.typeahed.backend.batch;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BatchFlushScheduler {

    private static final Logger logger = LoggerFactory.getLogger(BatchFlushScheduler.class);

    private final BatchBuffer batchBuffer;
    private final BatchWriter batchWriter;

    public BatchFlushScheduler(BatchBuffer batchBuffer, BatchWriter batchWriter) {
        this.batchBuffer = batchBuffer;
        this.batchWriter = batchWriter;
    }

    /**
     * Recovery trigger on startup.
     * Processes any unbatched search logs from previous sessions.
     */
    @PostConstruct
    public void onStartup() {
        logger.info("Initializing BatchFlushScheduler. Running startup recovery check...");
        try {
            batchWriter.recover();
        } catch (Exception e) {
            logger.error("Startup batch recovery failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Periodic task checking if the flush conditions have been met.
     */
    @Scheduled(fixedRateString = "${app.batch.poll-rate-ms:1000}")
    public void checkAndFlush() {
        if (batchBuffer.shouldFlush()) {
            logger.debug("Flush condition met. Triggering flush...");
            batchWriter.flush();
        }
    }
}
