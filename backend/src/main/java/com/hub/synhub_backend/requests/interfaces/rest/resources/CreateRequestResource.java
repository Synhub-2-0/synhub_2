package com.hub.synhub_backend.requests.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRequestResource(
        @NotNull @Size(max = 255) String description,
        @NotBlank String requestType
) {
}
