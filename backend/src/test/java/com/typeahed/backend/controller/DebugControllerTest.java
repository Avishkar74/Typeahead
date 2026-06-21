package com.typeahed.backend.controller;

import com.typeahed.backend.cache.CacheService;
import com.typeahed.backend.cache.CacheValue;
import com.typeahed.backend.cache.ConsistentHashRing;
import com.typeahed.backend.cache.RedisNodeRouter;
import com.typeahed.backend.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DebugControllerTest {

    private RedisNodeRouter redisNodeRouter;
    private CacheService cacheService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        redisNodeRouter = mock(RedisNodeRouter.class);
        cacheService = mock(CacheService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new DebugController(redisNodeRouter, cacheService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testDebugCacheHit() throws Exception {
        ConsistentHashRing hashRing = mock(ConsistentHashRing.class);
        when(redisNodeRouter.getHashRing()).thenReturn(hashRing);
        when(redisNodeRouter.route("iph")).thenReturn("localhost:6381");
        when(hashRing.getSlot("iph")).thenReturn(12000);
        
        CacheValue mockValue = new CacheValue();
        when(cacheService.get("iph", "trending")).thenReturn(Optional.of(mockValue));

        mockMvc.perform(get("/api/debug/cache")
                        .param("prefix", "iph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prefix").value("iph"))
                .andExpect(jsonPath("$.responsible_node").value("localhost:6381"))
                .andExpect(jsonPath("$.slot").value(12000))
                .andExpect(jsonPath("$.cache_hit").value(true));

        verify(redisNodeRouter, times(1)).route("iph");
        verify(cacheService, times(1)).get("iph", "trending");
    }

    @Test
    void testDebugCacheMiss() throws Exception {
        ConsistentHashRing hashRing = mock(ConsistentHashRing.class);
        when(redisNodeRouter.getHashRing()).thenReturn(hashRing);
        when(redisNodeRouter.route("iph")).thenReturn("localhost:6381");
        when(hashRing.getSlot("iph")).thenReturn(12000);
        
        when(cacheService.get("iph", "trending")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/debug/cache")
                        .param("prefix", "iph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cache_hit").value(false));
    }
}
