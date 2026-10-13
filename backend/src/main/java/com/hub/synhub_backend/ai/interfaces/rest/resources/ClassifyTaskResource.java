package com.hub.synhub_backend.ai.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record ClassifyTaskResource(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 2000) String description,
        OffsetDateTime dueDate) {}
