package com.typeahed.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CacheDebugResponseDto {

    @JsonProperty("prefix")
    private String prefix;

    @JsonProperty("responsible_node")
    private String responsibleNode;

    @JsonProperty("slot")
    private int slot;

    @JsonProperty("cache_hit")
    private boolean cacheHit;

    public CacheDebugResponseDto() {
    }

    public CacheDebugResponseDto(String prefix, String responsibleNode, int slot, boolean cacheHit) {
        this.prefix = prefix;
        this.responsibleNode = responsibleNode;
        this.slot = slot;
        this.cacheHit = cacheHit;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getResponsibleNode() {
        return responsibleNode;
    }

    public void setResponsibleNode(String responsibleNode) {
        this.responsibleNode = responsibleNode;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public boolean isCacheHit() {
        return cacheHit;
    }

    public void setCacheHit(boolean cacheHit) {
        this.cacheHit = cacheHit;
    }
}
