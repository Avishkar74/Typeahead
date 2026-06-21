package com.typeahed.backend.cache;

import com.typeahed.backend.config.AppProperties;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RedisClientFactory implements DisposableBean {

    private final Map<String, LettuceConnectionFactory> factories = new ConcurrentHashMap<>();
    private final Map<String, RedisTemplate<String, String>> templates = new ConcurrentHashMap<>();

    public RedisClientFactory(AppProperties appProperties) {
        for (String node : appProperties.getRedis().getNodes()) {
            String[] parts = node.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
            LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
            factory.afterPropertiesSet();

            RedisTemplate<String, String> template = new RedisTemplate<>();
            template.setConnectionFactory(factory);
            template.setKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(new StringRedisSerializer());
            template.setHashKeySerializer(new StringRedisSerializer());
            template.setHashValueSerializer(new StringRedisSerializer());
            template.afterPropertiesSet();

            factories.put(node, factory);
            templates.put(node, template);
        }
    }

    public RedisTemplate<String, String> getTemplate(String nodeAddress) {
        RedisTemplate<String, String> template = templates.get(nodeAddress);
        if (template == null) {
            throw new IllegalArgumentException("No Redis connection configured for node: " + nodeAddress);
        }
        return template;
    }

    @Override
    public void destroy() {
        for (LettuceConnectionFactory factory : factories.values()) {
            try {
                factory.destroy();
            } catch (Exception e) {
                // Suppress exception during shutdown
            }
        }
        factories.clear();
        templates.clear();
    }
}
