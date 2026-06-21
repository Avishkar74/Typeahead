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
    private final DatasetImport datasetImport = new DatasetImport();

    public Batch getBatch() {
        return batch;
    }

    public Redis getRedis() {
        return redis;
    }

    public VirtualTime getVirtualTime() {
        return virtualTime;
    }

    public DatasetImport getDatasetImport() {
        return datasetImport;
    }

    public static class DatasetImport {
        private boolean enabled = false;
        private int batchSize = 1000;
        private String queriesFilePath = "dataset/queries_count.csv";
        private String logsFilePath = "dataset/search_logs.csv";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public String getQueriesFilePath() {
            return queriesFilePath;
        }

        public void setQueriesFilePath(String queriesFilePath) {
            this.queriesFilePath = queriesFilePath;
        }

        public String getLogsFilePath() {
            return logsFilePath;
        }

        public void setLogsFilePath(String logsFilePath) {
            this.logsFilePath = logsFilePath;
        }
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
