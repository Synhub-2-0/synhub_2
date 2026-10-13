package com.hub.synhub_backend.tasks.domain.model.queries;

import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;

/** Tareas de un grupo (status opcional); solo las ven sus integrantes. */
public record GetTasksByGroupIdAndStatus(Long groupId, TaskStatus status, Long requesterId) {
}
