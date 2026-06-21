package com.typeahed.backend.batch;

import com.typeahed.backend.config.AppProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class BatchBuffer {

    private final Map<String, Integer> buffer = new HashMap<>();
    private final int bufferSizeThreshold;
    private final int flushIntervalSeconds;
    private final Clock clock;
    private Instant lastFlushTime;

    public BatchBuffer(AppProperties appProperties, Clock clock) {
        this.bufferSizeThreshold = appProperties.getBatch().getBufferSizeThreshold();
        this.flushIntervalSeconds = appProperties.getBatch().getFlushIntervalSeconds();
        this.clock = clock;
        this.lastFlushTime = Instant.now(clock);
    }

    /**
     * Aggregates a search event in the buffer.
     * Normalizes by trimming and converting to lower case.
     */
    public synchronized void addSearch(String queryText) {
        if (queryText == null) {
            return;
        }
        String queryLower = queryText.toLowerCase().strip();
        if (queryLower.isEmpty()) {
            return;
        }
        buffer.put(queryLower, buffer.getOrDefault(queryLower, 0) + 1);
    }

    /**
     * Checks if the buffer has met its flush criteria.
     */
    public synchronized boolean shouldFlush() {
        long elapsedSeconds = Instant.now(clock).getEpochSecond() - lastFlushTime.getEpochSecond();
        return buffer.size() >= bufferSizeThreshold || elapsedSeconds >= flushIntervalSeconds;
    }

    /**
     * Retrieves a copy of the accumulated buffer and clears it.
     */
    public synchronized Map<String, Integer> getAndClear() {
        Map<String, Integer> snapshot = new HashMap<>(buffer);
        buffer.clear();
        lastFlushTime = Instant.now(clock);
        return snapshot;
    }

    public synchronized int getBufferSize() {
        return buffer.size();
    }

    public synchronized Instant getLastFlushTime() {
        return lastFlushTime;
    }

    public synchronized void setLastFlushTime(Instant lastFlushTime) {
        this.lastFlushTime = lastFlushTime;
    }
}
