package com.typeahed.backend.repository;

import com.typeahed.backend.entity.SearchLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    List<SearchLog> findByBatchedFalseOrderByCreatedAtAsc();

    List<SearchLog> findByBatchedFalse(Pageable pageable);

    List<SearchLog> findByQueryLower(String queryLower);

    List<SearchLog> findByVirtualSearchedAtAfter(LocalDateTime dateTime);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE SearchLog s SET s.batched = true, s.batchedAt = :batchedAt WHERE s.queryLower IN :queries AND s.batched = false")
    int markLogsAsBatched(List<String> queries, LocalDateTime batchedAt);

    @org.springframework.data.jpa.repository.Query("SELECT s.queryLower as queryLower, COUNT(s) as count FROM SearchLog s WHERE s.batched = false GROUP BY s.queryLower")
    List<UnbatchedQueryCount> findUnbatchedQueryCounts();
}
