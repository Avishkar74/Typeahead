package com.typeahed.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "queries")
public class Query {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(name = "query_text", nullable = false, unique = true)
    private String queryText;

    @NotBlank
    @Size(max = 255)
    @Column(name = "query_lower", nullable = false)
    private String queryLower;

    @Column(name = "global_count")
    private Integer globalCount = 0;

    @Column(name = "weekly_count")
    private Integer weeklyCount = 0;

    @Column(name = "daily_count")
    private Integer dailyCount = 0;

    @Column(name = "trending_score", precision = 10, scale = 2)
    private BigDecimal trendingScore = BigDecimal.ZERO;

    @Column(name = "first_searched_at")
    private LocalDateTime firstSearchedAt;

    @Column(name = "last_searched_at")
    private LocalDateTime lastSearchedAt;

    @Column(name = "trending_score_calculated_at")
    private LocalDateTime trendingScoreCalculatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
        if (queryText != null) {
            this.queryLower = queryText.toLowerCase().trim();
        }
    }

    public String getQueryLower() {
        return queryLower;
    }

    public void setQueryLower(String queryLower) {
        this.queryLower = queryLower;
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

    public LocalDateTime getFirstSearchedAt() {
        return firstSearchedAt;
    }

    public void setFirstSearchedAt(LocalDateTime firstSearchedAt) {
        this.firstSearchedAt = firstSearchedAt;
    }

    public LocalDateTime getLastSearchedAt() {
        return lastSearchedAt;
    }

    public void setLastSearchedAt(LocalDateTime lastSearchedAt) {
        this.lastSearchedAt = lastSearchedAt;
    }

    public LocalDateTime getTrendingScoreCalculatedAt() {
        return trendingScoreCalculatedAt;
    }

    public void setTrendingScoreCalculatedAt(LocalDateTime trendingScoreCalculatedAt) {
        this.trendingScoreCalculatedAt = trendingScoreCalculatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
