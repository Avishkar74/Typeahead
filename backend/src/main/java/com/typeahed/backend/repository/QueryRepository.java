package com.typeahed.backend.repository;

import com.typeahed.backend.entity.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QueryRepository extends JpaRepository<Query, Long> {

    List<Query> findTop10ByQueryLowerStartingWithOrderByTrendingScoreDesc(String prefix);

    List<Query> findTop10ByQueryLowerStartingWithOrderByGlobalCountDesc(String prefix);

    Optional<Query> findByQueryLower(String queryLower);
}
