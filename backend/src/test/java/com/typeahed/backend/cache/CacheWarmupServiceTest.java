package com.typeahed.backend.cache;

import com.typeahed.backend.config.AppProperties;
import com.typeahed.backend.entity.Query;
import com.typeahed.backend.repository.QueryRepository;
import com.typeahed.backend.virtualtime.VirtualTimeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CacheWarmupServiceTest {

    private AppProperties appProperties;
    private CacheService cacheService;
    private QueryRepository queryRepository;
    private VirtualTimeManager virtualTimeManager;
    private Clock clock;
    private SuggestionService suggestionService;
    private CacheWarmupService cacheWarmupService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        cacheService = mock(CacheService.class);
        queryRepository = mock(QueryRepository.class);
        virtualTimeManager = mock(VirtualTimeManager.class);
        clock = Clock.fixed(Instant.parse("2026-06-22T12:00:00Z"), ZoneId.of("UTC"));
        suggestionService = new SuggestionService(cacheService, queryRepository, virtualTimeManager, clock);
        cacheWarmupService = new CacheWarmupService(appProperties, queryRepository, suggestionService);
    }

    @Test
    void warmsUniquePrefixesForBothRankingsWithinConfiguredPrefixBounds() {
        appProperties.getCacheWarmup().setEnabled(true);
        appProperties.getCacheWarmup().setMaxQueries(50000);
        appProperties.getCacheWarmup().setMaxPrefixLength(4);

        Query history = query("history", 1000, 100, 10, "900.0");
        Query historical = query("historical", 800, 80, 8, "700.0");
        Pageable pageable = PageRequest.of(0, 50000, Sort.by(Sort.Direction.DESC, "globalCount"));
        when(queryRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(history, historical), pageable, 2));

        Query prefixResult = query("history", 1000, 100, 10, "900.0");
        when(queryRepository.findTop10ByQueryLowerStartingWithOrderByTrendingScoreDesc(anyString())).thenReturn(List.of(prefixResult));
        when(queryRepository.findTop10ByQueryLowerStartingWithOrderByGlobalCountDesc(anyString())).thenReturn(List.of(prefixResult));

        cacheWarmupService.warmCacheOnStartup();

        ArgumentCaptor<String> prefixCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> rankingCaptor = ArgumentCaptor.forClass(String.class);
        verify(cacheService, times(4)).put(prefixCaptor.capture(), rankingCaptor.capture(), any(CacheValue.class), any(java.time.Duration.class));

        List<String> prefixes = prefixCaptor.getAllValues();
        assertThat(prefixes).containsExactlyInAnyOrder("his", "hist", "his", "hist");
        assertThat(new LinkedHashSet<>(prefixes)).containsExactly("his", "hist");

        List<String> rankings = rankingCaptor.getAllValues();
        assertThat(rankings).containsExactlyInAnyOrder("trending", "global", "trending", "global");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(queryRepository).findAll(pageableCaptor.capture());
        Pageable requested = pageableCaptor.getValue();
        assertThat(requested.getPageSize()).isEqualTo(50000);
        assertThat(requested.getSort().getOrderFor("globalCount")).isNotNull();
        assertThat(requested.getSort().getOrderFor("globalCount").isDescending()).isTrue();
    }

    @Test
    void disabledWarmupSkipsExecution() {
        appProperties.getCacheWarmup().setEnabled(false);

        cacheWarmupService.warmCacheOnStartup();

        verifyNoInteractions(queryRepository, cacheService);
    }

    private Query query(String text, int global, int weekly, int daily, String score) {
        Query query = new Query();
        query.setQueryText(text);
        query.setGlobalCount(global);
        query.setWeeklyCount(weekly);
        query.setDailyCount(daily);
        query.setTrendingScore(new BigDecimal(score));
        return query;
    }
}
