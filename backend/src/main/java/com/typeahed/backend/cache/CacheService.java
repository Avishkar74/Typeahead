package com.typeahed.backend.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);

    private final RedisClientFactory clientFactory;
    private final RedisNodeRouter router;
    private final ObjectMapper objectMapper;

    public CacheService(RedisClientFactory clientFactory, RedisNodeRouter router, ObjectMapper objectMapper) {
        this.clientFactory = clientFactory;
        this.router = router;
        this.objectMapper = objectMapper;
    }

    public Optional<CacheValue> get(String prefix, String rankingType) {
        if (prefix == null || rankingType == null) {
            return Optional.empty();
        }

        try {
            String node = router.route(prefix);
            RedisTemplate<String, String> template = clientFactory.getTemplate(node);
            String key = buildCacheKey(prefix, rankingType);
            String json = template.opsForValue().get(key);

            if (json == null) {
                return Optional.empty();
            }

            CacheValue cacheValue = objectMapper.readValue(json, CacheValue.class);
            return Optional.of(cacheValue);
        } catch (Exception e) {
            logger.warn("Failed to read cache for prefix '{}' and ranking type '{}': {}", prefix, rankingType, e.getMessage());
            return Optional.empty();
        }
    }

    public void put(String prefix, String rankingType, CacheValue value, Duration ttl) {
        if (prefix == null || rankingType == null || value == null) {
            return;
        }

        try {
            String node = router.route(prefix);
            RedisTemplate<String, String> template = clientFactory.getTemplate(node);
            String key = buildCacheKey(prefix, rankingType);
            String json = objectMapper.writeValueAsString(value);

            if (ttl != null) {
                template.opsForValue().set(key, json, ttl);
            } else {
                template.opsForValue().set(key, json);
            }
        } catch (Exception e) {
            logger.error("Failed to write to cache for prefix '{}' and ranking type '{}': {}", prefix, rankingType, e.getMessage(), e);
        }
    }

    public void delete(String prefix, String rankingType) {
        if (prefix == null || rankingType == null) {
            return;
        }

        try {
            String node = router.route(prefix);
            RedisTemplate<String, String> template = clientFactory.getTemplate(node);
            String key = buildCacheKey(prefix, rankingType);
            template.delete(key);
        } catch (Exception e) {
            logger.error("Failed to delete cache key for prefix '{}' and ranking type '{}': {}", prefix, rankingType, e.getMessage(), e);
        }
    }

    private String buildCacheKey(String prefix, String rankingType) {
        return "prefix:" + prefix.toLowerCase().trim() + ":" + rankingType.toLowerCase().trim();
    }
}
