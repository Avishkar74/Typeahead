package com.typeahed.backend;

import com.typeahed.backend.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@DisabledIf("com.typeahed.backend.BackendApplicationTests#isDockerNotAvailable")
class BackendApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("app.redis.nodes", () -> List.of(redis.getHost() + ":" + redis.getFirstMappedPort()));
    }

    @Autowired
    private AppProperties appProperties;

    @Test
    void contextLoads() {
        // Verifies that the Spring application context loads successfully with database and redis containers.
    }

    @Test
    void testConfigurationLoading() {
        assertThat(appProperties).isNotNull();
        assertThat(appProperties.getBatch().getFlushIntervalSeconds()).isEqualTo(30);
        assertThat(appProperties.getBatch().getBufferSizeThreshold()).isEqualTo(10);
        assertThat(appProperties.getVirtualTime().getReferenceDate()).isEqualTo("2006-05-31T23:59:56");
        assertThat(appProperties.getRedis().getNodes()).isNotEmpty();
    }

    static boolean isDockerNotAvailable() {
        try {
            return !org.testcontainers.DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            return true;
        }
    }
}
