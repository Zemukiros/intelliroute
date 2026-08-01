package com.intelliroute.api.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/routes/alternatives}.
 *
 * @param maximumRoutes optional; defaults to 3, bounded 1..5
 */
public record AlternativesRequest(
        @NotBlank(message = "origin is required")
        @Size(max = 32, message = "origin must be at most 32 characters")
        String origin,

        @NotBlank(message = "destination is required")
        @Size(max = 32, message = "destination must be at most 32 characters")
        String destination,

        @Min(value = 1, message = "maximumRoutes must be at least 1")
        @Max(value = 5, message = "maximumRoutes must be at most 5")
        Integer maximumRoutes) {

    public int maximumRoutesOrDefault() {
        return maximumRoutes == null ? 3 : maximumRoutes;
    }
}
