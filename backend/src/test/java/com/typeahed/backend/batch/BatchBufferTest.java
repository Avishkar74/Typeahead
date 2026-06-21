package com.typeahed.backend.batch;

import com.typeahed.backend.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BatchBufferTest {

    private AppProperties appProperties;
    private MutableClock clock;
    private BatchBuffer batchBuffer;

    @BeforeEach
    void setUp() {
        appProperties = mock(AppProperties.class);
        AppProperties.Batch batchConfig = mock(AppProperties.Batch.class);
        when(batchConfig.getBufferSizeThreshold()).thenReturn(3);
        when(batchConfig.getFlushIntervalSeconds()).thenReturn(5);
        when(appProperties.getBatch()).thenReturn(batchConfig);

        clock = new MutableClock(Instant.parse("2026-06-22T00:00:00Z"), ZoneId.systemDefault());
        batchBuffer = new BatchBuffer(appProperties, clock);
    }

    @Test
    void testAddSearchAndAggregation() {
        batchBuffer.addSearch("iphone");
        batchBuffer.addSearch("IPHONE ");
        batchBuffer.addSearch("java  ");

        Map<String, Integer> snapshot = batchBuffer.getAndClear();
        assertThat(snapshot).hasSize(2);
        assertThat(snapshot.get("iphone")).isEqualTo(2);
        assertThat(snapshot.get("java")).isEqualTo(1);
        assertThat(batchBuffer.getBufferSize()).isEqualTo(0);
    }

    @Test
    void testShouldFlushSizeThreshold() {
        assertThat(batchBuffer.shouldFlush()).isFalse();

        batchBuffer.addSearch("q1");
        batchBuffer.addSearch("q2");
        assertThat(batchBuffer.shouldFlush()).isFalse();

        batchBuffer.addSearch("q3");
        assertThat(batchBuffer.shouldFlush()).isTrue();
    }

    @Test
    void testShouldFlushTimeInterval() {
        assertThat(batchBuffer.shouldFlush()).isFalse();

        batchBuffer.addSearch("q1");
        assertThat(batchBuffer.shouldFlush()).isFalse();

        // Advance clock by 5 seconds (the threshold is 5)
        clock.advance(Duration.ofSeconds(5));
        assertThat(batchBuffer.shouldFlush()).isTrue();
    }

    @Test
    void testGetAndClearResetsTime() {
        batchBuffer.addSearch("q1");
        clock.advance(Duration.ofSeconds(5));
        assertThat(batchBuffer.shouldFlush()).isTrue();

        Map<String, Integer> snapshot = batchBuffer.getAndClear();
        assertThat(snapshot).hasSize(1);
        assertThat(batchBuffer.shouldFlush()).isFalse();
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        int threads = 10;
        int searchesPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < searchesPerThread; j++) {
                        batchBuffer.addSearch("query_" + j);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        Map<String, Integer> snapshot = batchBuffer.getAndClear();
        assertThat(snapshot).hasSize(searchesPerThread);
        for (int j = 0; j < searchesPerThread; j++) {
            assertThat(snapshot.get("query_" + j)).isEqualTo(threads);
        }
    }

    private static class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        public MutableClock(Instant start, ZoneId zone) {
            this.instant = start;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        public void advance(Duration duration) {
            this.instant = this.instant.plus(duration);
        }
    }
}
