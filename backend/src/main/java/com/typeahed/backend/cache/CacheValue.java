package com.typeahed.backend.cache;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class CacheValue {

    @JsonProperty("prefix")
    private String prefix;

    @JsonProperty("ranking_type")
    private String rankingType;

    @JsonProperty("results")
    private List<SuggestionDto> results;

    @JsonProperty("cached_at")
    private String cachedAt;

    @JsonProperty("expires_at")
    private String expiresAt;

    // No-arg constructor required for deserialization
    public CacheValue() {
    }

    public CacheValue(String prefix, String rankingType, List<SuggestionDto> results, String cachedAt, String expiresAt) {
        this.prefix = prefix;
        this.rankingType = rankingType;
        this.results = results;
        this.cachedAt = cachedAt;
        this.expiresAt = expiresAt;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getRankingType() {
        return rankingType;
    }

    public void setRankingType(String rankingType) {
        this.rankingType = rankingType;
    }

    public List<SuggestionDto> getResults() {
        return results;
    }

    public void setResults(List<SuggestionDto> results) {
        this.results = results;
    }

    public String getCachedAt() {
        return cachedAt;
    }

    public void setCachedAt(String cachedAt) {
        this.cachedAt = cachedAt;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }
}
