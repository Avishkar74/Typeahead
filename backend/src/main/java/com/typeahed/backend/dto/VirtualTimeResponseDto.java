package com.typeahed.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VirtualTimeResponseDto {

    @JsonProperty("virtual_time")
    private String virtualTime;

    @JsonProperty("reference_time")
    private String referenceTime;

    @JsonProperty("real_time_elapsed_seconds")
    private long realTimeElapsedSeconds;

    @JsonProperty("batch_flush_increment_seconds")
    private int batchFlushIncrementSeconds;

    public VirtualTimeResponseDto() {
    }

    public VirtualTimeResponseDto(String virtualTime,
                                  String referenceTime,
                                  long realTimeElapsedSeconds,
                                  int batchFlushIncrementSeconds) {
        this.virtualTime = virtualTime;
        this.referenceTime = referenceTime;
        this.realTimeElapsedSeconds = realTimeElapsedSeconds;
        this.batchFlushIncrementSeconds = batchFlushIncrementSeconds;
    }

    public String getVirtualTime() {
        return virtualTime;
    }

    public void setVirtualTime(String virtualTime) {
        this.virtualTime = virtualTime;
    }

    public String getReferenceTime() {
        return referenceTime;
    }

    public void setReferenceTime(String referenceTime) {
        this.referenceTime = referenceTime;
    }

    public long getRealTimeElapsedSeconds() {
        return realTimeElapsedSeconds;
    }

    public void setRealTimeElapsedSeconds(long realTimeElapsedSeconds) {
        this.realTimeElapsedSeconds = realTimeElapsedSeconds;
    }

    public int getBatchFlushIncrementSeconds() {
        return batchFlushIncrementSeconds;
    }

    public void setBatchFlushIncrementSeconds(int batchFlushIncrementSeconds) {
        this.batchFlushIncrementSeconds = batchFlushIncrementSeconds;
    }
}
