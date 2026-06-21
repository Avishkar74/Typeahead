package com.typeahed.backend.service;

import com.typeahed.backend.batch.BatchBuffer;
import com.typeahed.backend.entity.SearchLog;
import com.typeahed.backend.repository.SearchLogRepository;
import com.typeahed.backend.virtualtime.VirtualTimeManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SearchServiceTest {

    private SearchLogRepository searchLogRepository;
    private BatchBuffer batchBuffer;
    private VirtualTimeManager virtualTimeManager;
    private SearchService searchService;

    @BeforeEach
    void setUp() {
        searchLogRepository = mock(SearchLogRepository.class);
        batchBuffer = mock(BatchBuffer.class);
        virtualTimeManager = mock(VirtualTimeManager.class);
        searchService = new SearchService(searchLogRepository, batchBuffer, virtualTimeManager);

        when(virtualTimeManager.getVirtualTime()).thenReturn(LocalDateTime.of(2006, 5, 31, 12, 0));
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void testSubmitSearchSuccessfulWithoutTransaction() {
        searchService.submitSearch("apple");

        verify(searchLogRepository, times(1)).save(any(SearchLog.class));
        verify(batchBuffer, times(1)).addSearch("apple");
    }

    @Test
    void testSubmitSearchSuccessfulWithTransactionCommit() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        searchService.submitSearch("apple");

        verify(searchLogRepository, times(1)).save(any(SearchLog.class));
        verify(batchBuffer, never()).addSearch("apple");

        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertThat(synchronizations).hasSize(1);
        synchronizations.get(0).afterCommit();

        verify(batchBuffer, times(1)).addSearch("apple");
    }

    @Test
    void testSubmitSearchFailedTransactionRollback() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        searchService.submitSearch("apple");

        verify(searchLogRepository, times(1)).save(any(SearchLog.class));
        verify(batchBuffer, never()).addSearch("apple");

        TransactionSynchronizationManager.clear();

        verify(batchBuffer, never()).addSearch("apple");
    }

    @Test
    void testSubmitSearchDefensiveValidation() {
        assertThatThrownBy(() -> searchService.submitSearch(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Query text cannot be null");

        assertThatThrownBy(() -> searchService.submitSearch("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Query text cannot be blank");

        String longQuery = "a".repeat(256);
        assertThatThrownBy(() -> searchService.submitSearch(longQuery))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Query text must not exceed 255 characters");
    }
}
