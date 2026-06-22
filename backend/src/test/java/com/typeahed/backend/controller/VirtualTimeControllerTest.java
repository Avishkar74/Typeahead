package com.typeahed.backend.controller;

import com.typeahed.backend.config.AppProperties;
import com.typeahed.backend.exception.GlobalExceptionHandler;
import com.typeahed.backend.virtualtime.VirtualTimeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VirtualTimeControllerTest {

    private VirtualTimeManager virtualTimeManager;
    private AppProperties appProperties;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        virtualTimeManager = mock(VirtualTimeManager.class);
        appProperties = new AppProperties();
        mockMvc = MockMvcBuilders.standaloneSetup(new VirtualTimeController(virtualTimeManager, appProperties))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetVirtualTimeSuccess() throws Exception {
        LocalDateTime referenceTime = LocalDateTime.of(2006, 5, 31, 23, 59, 56);
        LocalDateTime currentVirtualTime = LocalDateTime.of(2006, 6, 1, 12, 34, 56);
        appProperties.getVirtualTime().setReferenceDate(referenceTime.toString());

        when(virtualTimeManager.getVirtualTime()).thenReturn(currentVirtualTime);
        when(virtualTimeManager.getSavedVirtualTime()).thenReturn(referenceTime);
        when(virtualTimeManager.getElapsedRealSeconds()).thenReturn(12345L);

        mockMvc.perform(get("/api/virtual-time"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.virtual_time").value(currentVirtualTime.toString()))
                .andExpect(jsonPath("$.reference_time").value(referenceTime.toString()))
                .andExpect(jsonPath("$.real_time_elapsed_seconds").value(12345))
                .andExpect(jsonPath("$.batch_flush_increment_seconds").value(60));

        verify(virtualTimeManager, times(1)).getVirtualTime();
        verify(virtualTimeManager, times(1)).getSavedVirtualTime();
        verify(virtualTimeManager, times(1)).getElapsedRealSeconds();
    }
}
