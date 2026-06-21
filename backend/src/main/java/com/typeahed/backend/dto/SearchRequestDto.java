package com.typeahed.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SearchRequestDto {

    @NotBlank(message = "Query text cannot be blank")
    @Size(max = 255, message = "Query text must not exceed 255 characters")
    private String query;

    public SearchRequestDto() {
    }

    public SearchRequestDto(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
