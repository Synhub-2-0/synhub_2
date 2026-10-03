package com.hub.synhub_backend.tasks.interfaces.rest.resources;

import java.time.OffsetDateTime;

public record CreateTaskResource(
        String title,
        String description,
        OffsetDateTime dueDate,
        Long userId,
        Long groupId,
        Integer difficulty,
        String context,
        String urgency,
        String labels,
        Boolean aiAccepted
) {
}
