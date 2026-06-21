package com.typeahed.backend.cache;

import com.typeahed.backend.entity.Query;
import com.typeahed.backend.repository.QueryRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SuggestionServiceTest {

    private CacheService cacheService;
    private QueryRepository queryRepository;
    private VirtualTimeManager virtualTimeManager;
    private Clock clock;
    private SuggestionService suggestionService;

    @BeforeEach
    void setUp() {
        cacheService = mock(CacheService.class);
        queryRepository = mock(QueryRepository.class);
        virtualTimeManager = mock(VirtualTimeManager.class);
        clock = Clock.fixed(Instant.parse("2026-06-22T12:00:00Z"), ZoneId.of("UTC"));
        suggestionService = new SuggestionService(cacheService, queryRepository, virtualTimeManager, clock);

        when(virtualTimeManager.getVirtualTime()).thenReturn(LocalDateTime.of(2006, 6, 1, 10, 0, 0));
    }

    @Test
    void testGetSuggestionsCacheHit() {
        SuggestionDto cachedDto = new SuggestionDto("iphone", 100, 20, 5, BigDecimal.valueOf(65.5));
        CacheValue cachedValue = new CacheValue("iph", "trending", List.of(cachedDto), "2006-06-01T10:00:00Z", "2006-06-01T11:00:00Z");

        when(cacheService.get("iph", "trending")).thenReturn(Optional.of(cachedValue));

        List<SuggestionDto> suggestions = suggestionService.getSuggestions("iph", "trending");

        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.get(0).getQuery()).isEqualTo("iphone");
        assertThat(suggestions.get(0).getGlobalCount()).isEqualTo(100);

        verifyNoInteractions(queryRepository);
        verify(cacheService, never()).put(anyString(), anyString(), any(CacheValue.class), any(Duration.class));
    }

    @Test
    void testGetSuggestionsCacheMissTrending() {
        when(cacheService.get("iph", "trending")).thenReturn(Optional.empty());

        Query q = new Query();
        q.setQueryText("iphone");
        q.setQueryLower("iphone");
        q.setGlobalCount(100);
        q.setWeeklyCount(20);
        q.setDailyCount(5);
        q.setTrendingScore(BigDecimal.valueOf(65.5));

        when(queryRepository.findTop10ByQueryLowerStartingWithOrderByTrendingScoreDesc("iph")).thenReturn(List.of(q));

        List<SuggestionDto> suggestions = suggestionService.getSuggestions("iph", "trending");

        assertThat(suggestions).hasSize(1);
        SuggestionDto result = suggestions.get(0);
        assertThat(result.getQuery()).isEqualTo("iphone");
        assertThat(result.getTrendingScore()).isEqualByComparingTo(BigDecimal.valueOf(65.5));

        // Verify PostgreSQL query occurred
        verify(queryRepository, times(1)).findTop10ByQueryLowerStartingWithOrderByTrendingScoreDesc("iph");

        // Verify cache populated with 1-hour TTL
        ArgumentCaptor<CacheValue> valueCaptor = ArgumentCaptor.forClass(CacheValue.class);
        verify(cacheService, times(1)).put(
                eq("iph"),
                eq("trending"),
                valueCaptor.capture(),
                eq(Duration.ofHours(1))
        );

        CacheValue savedVal = valueCaptor.getValue();
        assertThat(savedVal.getPrefix()).isEqualTo("iph");
        assertThat(savedVal.getRankingType()).isEqualTo("trending");
        assertThat(savedVal.getCachedAt()).isEqualTo("2026-06-22T12:00:00Z");
        assertThat(savedVal.getExpiresAt()).isEqualTo("2026-06-22T13:00:00Z");
        assertThat(savedVal.getResults()).hasSize(1);
    }

    @Test
    void testGetSuggestionsCacheMissGlobal() {
        when(cacheService.get("iph", "global")).thenReturn(Optional.empty());

        Query q = new Query();
        q.setQueryText("iphone");
        q.setQueryLower("iphone");
        q.setGlobalCount(100);
        q.setWeeklyCount(20);
        q.setDailyCount(5);
        q.setTrendingScore(BigDecimal.valueOf(65.5));

        when(queryRepository.findTop10ByQueryLowerStartingWithOrderByGlobalCountDesc("iph")).thenReturn(List.of(q));

        List<SuggestionDto> suggestions = suggestionService.getSuggestions("iph", "global");

        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.get(0).getGlobalCount()).isEqualTo(100);

        verify(queryRepository, times(1)).findTop10ByQueryLowerStartingWithOrderByGlobalCountDesc("iph");

        verify(cacheService, times(1)).put(
                eq("iph"),
                eq("global"),
                any(CacheValue.class),
                eq(Duration.ofHours(1))
        );
    }
}
