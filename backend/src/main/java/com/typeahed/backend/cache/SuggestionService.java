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
        String prefix = (query != null) ? query.toLowerCase().trim() : "";
        String ranking = (rankingType != null && rankingType.equalsIgnoreCase(RANKING_GLOBAL)) 
                ? RANKING_GLOBAL 
                : RANKING_TRENDING;

        // 1. Try cache hit
        Optional<CacheValue> cached = cacheService.get(prefix, ranking);
        if (cached.isPresent()) {
            return cached.get().getResults();
        }

        // 2. Cache miss -> Fetch from PostgreSQL
        List<Query> dbQueries;
        if (ranking.equals(RANKING_GLOBAL)) {
            dbQueries = queryRepository.findTop10ByQueryLowerStartingWithOrderByGlobalCountDesc(prefix);
        } else {
            dbQueries = queryRepository.findTop10ByQueryLowerStartingWithOrderByTrendingScoreDesc(prefix);
        }

        // 3. Map to SuggestionDto
        List<SuggestionDto> suggestions = dbQueries.stream()
                .map(q -> new SuggestionDto(
                        q.getQueryText(),
                        q.getGlobalCount(),
                        q.getWeeklyCount(),
                        q.getDailyCount(),
                        q.getTrendingScore()
                ))
                .collect(Collectors.toList());

        // 4. Populate cache
        LocalDateTime cachedAtTime = LocalDateTime.now(clock);
        LocalDateTime expiresAtTime = cachedAtTime.plus(CACHE_TTL);
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        CacheValue cacheValue = new CacheValue(
                prefix,
                ranking,
                suggestions,
                cachedAtTime.format(formatter) + "Z",
                expiresAtTime.format(formatter) + "Z"
        );

        cacheService.put(prefix, ranking, cacheValue, CACHE_TTL);

        return suggestions;
    }
}
