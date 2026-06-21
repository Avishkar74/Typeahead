package com.typeahed.backend.cache;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class SuggestionDto {

    @JsonProperty("query")
    private String query;

    @JsonProperty("global_count")
    private Integer globalCount;

    @JsonProperty("weekly_count")
    private Integer weeklyCount;

    @JsonProperty("daily_count")
    private Integer dailyCount;

    @JsonProperty("trending_score")
    private BigDecimal trendingScore;

    // No-arg constructor required for deserialization
    public SuggestionDto() {
    }

    public SuggestionDto(String query, Integer globalCount, Integer weeklyCount, Integer dailyCount, BigDecimal trendingScore) {
        this.query = query;
        this.globalCount = globalCount;
        this.weeklyCount = weeklyCount;
        this.dailyCount = dailyCount;
        this.trendingScore = trendingScore;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Integer getGlobalCount() {
        return globalCount;
    }

    public void setGlobalCount(Integer globalCount) {
        this.globalCount = globalCount;
    }

    public Integer getWeeklyCount() {
        return weeklyCount;
    }

    public void setWeeklyCount(Integer weeklyCount) {
        this.weeklyCount = weeklyCount;
    }

    public Integer getDailyCount() {
        return dailyCount;
    }

    public void setDailyCount(Integer dailyCount) {
        this.dailyCount = dailyCount;
    }

    public BigDecimal getTrendingScore() {
        return trendingScore;
    }

    public void setTrendingScore(BigDecimal trendingScore) {
        this.trendingScore = trendingScore;
    }
}
