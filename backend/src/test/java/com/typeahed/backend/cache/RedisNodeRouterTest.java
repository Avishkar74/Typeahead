package com.typeahed.backend.cache;

import com.typeahed.backend.config.AppProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisNodeRouterTest {

    @Test
    void testRouterLoadsNodesFromProperties() {
        AppProperties appProperties = mock(AppProperties.class);
        AppProperties.Redis redisConfig = mock(AppProperties.Redis.class);
        when(redisConfig.getNodes()).thenReturn(List.of("host1:6379", "host2:6379", "host3:6379"));
        when(appProperties.getRedis()).thenReturn(redisConfig);

        RedisNodeRouter router = new RedisNodeRouter(appProperties);

        // Verify hash ring is built from properties
        assertThat(router.getHashRing().getNodes())
                .containsExactly("host1:6379", "host2:6379", "host3:6379");

        // Verify routing behaves deterministically using the ring
        String prefix = "iph";
        String routedNode = router.route(prefix);
        String ringNode = router.getHashRing().getNode(prefix);
        assertThat(routedNode).isEqualTo(ringNode);
    }
}
