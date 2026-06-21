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
        if (queryText == null || queryText.trim().isEmpty()) {
            throw new IllegalArgumentException("Query text cannot be null or empty");
        }

        String normalized = queryText.toLowerCase().trim();

        // 1. Log query using virtual timestamp (as documented in VIRTUAL_TIME_MANAGEMENT.md)
        SearchLog log = new SearchLog();
        log.setQueryText(queryText);
        log.setQueryLower(normalized);
        log.setVirtualSearchedAt(virtualTimeManager.getVirtualTime());
        searchLogRepository.save(log);

        // 2. Add to in-memory batch write buffer
        batchBuffer.addSearch(queryText);
    }
}
