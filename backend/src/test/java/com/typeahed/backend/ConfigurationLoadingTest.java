package com.typeahed.backend;

import com.typeahed.backend.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = ConfigurationLoadingTest.TestConfig.class)
@TestPropertySource(properties = {
        "app.batch.flush-interval-seconds=30",
        "app.batch.buffer-size-threshold=10",
        "app.redis.nodes=localhost:6379,localhost:6380,localhost:6381",
        "app.virtual-time.reference-date=2006-05-31T23:59:56"
})
class ConfigurationLoadingTest {

    @Autowired
    private AppProperties appProperties;

    @Test
    void testConfigurationLoading() {
        assertThat(appProperties).isNotNull();
        assertThat(appProperties.getBatch().getFlushIntervalSeconds()).isEqualTo(30);
        assertThat(appProperties.getBatch().getBufferSizeThreshold()).isEqualTo(10);
        assertThat(appProperties.getVirtualTime().getReferenceDate()).isEqualTo("2006-05-31T23:59:56");
        assertThat(appProperties.getRedis().getNodes()).containsExactly("localhost:6379", "localhost:6380", "localhost:6381");
    }

    @EnableConfigurationProperties(AppProperties.class)
    static class TestConfig {
    }
}
