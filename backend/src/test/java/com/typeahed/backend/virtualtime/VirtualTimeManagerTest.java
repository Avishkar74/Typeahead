package com.typeahed.backend.virtualtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class VirtualTimeManagerTest {

    private VirtualTimePersistence persistence;
    private MutableClock mutableClock;
    private VirtualTimeManager manager;

    @BeforeEach
    void setUp() {
        persistence = mock(VirtualTimePersistence.class);
        mutableClock = new MutableClock(Instant.parse("2026-06-22T00:00:00Z"), ZoneId.systemDefault());
        manager = new VirtualTimeManager(persistence, mutableClock);
    }

    @Test
    void testInitLoadsSavedVirtualTime() {
        LocalDateTime savedTime = LocalDateTime.of(2006, 6, 1, 10, 0, 0);
        when(persistence.load()).thenReturn(savedTime);

        manager.init();

        assertThat(manager.getSavedVirtualTime()).isEqualTo(savedTime);
        assertThat(manager.getAppStartRealTime()).isEqualTo(LocalDateTime.now(mutableClock));
        // Since no real time has elapsed, current virtual time should be exactly savedTime
        assertThat(manager.getVirtualTime()).isEqualTo(savedTime);
        verify(persistence, times(1)).load();
    }

    @Test
    void testVirtualTimeProgression() {
        LocalDateTime savedTime = LocalDateTime.of(2006, 6, 1, 10, 0, 0);
        when(persistence.load()).thenReturn(savedTime);

        manager.init();

        // Simulate 5 minutes passing by advancing the mutable clock
        mutableClock.advance(Duration.ofMinutes(5));

        // Get virtual time, should have advanced by 5 minutes
        LocalDateTime virtualTime = manager.getVirtualTime();
        assertThat(Duration.between(savedTime, virtualTime).toMinutes()).isEqualTo(5);
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
