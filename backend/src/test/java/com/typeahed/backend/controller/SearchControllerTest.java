package com.typeahed.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typeahed.backend.dto.SearchRequestDto;
import com.typeahed.backend.exception.GlobalExceptionHandler;
import com.typeahed.backend.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SearchControllerTest {

    private SearchService searchService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        searchService = mock(SearchService.class);
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new SearchController(searchService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testSubmitSearchSuccess() throws Exception {
        SearchRequestDto request = new SearchRequestDto("iphone");

        mockMvc.perform(post("/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Searched"));

        verify(searchService, times(1)).submitSearch("iphone");
    }

    @Test
    void testSubmitSearchValidationBlankQuery() throws Exception {
        SearchRequestDto request = new SearchRequestDto("   ");

        mockMvc.perform(post("/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details[0]").value("query: Query text cannot be blank"));

        verifyNoInteractions(searchService);
    }

    @Test
    void testSubmitSearchValidationNullQuery() throws Exception {
        SearchRequestDto request = new SearchRequestDto(null);

        mockMvc.perform(post("/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));

        verifyNoInteractions(searchService);
    }
}
