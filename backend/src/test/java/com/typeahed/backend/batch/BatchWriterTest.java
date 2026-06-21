package com.typeahed.backend.batch;

import com.typeahed.backend.cache.CacheService;
import com.typeahed.backend.cache.RedisNodeRouter;
import com.typeahed.backend.entity.Query;
import com.typeahed.backend.repository.QueryRepository;
import com.typeahed.backend.repository.SearchLogRepository;
import com.typeahed.backend.repository.UnbatchedQueryCount;
import com.typeahed.backend.virtualtime.VirtualTimeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BatchWriterTest {

    private BatchBuffer batchBuffer;
    private QueryRepository queryRepository;
    private SearchLogRepository searchLogRepository;
    private VirtualTimeManager virtualTimeManager;
    private RedisNodeRouter redisNodeRouter;
    private CacheService cacheService;
    private Clock clock;
    private BatchWriter batchWriter;

    @BeforeEach
    void setUp() {
        batchBuffer = mock(BatchBuffer.class);
        queryRepository = mock(QueryRepository.class);
        searchLogRepository = mock(SearchLogRepository.class);
        virtualTimeManager = mock(VirtualTimeManager.class);
        redisNodeRouter = mock(RedisNodeRouter.class);
        cacheService = mock(CacheService.class);
        clock = Clock.fixed(Instant.parse("2026-06-22T00:00:00Z"), ZoneId.systemDefault());

        batchWriter = new BatchWriter(
                batchBuffer,
                queryRepository,
                searchLogRepository,
                virtualTimeManager,
                redisNodeRouter,
                cacheService,
                clock
        );

        when(virtualTimeManager.getVirtualTime()).thenReturn(LocalDateTime.of(2006, 6, 1, 10, 0, 0));
        when(redisNodeRouter.route(anyString())).thenReturn("localhost:6379");
    }

    @Test
    void testFlushNoOpsIfEmpty() {
        when(batchBuffer.getAndClear()).thenReturn(Map.of());

        batchWriter.flush();

        verifyNoInteractions(queryRepository);
        verifyNoInteractions(searchLogRepository);
        verifyNoInteractions(virtualTimeManager);
    }

    @Test
    void testFlushNewQuery() {
        Map<String, Integer> snapshot = Map.of("iphone", 5);
        when(batchBuffer.getAndClear()).thenReturn(snapshot);
        when(queryRepository.findByQueryLower("iphone")).thenReturn(Optional.empty());

        batchWriter.flush();

        // 1. Verify Query save
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(queryRepository, times(1)).save(queryCaptor.capture());
        Query saved = queryCaptor.getValue();
        assertThat(saved.getQueryLower()).isEqualTo("iphone");
        assertThat(saved.getGlobalCount()).isEqualTo(5);
        assertThat(saved.getWeeklyCount()).isEqualTo(5);
        assertThat(saved.getDailyCount()).isEqualTo(5);
        // score = 0.6 * 5 + 0.3 * 5 + 0.1 * 5 = 5.0
        assertThat(saved.getTrendingScore()).isEqualByComparingTo(BigDecimal.valueOf(5.0));

        // 2. Verify SearchLog marked
        verify(searchLogRepository, times(1)).markLogsAsBatched(
                eq(List.of("iphone")),
                eq(LocalDateTime.now(clock))
        );

        // 3. Verify VirtualTime advanced
        verify(virtualTimeManager, times(1)).advanceVirtualTime(Duration.ofSeconds(60));

        // 4. Verify routing/invalidations
        // "iphone" generates prefixes: "i", "ip", "iph", "ipho", "iphon", "iphone"
        verify(redisNodeRouter, times(6)).route(anyString());
    }

    @Test
    void testFlushExistingQueryWithinWindow() {
        Map<String, Integer> snapshot = Map.of("iphone", 2);
        when(batchBuffer.getAndClear()).thenReturn(snapshot);

        Query existing = new Query();
        existing.setQueryLower("iphone");
        existing.setQueryText("iphone");
        existing.setGlobalCount(100);
        existing.setWeeklyCount(10);
        existing.setDailyCount(5);
        // last searched is 2 hours before the current virtual time (10:00:00)
        existing.setLastSearchedAt(LocalDateTime.of(2006, 6, 1, 8, 0, 0));

        when(queryRepository.findByQueryLower("iphone")).thenReturn(Optional.of(existing));

        batchWriter.flush();

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(queryRepository, times(1)).save(queryCaptor.capture());
        Query saved = queryCaptor.getValue();
        assertThat(saved.getGlobalCount()).isEqualTo(102);
        assertThat(saved.getWeeklyCount()).isEqualTo(12); // within 7 days
        assertThat(saved.getDailyCount()).isEqualTo(7);   // within 24 hours
        // score = 0.6*102 + 0.3*12 + 0.1*7 = 61.2 + 3.6 + 0.7 = 65.5
        assertThat(saved.getTrendingScore()).isEqualByComparingTo(BigDecimal.valueOf(65.5));
    }

    @Test
    void testFlushExistingQueryOutsideWindow() {
        Map<String, Integer> snapshot = Map.of("iphone", 2);
        when(batchBuffer.getAndClear()).thenReturn(snapshot);

        Query existing = new Query();
        existing.setQueryLower("iphone");
        existing.setQueryText("iphone");
        existing.setGlobalCount(100);
        existing.setWeeklyCount(10);
        existing.setDailyCount(5);
        // last searched is 8 days before (outside both 24h and 7d windows)
        existing.setLastSearchedAt(LocalDateTime.of(2006, 5, 24, 10, 0, 0));

        when(queryRepository.findByQueryLower("iphone")).thenReturn(Optional.of(existing));

        batchWriter.flush();

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(queryRepository, times(1)).save(queryCaptor.capture());
        Query saved = queryCaptor.getValue();
        assertThat(saved.getGlobalCount()).isEqualTo(102);
        assertThat(saved.getWeeklyCount()).isEqualTo(2); // reset to count
        assertThat(saved.getDailyCount()).isEqualTo(2);  // reset to count
    }

    @Test
    void testFlushRollbackOnException() {
        Map<String, Integer> snapshot = Map.of("iphone", 5);
        when(batchBuffer.getAndClear()).thenReturn(snapshot);
        when(queryRepository.findByQueryLower("iphone")).thenReturn(Optional.empty());
        doThrow(new RuntimeException("DB offline")).when(queryRepository).save(any());

        assertThatThrownBy(() -> batchWriter.flush())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB offline");
    }

    @Test
    void testRecovery() {
        UnbatchedQueryCount countStub = new UnbatchedQueryCountStub("java", 15L);
        when(searchLogRepository.findUnbatchedQueryCounts()).thenReturn(List.of(countStub));
        when(queryRepository.findByQueryLower("java")).thenReturn(Optional.empty());

        batchWriter.recover();

        verify(queryRepository, times(1)).save(any(Query.class));
        verify(searchLogRepository, times(1)).markLogsAsBatched(
                eq(List.of("java")),
                eq(LocalDateTime.now(clock))
        );
    }

    private static class UnbatchedQueryCountStub implements UnbatchedQueryCount {
        private final String queryLower;
        private final Long count;

        public UnbatchedQueryCountStub(String queryLower, Long count) {
            this.queryLower = queryLower;
            this.count = count;
        }

        @Override
        public String getQueryLower() {
            return queryLower;
        }

        @Override
        public Long getCount() {
            return count;
        }
    }
}
