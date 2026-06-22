package com.typeahed.backend.controller;

import com.typeahed.backend.cache.CacheService;
import com.typeahed.backend.cache.RedisNodeRouter;
import com.typeahed.backend.dto.CacheDebugResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Debug", description = "Cache routing diagnostics endpoints")
public class DebugController {

    private final RedisNodeRouter redisNodeRouter;
    private final CacheService cacheService;

    public DebugController(RedisNodeRouter redisNodeRouter, CacheService cacheService) {
        this.redisNodeRouter = redisNodeRouter;
        this.cacheService = cacheService;
    }

    @GetMapping("/api/debug/cache")
    @Operation(
            summary = "Debug cache routing for a prefix",
            description = "Resolves which slot and standalone Redis node is responsible for the given prefix, and checks for cache presence."
    )
    @ApiResponse(responseCode = "200", description = "Debug parameters successfully resolved",
            content = @Content(schema = @Schema(implementation = CacheDebugResponseDto.class)))
    public ResponseEntity<CacheDebugResponseDto> debugCache(
            @RequestParam("prefix") String prefix) {

        String normalized = (prefix != null) ? prefix.toLowerCase().trim() : "";
        String node = redisNodeRouter.route(normalized);
        int slot = redisNodeRouter.getHashRing().getSlot(normalized);
        boolean cacheHit = normalized.length() >= 3 && cacheService.get(normalized, "trending").isPresent();

        CacheDebugResponseDto response = new CacheDebugResponseDto(normalized, node, slot, cacheHit);
        return ResponseEntity.ok(response);
    }
}
