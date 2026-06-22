package com.typeahed.backend.cache;

import com.typeahed.backend.config.AppProperties;
import com.typeahed.backend.entity.Query;
import com.typeahed.backend.repository.QueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class CacheWarmupService {

    private static final Logger logger = LoggerFactory.getLogger(CacheWarmupService.class);
    private static final String RANKING_TRENDING = "trending";
    private static final String RANKING_GLOBAL = "global";

    private final AppProperties appProperties;
    private final QueryRepository queryRepository;
    private final SuggestionService suggestionService;

    public CacheWarmupService(AppProperties appProperties,
                              QueryRepository queryRepository,
                              SuggestionService suggestionService) {
        this.appProperties = appProperties;
        this.queryRepository = queryRepository;
        this.suggestionService = suggestionService;
    }

    @Order(Ordered.LOWEST_PRECEDENCE)
    @EventListener(ApplicationReadyEvent.class)
    public void warmCacheOnStartup() {
        if (!appProperties.getCacheWarmup().isEnabled()) {
            logger.info("Cache warmup is disabled by configuration (app.cache.warmup.enabled=false). Skipping warmup.");
            return;
        }

        long startNanos = System.nanoTime();
        int maxQueries = appProperties.getCacheWarmup().getMaxQueries();
        int maxPrefixLength = appProperties.getCacheWarmup().getMaxPrefixLength();

        logger.info("Cache warmup started...");

        Page<Query> queryPage = queryRepository.findAll(PageRequest.of(
                0,
                maxQueries,
                Sort.by(Sort.Direction.DESC, "globalCount")
        ));

        List<Query> queries = queryPage.getContent();
        Set<String> prefixes = new LinkedHashSet<>();

        for (Query query : queries) {
            collectPrefixes(prefixes, query.getQueryLower(), maxPrefixLength);
        }

        int entriesCached = 0;
        for (String prefix : prefixes) {
            suggestionService.warmSuggestions(prefix, RANKING_TRENDING);
            suggestionService.warmSuggestions(prefix, RANKING_GLOBAL);
            entriesCached += 2;
        }

        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;
        logger.info("Scanned queries: {}", queries.size());
        logger.info("Unique prefixes: {}", prefixes.size());
        logger.info("Cache entries written: {}", entriesCached);
        logger.info("Elapsed: {}s", String.format(java.util.Locale.US, "%.1f", elapsedMillis / 1000.0));
        logger.info("Cache warmup complete");
    }

    private void collectPrefixes(Set<String> prefixes, String queryLower, int maxPrefixLength) {
        if (queryLower == null) {
            return;
        }

        String normalized = queryLower.trim();
        int upperBound = Math.min(maxPrefixLength, normalized.length());

        for (int length = 3; length <= upperBound; length++) {
            prefixes.add(normalized.substring(0, length));
        }
    }
}
