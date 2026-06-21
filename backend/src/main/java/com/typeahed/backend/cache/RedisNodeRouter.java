package com.typeahed.backend.cache;

import com.typeahed.backend.config.AppProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RedisNodeRouter {

    private final ConsistentHashRing hashRing;

    public RedisNodeRouter(AppProperties appProperties) {
        List<String> nodes = appProperties.getRedis().getNodes();
        this.hashRing = new ConsistentHashRing(nodes);
    }

    /**
     * Routes the given prefix key to the responsible Redis node.
     * 
     * @param prefix the cache prefix key to route
     * @return the Redis node address (e.g. "localhost:6379")
     */
    public String route(String prefix) {
        return hashRing.getNode(prefix);
    }

    /**
     * Exposes the underlying hash ring for testing or inspection.
     */
    public ConsistentHashRing getHashRing() {
        return hashRing;
    }
}
