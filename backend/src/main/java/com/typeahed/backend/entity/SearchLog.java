package com.typeahed.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "search_logs")
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(name = "query_lower", nullable = false)
    private String queryLower;

    @Size(max = 255)
    @Column(name = "query_text")
    private String queryText;

    @NotNull
    @Column(name = "virtual_searched_at", nullable = false)
    private LocalDateTime virtualSearchedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "batched")
    private Boolean batched = false;

    @Column(name = "batched_at")
    private LocalDateTime batchedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (batched == null) {
            batched = false;
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQueryLower() {
        return queryLower;
    }

    public void setQueryLower(String queryLower) {
        this.queryLower = queryLower;
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

    public LocalDateTime getVirtualSearchedAt() {
        return virtualSearchedAt;
    }

    public void setVirtualSearchedAt(LocalDateTime virtualSearchedAt) {
        this.virtualSearchedAt = virtualSearchedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getBatched() {
        return batched;
    }

    public void setBatched(Boolean batched) {
        this.batched = batched;
    }

    public LocalDateTime getBatchedAt() {
        return batchedAt;
    }

    public void setBatchedAt(LocalDateTime batchedAt) {
        this.batchedAt = batchedAt;
    }
}
