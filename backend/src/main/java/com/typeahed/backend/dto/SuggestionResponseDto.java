package com.typeahed.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.typeahed.backend.cache.SuggestionDto;
import java.util.List;

public class SuggestionResponseDto {

    @JsonProperty("prefix")
    private String prefix;

    @JsonProperty("ranking_type")
    private String rankingType;

    @JsonProperty("results")
    private List<SuggestionDto> results;

    @JsonProperty("cache_hit")
    private boolean cacheHit;

    public SuggestionResponseDto() {
    }

    public SuggestionResponseDto(String prefix, String rankingType, List<SuggestionDto> results, boolean cacheHit) {
        this.prefix = prefix;
        this.rankingType = rankingType;
        this.results = results;
        this.cacheHit = cacheHit;
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

    public boolean isCacheHit() {
        return cacheHit;
    }

    public void setCacheHit(boolean cacheHit) {
        this.cacheHit = cacheHit;
    }
}
