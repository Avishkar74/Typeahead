package com.typeahed.backend.cache;

import com.typeahed.backend.entity.Query;
import com.typeahed.backend.repository.QueryRepository;
import com.typeahed.backend.virtualtime.VirtualTimeManager;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SuggestionService {

    private static final String RANKING_TRENDING = "trending";
    private static final String RANKING_GLOBAL = "global";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final CacheService cacheService;
    private final QueryRepository queryRepository;
    private final VirtualTimeManager virtualTimeManager;
    private final Clock clock;

    public SuggestionService(CacheService cacheService,
                             QueryRepository queryRepository,
                             VirtualTimeManager virtualTimeManager,
                             Clock clock) {
        this.cacheService = cacheService;
        this.queryRepository = queryRepository;
        this.virtualTimeManager = virtualTimeManager;
        this.clock = clock;
    }

    public List<SuggestionDto> getSuggestions(String query, String rankingType) {
        String prefix = normalizePrefix(query);
        String ranking = (rankingType != null && rankingType.equalsIgnoreCase(RANKING_GLOBAL)) 
                ? RANKING_GLOBAL 
                : RANKING_TRENDING;

        if (!isSearchablePrefix(prefix)) {
            CacheContext.setCacheHit(false);
            return List.of();
        }

        // 1. Try cache hit
        Optional<CacheValue> cached = cacheService.get(prefix, ranking);
        if (cached.isPresent()) {
            CacheContext.setCacheHit(true);
            return cached.get().getResults();
        }

        CacheContext.setCacheHit(false);

        return loadAndCacheSuggestions(prefix, ranking);
    }

    public List<SuggestionDto> warmSuggestions(String query, String rankingType) {
        String prefix = normalizePrefix(query);
        String ranking = (rankingType != null && rankingType.equalsIgnoreCase(RANKING_GLOBAL))
                ? RANKING_GLOBAL
                : RANKING_TRENDING;

        if (!isSearchablePrefix(prefix)) {
            return List.of();
        }

        return loadAndCacheSuggestions(prefix, ranking);
    }

    public List<SuggestionDto> getTrendingOverall() {
        List<Query> dbQueries = queryRepository.findTop10ByOrderByTrendingScoreDesc();
        return dbQueries.stream()
                .map(q -> new SuggestionDto(
                        q.getQueryText(),
                        q.getGlobalCount(),
                        q.getWeeklyCount(),
                        q.getDailyCount(),
                        q.getTrendingScore()
                ))
                .collect(Collectors.toList());
    }

    private List<SuggestionDto> loadAndCacheSuggestions(String prefix, String ranking) {
        List<Query> dbQueries = fetchRankedQueries(prefix, ranking);
        List<SuggestionDto> suggestions = dbQueries.stream()
                .map(toSuggestionDto())
                .collect(Collectors.toList());

        cacheService.put(prefix, ranking, buildCacheValue(prefix, ranking, suggestions), CACHE_TTL);
        return suggestions;
    }

    private List<Query> fetchRankedQueries(String prefix, String ranking) {
        if (RANKING_GLOBAL.equals(ranking)) {
            return queryRepository.findTop10ByQueryLowerStartingWithOrderByGlobalCountDesc(prefix);
        }
        return queryRepository.findTop10ByQueryLowerStartingWithOrderByTrendingScoreDesc(prefix);
    }

    private Function<Query, SuggestionDto> toSuggestionDto() {
        return q -> new SuggestionDto(
                q.getQueryText(),
                q.getGlobalCount(),
                q.getWeeklyCount(),
                q.getDailyCount(),
                q.getTrendingScore()
        );
    }

    private CacheValue buildCacheValue(String prefix, String ranking, List<SuggestionDto> suggestions) {
        LocalDateTime cachedAtTime = LocalDateTime.now(clock);
        LocalDateTime expiresAtTime = cachedAtTime.plus(CACHE_TTL);
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        return new CacheValue(
                prefix,
                ranking,
                suggestions,
                cachedAtTime.format(formatter) + "Z",
                expiresAtTime.format(formatter) + "Z"
        );
    }

    private String normalizePrefix(String query) {
        return (query != null) ? query.toLowerCase().trim() : "";
    }

    private boolean isSearchablePrefix(String prefix) {
        return prefix != null && prefix.length() >= 3;
    }
}
