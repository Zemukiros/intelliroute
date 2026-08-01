package com.intelliroute.api.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/routes/calculate}.
 */
public record RouteRequest(
        @NotBlank(message = "origin is required")
        @Size(max = 32, message = "origin must be at most 32 characters")
        String origin,

        @NotBlank(message = "destination is required")
        @Size(max = 32, message = "destination must be at most 32 characters")
        String destination) {
}
