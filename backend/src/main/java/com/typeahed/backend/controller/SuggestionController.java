package com.typeahed.backend.controller;

import com.typeahed.backend.cache.CacheContext;
import com.typeahed.backend.cache.SuggestionDto;
import com.typeahed.backend.cache.SuggestionService;
import com.typeahed.backend.dto.SuggestionResponseDto;
import com.typeahed.backend.dto.TrendingResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Suggestions", description = "Endpoints for fetching query completion suggestions")
public class SuggestionController {

    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @GetMapping("/suggest")
    @Operation(
            summary = "Fetch suggestions for a prefix",
            description = "Retrieves up to 10 prefix-matching suggestions ranked by either trending_score or global_count."
    )
    @ApiResponse(responseCode = "200", description = "Suggestions fetched successfully",
            content = @Content(schema = @Schema(implementation = SuggestionResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Invalid ranking parameter")
    public ResponseEntity<SuggestionResponseDto> getSuggestions(
            @Parameter(description = "Prefix search query string")
            @RequestParam("q") String query,
            @Parameter(description = "Ranking mode: 'trending' or 'global' (defaults to 'trending')")
            @RequestParam(value = "ranking", required = false, defaultValue = "trending") String ranking) {

        if (ranking != null && !ranking.equalsIgnoreCase("trending") && !ranking.equalsIgnoreCase("global")) {
            throw new IllegalArgumentException("Invalid ranking type: must be 'trending' or 'global'");
        }

        try {
            List<SuggestionDto> results = suggestionService.getSuggestions(query, ranking);
            boolean cacheHit = CacheContext.getCacheHit();
            SuggestionResponseDto response = new SuggestionResponseDto(
                    query != null ? query.toLowerCase().trim() : "",
                    ranking.toLowerCase().trim(),
                    results,
                    cacheHit
            );
            return ResponseEntity.ok(response);
        } finally {
            CacheContext.clear();
        }
    }

    @GetMapping("/trending")
    @Operation(
            summary = "Get overall trending queries",
            description = "Retrieves the top 10 most trending queries in the system overall."
    )
    @ApiResponse(responseCode = "200", description = "Overall trending queries retrieved",
            content = @Content(schema = @Schema(implementation = TrendingResponseDto.class)))
    public ResponseEntity<TrendingResponseDto> getTrendingOverall() {
        List<SuggestionDto> results = suggestionService.getTrendingOverall();
        return ResponseEntity.ok(new TrendingResponseDto(results));
    }
}
