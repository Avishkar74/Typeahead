package com.typeahed.backend.dto;

import com.typeahed.backend.cache.SuggestionDto;
import java.util.List;

public class TrendingResponseDto {

    private List<SuggestionDto> results;

    public TrendingResponseDto() {
    }

    public TrendingResponseDto(List<SuggestionDto> results) {
        this.results = results;
    }

    public List<SuggestionDto> getResults() {
        return results;
    }

    public void setResults(List<SuggestionDto> results) {
        this.results = results;
    }
}
