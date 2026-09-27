package com.hub.synhub_backend.tasks.domain.model.commands;

import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;

public record UpdateTaskStatusCommand(
        Long requesterId,
        Long taskId,
        TaskStatus status
) {
}
