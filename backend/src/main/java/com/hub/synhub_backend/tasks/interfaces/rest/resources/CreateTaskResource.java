package com.hub.synhub_backend.tasks.interfaces.rest.resources;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record CreateTaskResource(
        @NotBlank @Size(max = 255) String title,
        @NotNull @Size(max = 255) String description,
        @NotNull OffsetDateTime dueDate,
        @NotNull Long userId,
        @NotNull Long groupId,
        @Min(1) @Max(5) Integer difficulty,
        @Size(max = 32) String context,
        @Size(max = 16) String urgency,
        @Size(max = 255) String labels,
        Boolean aiAccepted) {}
