package com.typeahed.backend;

import com.typeahed.backend.entity.SearchLog;
import com.typeahed.backend.repository.SearchLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisabledIf("com.typeahed.backend.SearchLogRepositoryTest#isDockerNotAvailable")
class SearchLogRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private SearchLogRepository searchLogRepository;

    @BeforeEach
    void setUp() {
        searchLogRepository.deleteAll();

        // Seed logs
        saveLog("iphone", LocalDateTime.of(2006, 5, 31, 12, 0), false);
        saveLog("java", LocalDateTime.of(2006, 5, 31, 13, 0), false);
        saveLog("iphone", LocalDateTime.of(2006, 5, 31, 14, 0), true);
        saveLog("ipad", LocalDateTime.of(2006, 5, 31, 15, 0), false);
    }

    private void saveLog(String text, LocalDateTime virtualTime, boolean batched) {
        SearchLog log = new SearchLog();
        log.setQueryText(text);
        log.setVirtualSearchedAt(virtualTime);
        log.setBatched(batched);
        searchLogRepository.save(log);
    }

    @Test
    void testFindUnbatchedLogs() {
        List<SearchLog> unbatched = searchLogRepository.findByBatchedFalseOrderByCreatedAtAsc();
        assertThat(unbatched).hasSize(3);
    }

    @Test
    void testFindUnbatchedLogsWithPageable() {
        List<SearchLog> unbatched = searchLogRepository.findByBatchedFalse(PageRequest.of(0, 2));
        assertThat(unbatched).hasSize(2);
    }

    @Test
    void testFindByQueryLower() {
        List<SearchLog> logs = searchLogRepository.findByQueryLower("iphone");
        assertThat(logs).hasSize(2);
    }

    @Test
    void testFindByVirtualSearchedAtAfter() {
        List<SearchLog> logs = searchLogRepository.findByVirtualSearchedAtAfter(LocalDateTime.of(2006, 5, 31, 13, 30));
        assertThat(logs).hasSize(2); // iphone (14:00) and ipad (15:00)
    }

    static boolean isDockerNotAvailable() {
        try {
            return !org.testcontainers.DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            return true;
        }
    }
}
