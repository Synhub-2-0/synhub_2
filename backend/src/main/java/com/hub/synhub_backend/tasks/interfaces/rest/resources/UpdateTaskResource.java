package com.hub.synhub_backend.tasks.interfaces.rest.resources;

import java.time.OffsetDateTime;

public record UpdateTaskResource(
        Long requesterId,
        String title,
        String description,
        OffsetDateTime dueDate,
        Long userId
) {
}
