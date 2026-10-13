package com.hub.synhub_backend.tasks.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * title/description vacíos y dueDate ausente no cambian; userId es el integrante asignado (puede ser el mismo).
 * Quien edita se toma de la sesión (AUDIT BUG-16).
 */
public record UpdateTaskResource(
        @Size(max = 255) String title,
        @Size(max = 255) String description,
        OffsetDateTime dueDate,
        @NotNull Long userId
) {
}
