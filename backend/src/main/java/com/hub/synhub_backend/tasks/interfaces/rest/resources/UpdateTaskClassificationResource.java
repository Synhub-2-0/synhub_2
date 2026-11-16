package com.hub.synhub_backend.tasks.interfaces.rest.resources;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Reemplaza la clasificación guardada: un campo nulo o vacío la quita. */
public record UpdateTaskClassificationResource(
        @Min(1) @Max(5) Integer difficulty,
        @Size(max = 32) String context,
        @Size(max = 16) String urgency,
        @Size(max = 255) String labels,
        Boolean aiAccepted,
        @Size(max = 500) String aiRationale) {}
