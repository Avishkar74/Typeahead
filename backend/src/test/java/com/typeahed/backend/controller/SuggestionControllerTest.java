package com.typeahed.backend.controller;

import com.typeahed.backend.cache.CacheContext;
import com.typeahed.backend.cache.SuggestionDto;
import com.typeahed.backend.cache.SuggestionService;
import com.typeahed.backend.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SuggestionControllerTest {

    private SuggestionService suggestionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        suggestionService = mock(SuggestionService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SuggestionController(suggestionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetSuggestionsSuccessCacheHit() throws Exception {
        SuggestionDto dto = new SuggestionDto("iphone", 10, 5, 2, BigDecimal.valueOf(6.5));
        
        // Mock suggestion results and set CacheContext ThreadLocal
        when(suggestionService.getSuggestions("iph", "trending")).thenAnswer(inv -> {
            CacheContext.setCacheHit(true);
            return List.of(dto);
        });

        mockMvc.perform(get("/suggest")
                        .param("q", "iph")
                        .param("ranking", "trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prefix").value("iph"))
                .andExpect(jsonPath("$.ranking_type").value("trending"))
                .andExpect(jsonPath("$.cache_hit").value(true))
                .andExpect(jsonPath("$.results[0].query").value("iphone"));

        verify(suggestionService, times(1)).getSuggestions("iph", "trending");
    }

    @Test
    void testGetSuggestionsSuccessCacheMiss() throws Exception {
        SuggestionDto dto = new SuggestionDto("iphone", 10, 5, 2, BigDecimal.valueOf(6.5));

        when(suggestionService.getSuggestions("iph", "global")).thenAnswer(inv -> {
            CacheContext.setCacheHit(false);
            return List.of(dto);
        });

        mockMvc.perform(get("/suggest")
                        .param("q", "iph")
                        .param("ranking", "global"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cache_hit").value(false))
                .andExpect(jsonPath("$.ranking_type").value("global"));
    }

    @Test
    void testGetSuggestionsInvalidRanking() throws Exception {
        mockMvc.perform(get("/suggest")
                        .param("q", "iph")
                        .param("ranking", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Argument"))
                .andExpect(jsonPath("$.message").value("Invalid ranking type: must be 'trending' or 'global'"));

        verifyNoInteractions(suggestionService);
    }

    @Test
    void testGetTrendingOverallSuccess() throws Exception {
        SuggestionDto dto = new SuggestionDto("iphone", 10, 5, 2, BigDecimal.valueOf(6.5));
        when(suggestionService.getTrendingOverall()).thenReturn(List.of(dto));

        mockMvc.perform(get("/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].query").value("iphone"));

        verify(suggestionService, times(1)).getTrendingOverall();
    }
}
