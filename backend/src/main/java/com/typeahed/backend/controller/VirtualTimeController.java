package com.typeahed.backend.controller;

import com.typeahed.backend.config.AppProperties;
import com.typeahed.backend.dto.VirtualTimeResponseDto;
import com.typeahed.backend.virtualtime.VirtualTimeManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@Tag(name = "Virtual Time", description = "Endpoints for inspecting the virtual time subsystem")
public class VirtualTimeController {

    private static final int BATCH_FLUSH_INCREMENT_SECONDS = 60;

    private final VirtualTimeManager virtualTimeManager;
    private final AppProperties appProperties;

    public VirtualTimeController(VirtualTimeManager virtualTimeManager, AppProperties appProperties) {
        this.virtualTimeManager = virtualTimeManager;
        this.appProperties = appProperties;
    }

    @GetMapping("/api/virtual-time")
    @Operation(
            summary = "Get the current virtual time snapshot",
            description = "Exposes the current virtual time, reference time, elapsed real seconds, and batch increment."
    )
    @ApiResponse(responseCode = "200", description = "Virtual time snapshot retrieved",
            content = @Content(schema = @Schema(implementation = VirtualTimeResponseDto.class)))
    public ResponseEntity<VirtualTimeResponseDto> getVirtualTime() {
        LocalDateTime currentVirtualTime = virtualTimeManager.getVirtualTime();
        LocalDateTime referenceTime = virtualTimeManager.getSavedVirtualTime();
        long elapsedSeconds = virtualTimeManager.getElapsedRealSeconds();

        if (referenceTime == null && appProperties != null && appProperties.getVirtualTime() != null) {
            referenceTime = LocalDateTime.parse(appProperties.getVirtualTime().getReferenceDate());
        }

        VirtualTimeResponseDto response = new VirtualTimeResponseDto(
                currentVirtualTime.toString(),
                referenceTime != null ? referenceTime.toString() : null,
                elapsedSeconds,
                BATCH_FLUSH_INCREMENT_SECONDS
        );
        return ResponseEntity.ok(response);
    }
}
