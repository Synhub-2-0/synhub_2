package com.hub.synhub_backend.tasks.domain.model.commands;

public record UpdateTaskStatusCommand(
        Long taskId,
        String status
) {
}
