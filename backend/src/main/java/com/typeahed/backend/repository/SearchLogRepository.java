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
}
