package com.hub.synhub_backend.ai.interfaces.rest.resources;

import java.time.OffsetDateTime;

public record ClassifyTaskResource(
        String title,
        String description,
        OffsetDateTime dueDate
) {
}
