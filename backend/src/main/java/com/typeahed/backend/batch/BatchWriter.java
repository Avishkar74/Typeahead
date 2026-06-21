package com.typeahed.backend.batch;

import com.typeahed.backend.cache.CacheService;
import com.typeahed.backend.cache.RedisNodeRouter;
import com.typeahed.backend.entity.Query;
import com.typeahed.backend.repository.QueryRepository;
import com.typeahed.backend.repository.SearchLogRepository;
import com.typeahed.backend.repository.UnbatchedQueryCount;
import com.typeahed.backend.virtualtime.VirtualTimeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class BatchWriter {

    private static final Logger logger = LoggerFactory.getLogger(BatchWriter.class);
    private static final int VIRTUAL_TIME_INCREMENT_SECONDS = 60;

    private final BatchBuffer batchBuffer;
    private final QueryRepository queryRepository;
    private final SearchLogRepository searchLogRepository;
    private final VirtualTimeManager virtualTimeManager;
    private final RedisNodeRouter redisNodeRouter;
    private final CacheService cacheService;
    private final Clock clock;

    public BatchWriter(BatchBuffer batchBuffer,
                       QueryRepository queryRepository,
                       SearchLogRepository searchLogRepository,
                       VirtualTimeManager virtualTimeManager,
                       RedisNodeRouter redisNodeRouter,
                       CacheService cacheService,
                       Clock clock) {
        this.batchBuffer = batchBuffer;
        this.queryRepository = queryRepository;
        this.searchLogRepository = searchLogRepository;
        this.virtualTimeManager = virtualTimeManager;
        this.redisNodeRouter = redisNodeRouter;
        this.cacheService = cacheService;
        this.clock = clock;
    }

    /**
     * Flushes the current accumulated search events in the buffer to PostgreSQL under transactional control.
     */
    @Transactional
    public void flush() {
        Map<String, Integer> snapshot = batchBuffer.getAndClear();
        if (snapshot.isEmpty()) {
            return;
        }

        try {
            LocalDateTime currentVirtualTime = virtualTimeManager.getVirtualTime();
            List<String> queryKeys = new ArrayList<>(snapshot.keySet());

            // 1. Batch update queries table
            updateQueriesFromCounts(snapshot, currentVirtualTime);

            // 2. Mark search_logs as batched
            searchLogRepository.markLogsAsBatched(queryKeys, LocalDateTime.now(clock));

            // 3. Advance virtual time
            virtualTimeManager.advanceVirtualTime(Duration.ofSeconds(VIRTUAL_TIME_INCREMENT_SECONDS));

            // 4. Invalidate cache prefixes
            invalidatePrefixes(queryKeys);

            logger.info("Successfully flushed {} queries to database", snapshot.size());

        } catch (Exception e) {
            logger.error("Failed to perform batch flush: {}", e.getMessage(), e);
            throw e; // Re-throw to trigger rollback
        }
    }

    /**
     * Aggregates and updates any unbatched logs from search_logs (e.g. following an application crash).
     */
    @Transactional
    public void recover() {
        List<UnbatchedQueryCount> unbatched = searchLogRepository.findUnbatchedQueryCounts();
        if (unbatched.isEmpty()) {
            return;
        }

        logger.info("Found {} unbatched query groups in search_logs. Starting recovery aggregation...", unbatched.size());

        try {
            LocalDateTime currentVirtualTime = virtualTimeManager.getVirtualTime();
            Map<String, Integer> counts = new HashMap<>();
            List<String> queryKeys = new ArrayList<>();

            for (UnbatchedQueryCount u : unbatched) {
                counts.put(u.getQueryLower(), u.getCount().intValue());
                queryKeys.add(u.getQueryLower());
            }

            // 1. Batch update queries table
            updateQueriesFromCounts(counts, currentVirtualTime);

            // 2. Mark search_logs as batched
            searchLogRepository.markLogsAsBatched(queryKeys, LocalDateTime.now(clock));

            logger.info("Successfully recovered and aggregated {} unbatched queries", queryKeys.size());

        } catch (Exception e) {
            logger.error("Failed to perform batch recovery: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void updateQueriesFromCounts(Map<String, Integer> counts, LocalDateTime currentVirtualTime) {
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            String queryLower = entry.getKey();
            int count = entry.getValue();

            Optional<Query> queryOpt = queryRepository.findByQueryLower(queryLower);
            Query query;
            if (queryOpt.isPresent()) {
                query = queryOpt.get();
                query.setGlobalCount(query.getGlobalCount() + count);

                LocalDateTime lastSearched = query.getLastSearchedAt();
                if (lastSearched != null) {
                    if (Duration.between(lastSearched, currentVirtualTime).toHours() < 24) {
                        query.setDailyCount(query.getDailyCount() + count);
                    } else {
                        query.setDailyCount(count);
                    }

                    if (Duration.between(lastSearched, currentVirtualTime).toDays() < 7) {
                        query.setWeeklyCount(query.getWeeklyCount() + count);
                    } else {
                        query.setWeeklyCount(count);
                    }
                } else {
                    query.setDailyCount(query.getDailyCount() + count);
                    query.setWeeklyCount(query.getWeeklyCount() + count);
                }
                query.setLastSearchedAt(currentVirtualTime);
            } else {
                query = new Query();
                query.setQueryText(queryLower);
                query.setQueryLower(queryLower);
                query.setGlobalCount(count);
                query.setWeeklyCount(count);
                query.setDailyCount(count);
                query.setFirstSearchedAt(currentVirtualTime);
                query.setLastSearchedAt(currentVirtualTime);
            }

            double score = 0.6 * query.getGlobalCount()
                         + 0.3 * query.getWeeklyCount()
                         + 0.1 * query.getDailyCount();
            query.setTrendingScore(BigDecimal.valueOf(score));
            query.setTrendingScoreCalculatedAt(currentVirtualTime);

            queryRepository.save(query);
        }
    }

    private void invalidatePrefixes(List<String> queryKeys) {
        Set<String> invalidatedPrefixes = new HashSet<>();
        for (String queryLower : queryKeys) {
            for (int i = 1; i <= queryLower.length(); i++) {
                invalidatedPrefixes.add(queryLower.substring(0, i));
            }
        }

        for (String prefix : invalidatedPrefixes) {
            String node = redisNodeRouter.route(prefix);
            logger.info("Invalidating cache prefix '{}' on Redis node '{}'", prefix, node);
            cacheService.delete(prefix, "trending");
            cacheService.delete(prefix, "global");
        }
    }
}
