package com.hub.synhub_backend.tasks.domain.model.commands;

import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskClassificationUpdate;
import java.time.OffsetDateTime;

/** classification nula: la clasificación de la tarea no cambia. */
public record UpdateTaskCommand(
        Long requesterId,
        Long taskId,
        String title,
        String description,
        OffsetDateTime dueDate,
        Long newUserId,
        TaskClassificationUpdate classification) {

    public UpdateTaskCommand(
            Long requesterId, Long taskId, String title, String description, OffsetDateTime dueDate, Long newUserId) {
        this(requesterId, taskId, title, description, dueDate, newUserId, null);
    }
}
