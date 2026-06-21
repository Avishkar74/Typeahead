package com.typeahed.backend.service;

import com.typeahed.backend.batch.BatchBuffer;
import com.typeahed.backend.entity.SearchLog;
import com.typeahed.backend.repository.SearchLogRepository;
import com.typeahed.backend.virtualtime.VirtualTimeManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchService {

    private final SearchLogRepository searchLogRepository;
    private final BatchBuffer batchBuffer;
    private final VirtualTimeManager virtualTimeManager;

    public SearchService(SearchLogRepository searchLogRepository,
                         BatchBuffer batchBuffer,
                         VirtualTimeManager virtualTimeManager) {
        this.searchLogRepository = searchLogRepository;
        this.batchBuffer = batchBuffer;
        this.virtualTimeManager = virtualTimeManager;
    }

    @Transactional
    public void submitSearch(String queryText) {
        if (queryText == null) {
            throw new IllegalArgumentException("Query text cannot be null");
        }
        if (queryText.trim().isEmpty()) {
            throw new IllegalArgumentException("Query text cannot be blank");
        }
        if (queryText.length() > 255) {
            throw new IllegalArgumentException("Query text must not exceed 255 characters");
        }

        String normalized = queryText.toLowerCase().trim();

        // 1. Log query using virtual timestamp
        SearchLog log = new SearchLog();
        log.setQueryText(queryText);
        log.setQueryLower(normalized);
        log.setVirtualSearchedAt(virtualTimeManager.getVirtualTime());
        searchLogRepository.save(log);

        // 2. Add to in-memory batch write buffer only after successful transaction commit
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        batchBuffer.addSearch(queryText);
                    }
                }
            );
        } else {
            batchBuffer.addSearch(queryText);
        }
    }
}
