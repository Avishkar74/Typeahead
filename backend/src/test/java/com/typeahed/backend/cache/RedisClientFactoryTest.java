package com.typeahed.backend.cache;

import com.typeahed.backend.config.AppProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedisClientFactoryTest {

    private AppProperties appProperties;
    private RedisClientFactory factory;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        // Setup 3 standalone nodes for test config
        appProperties.getRedis().setNodes(List.of("localhost:6379", "localhost:6380", "localhost:6381"));
        factory = new RedisClientFactory(appProperties);
    }

    @AfterEach
    void tearDown() {
        factory.destroy();
    }

    @Test
    void testGetTemplateForConfiguredNodes() {
        RedisTemplate<String, String> t1 = factory.getTemplate("localhost:6379");
        RedisTemplate<String, String> t2 = factory.getTemplate("localhost:6380");
        RedisTemplate<String, String> t3 = factory.getTemplate("localhost:6381");

        assertThat(t1).isNotNull();
        assertThat(t2).isNotNull();
        assertThat(t3).isNotNull();
        assertThat(t1).isNotSameAs(t2);
    }

    @Test
    void testGetTemplateThrowsForUnconfiguredNode() {
        assertThatThrownBy(() -> factory.getTemplate("localhost:9999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No Redis connection configured for node");
    }

    @Test
    void testCleanupOnDestroy() {
        factory.destroy();
        assertThatThrownBy(() -> factory.getTemplate("localhost:6379"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
