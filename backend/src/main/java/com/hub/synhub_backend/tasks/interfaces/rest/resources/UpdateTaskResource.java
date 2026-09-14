package com.hub.synhub_backend.tasks.interfaces.rest.resources;

import java.time.OffsetDateTime;

public record UpdateTaskResource(
        String title,
        String description,
        OffsetDateTime dueDate,
        Long memberId
) {
}
