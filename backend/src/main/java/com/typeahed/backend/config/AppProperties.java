package com.typeahed.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Batch batch = new Batch();
    private final Redis redis = new Redis();
    private final VirtualTime virtualTime = new VirtualTime();

    public Batch getBatch() {
        return batch;
    }

    public Redis getRedis() {
        return redis;
    }

    public VirtualTime getVirtualTime() {
        return virtualTime;
    }

    public static class Batch {
        private int flushIntervalSeconds = 30;
        private int bufferSizeThreshold = 10;

        public int getFlushIntervalSeconds() {
            return flushIntervalSeconds;
        }

        public void setFlushIntervalSeconds(int flushIntervalSeconds) {
            this.flushIntervalSeconds = flushIntervalSeconds;
        }

        public int getBufferSizeThreshold() {
            return bufferSizeThreshold;
        }

        public void setBufferSizeThreshold(int bufferSizeThreshold) {
            this.bufferSizeThreshold = bufferSizeThreshold;
        }
    }

    public static class Redis {
        private List<String> nodes = List.of("localhost:6379", "localhost:6380", "localhost:6381");

        public List<String> getNodes() {
            return nodes;
        }

        public void setNodes(List<String> nodes) {
            this.nodes = nodes;
        }
    }

    public static class VirtualTime {
        private String referenceDate = "2006-05-31T23:59:56";

        public String getReferenceDate() {
            return referenceDate;
        }

        public void setReferenceDate(String referenceDate) {
            this.referenceDate = referenceDate;
        }
    }
}
