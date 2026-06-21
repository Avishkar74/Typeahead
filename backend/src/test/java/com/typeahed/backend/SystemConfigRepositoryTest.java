package com.typeahed.backend;

import com.typeahed.backend.entity.SystemConfig;
import com.typeahed.backend.repository.SystemConfigRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisabledIf("com.typeahed.backend.SystemConfigRepositoryTest#isDockerNotAvailable")
class SystemConfigRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @BeforeEach
    void setUp() {
        systemConfigRepository.deleteAll();

        // Seed config
        SystemConfig config = new SystemConfig();
        config.setConfigKey("last_virtual_time");
        config.setConfigValue("2006-05-31T23:59:56");
        systemConfigRepository.save(config);
    }

    @Test
    void testFindByConfigKey() {
        Optional<SystemConfig> config = systemConfigRepository.findByConfigKey("last_virtual_time");
        assertThat(config).isPresent();
        assertThat(config.get().getConfigValue()).isEqualTo("2006-05-31T23:59:56");
    }

    @Test
    void testFindByConfigKeyNonexistent() {
        Optional<SystemConfig> config = systemConfigRepository.findByConfigKey("nonexistent");
        assertThat(config).isEmpty();
    }

    static boolean isDockerNotAvailable() {
        try {
            return !org.testcontainers.DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            return true;
        }
    }
}
