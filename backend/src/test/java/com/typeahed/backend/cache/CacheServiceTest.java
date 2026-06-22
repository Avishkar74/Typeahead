package com.typeahed.backend.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CacheServiceTest {

    private RedisClientFactory clientFactory;
    private RedisNodeRouter router;
    private ObjectMapper objectMapper;
    private CacheService cacheService;

    private RedisTemplate<String, String> redisTemplate;
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        clientFactory = mock(RedisClientFactory.class);
        router = mock(RedisNodeRouter.class);
        objectMapper = new ObjectMapper(); // Use real Jackson ObjectMapper for serialization tests

        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(router.route(anyString())).thenReturn("localhost:6379");
        when(clientFactory.getTemplate("localhost:6379")).thenReturn(redisTemplate);

        cacheService = new CacheService(clientFactory, router, objectMapper);
    }

    @Test
    void testGetCacheHit() throws Exception {
        String jsonPayload = "{\"prefix\":\"iph\",\"ranking_type\":\"trending\",\"results\":[" +
                "{\"query\":\"iphone\",\"global_count\":10,\"weekly_count\":5,\"daily_count\":2,\"trending_score\":6.5}" +
                "],\"cached_at\":\"2006-06-01T10:00:00Z\",\"expires_at\":\"2006-06-01T11:00:00Z\"}";

        when(valueOperations.get("prefix:iph:trending")).thenReturn(jsonPayload);

        Optional<CacheValue> result = cacheService.get("iph", "trending");

        assertThat(result).isPresent();
        CacheValue val = result.get();
        assertThat(val.getPrefix()).isEqualTo("iph");
        assertThat(val.getRankingType()).isEqualTo("trending");
        assertThat(val.getCachedAt()).isEqualTo("2006-06-01T10:00:00Z");
        assertThat(val.getResults()).hasSize(1);

        SuggestionDto dto = val.getResults().get(0);
        assertThat(dto.getQuery()).isEqualTo("iphone");
        assertThat(dto.getGlobalCount()).isEqualTo(10);
        assertThat(dto.getWeeklyCount()).isEqualTo(5);
        assertThat(dto.getDailyCount()).isEqualTo(2);
        assertThat(dto.getTrendingScore()).isEqualByComparingTo(BigDecimal.valueOf(6.5));
    }

    @Test
    void testGetCacheMiss() {
        when(valueOperations.get("prefix:iph:trending")).thenReturn(null);

        Optional<CacheValue> result = cacheService.get("iph", "trending");

        assertThat(result).isEmpty();
    }

    @Test
    void testGetSingleCharacterPrefixIsRejected() {
        Optional<CacheValue> result = cacheService.get("g", "trending");

        assertThat(result).isEmpty();
        verifyNoInteractions(redisTemplate, valueOperations);
    }

    @Test
    void testGetTwoCharacterPrefixIsRejected() {
        Optional<CacheValue> result = cacheService.get("go", "trending");

        assertThat(result).isEmpty();
        verifyNoInteractions(redisTemplate, valueOperations);
    }

    @Test
    void testPutCacheWrite() throws Exception {
        SuggestionDto dto = new SuggestionDto("iphone", 10, 5, 2, BigDecimal.valueOf(6.5));
        CacheValue value = new CacheValue("iph", "trending", List.of(dto), "2006-06-01T10:00:00Z", "2006-06-01T11:00:00Z");

        cacheService.put("iph", "trending", value, Duration.ofHours(1));

        verify(valueOperations, times(1)).set(
                eq("prefix:iph:trending"),
                contains("\"query\":\"iphone\""),
                eq(Duration.ofHours(1))
        );
    }

    @Test
    void testPutSingleCharacterPrefixIsNoOp() throws Exception {
        SuggestionDto dto = new SuggestionDto("google", 10, 5, 2, BigDecimal.valueOf(6.5));
        CacheValue value = new CacheValue("g", "trending", List.of(dto), "2006-06-01T10:00:00Z", "2006-06-01T11:00:00Z");

        cacheService.put("g", "trending", value, Duration.ofHours(1));

        verifyNoInteractions(redisTemplate, valueOperations);
    }

    @Test
    void testPutTwoCharacterPrefixIsNoOp() throws Exception {
        SuggestionDto dto = new SuggestionDto("google", 10, 5, 2, BigDecimal.valueOf(6.5));
        CacheValue value = new CacheValue("go", "trending", List.of(dto), "2006-06-01T10:00:00Z", "2006-06-01T11:00:00Z");

        cacheService.put("go", "trending", value, Duration.ofHours(1));

        verifyNoInteractions(redisTemplate, valueOperations);
    }

    @Test
    void testDeleteCacheKey() {
        cacheService.delete("iph", "trending");

        verify(redisTemplate, times(1)).delete("prefix:iph:trending");
    }

    @Test
    void testDeleteSingleCharacterPrefixIsNoOp() {
        cacheService.delete("g", "trending");

        verifyNoInteractions(redisTemplate, valueOperations);
    }

    @Test
    void testDeleteTwoCharacterPrefixIsNoOp() {
        cacheService.delete("go", "trending");

        verifyNoInteractions(redisTemplate, valueOperations);
    }
}
