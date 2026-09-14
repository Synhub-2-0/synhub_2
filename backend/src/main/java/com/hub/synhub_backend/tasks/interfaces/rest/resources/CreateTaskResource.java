package com.hub.synhub_backend.tasks.interfaces.rest.resources;

import java.time.OffsetDateTime;

public record CreateTaskResource(
        String title,
        String description,
        OffsetDateTime dueDate
) {
}
