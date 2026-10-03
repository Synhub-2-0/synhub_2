package com.hub.synhub_backend.tasks.domain.model.commands;

import java.time.OffsetDateTime;

public record CreateTaskCommand(
        Long requesterId,
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
