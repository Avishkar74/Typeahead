package com.typeahed.backend;

import com.typeahed.backend.entity.Query;
import com.typeahed.backend.repository.QueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisabledIf("com.typeahed.backend.QueryRepositoryTest#isDockerNotAvailable")
class QueryRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private QueryRepository queryRepository;

    @BeforeEach
    void setUp() {
        queryRepository.deleteAll();

        // Seed data
        saveQuery("iphone", 1000, 100, 10, new BigDecimal("600.00"));
        saveQuery("iphone charger", 500, 50, 5, new BigDecimal("350.00"));
        saveQuery("iphone 15", 800, 80, 8, new BigDecimal("550.00"));
        saveQuery("ipad", 400, 40, 4, new BigDecimal("250.00"));
        saveQuery("java", 300, 30, 3, new BigDecimal("150.00"));
    }

    private void saveQuery(String text, int global, int weekly, int daily, BigDecimal score) {
        Query query = new Query();
        query.setQueryText(text);
        query.setGlobalCount(global);
        query.setWeeklyCount(weekly);
        query.setDailyCount(daily);
        query.setTrendingScore(score);
        queryRepository.save(query);
    }

    @Test
    void testPrefixMatchingAndSortingByTrendingScore() {
        List<Query> results = queryRepository.findTop10ByQueryLowerStartingWithOrderByTrendingScoreDesc("iph");
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getQueryText()).isEqualTo("iphone");
        assertThat(results.get(1).getQueryText()).isEqualTo("iphone 15");
        assertThat(results.get(2).getQueryText()).isEqualTo("iphone charger");
    }

    @Test
    void testPrefixMatchingAndSortingByGlobalCount() {
        List<Query> results = queryRepository.findTop10ByQueryLowerStartingWithOrderByGlobalCountDesc("iph");
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getQueryText()).isEqualTo("iphone");
        assertThat(results.get(1).getQueryText()).isEqualTo("iphone 15");
        assertThat(results.get(2).getQueryText()).isEqualTo("iphone charger");
    }

    @Test
    void testFindByQueryLower() {
        assertThat(queryRepository.findByQueryLower("iphone")).isPresent();
        assertThat(queryRepository.findByQueryLower("nonexistent")).isEmpty();
    }

    static boolean isDockerNotAvailable() {
        try {
            return !org.testcontainers.DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            return true;
        }
    }
}
