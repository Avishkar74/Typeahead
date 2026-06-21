package com.typeahed.backend.controller;

import com.typeahed.backend.dto.SearchRequestDto;
import com.typeahed.backend.dto.SearchResponseDto;
import com.typeahed.backend.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Search", description = "Endpoints for submitting search terms")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping("/search")
    @Operation(
            summary = "Submit a search query",
            description = "Logs a search query under virtual time and enqueues it in the buffer for batch flushing."
    )
    @ApiResponse(responseCode = "200", description = "Query successfully logged",
            content = @Content(schema = @Schema(implementation = SearchResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Invalid validation parameters or malformed request")
    public ResponseEntity<SearchResponseDto> submitSearch(@Valid @RequestBody SearchRequestDto request) {
        searchService.submitSearch(request.getQuery());
        return ResponseEntity.ok(new SearchResponseDto("Searched"));
    }
}
