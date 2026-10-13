package com.hub.synhub_backend.tasks.domain.model.queries;

/** Tarea por id; solo la ven los integrantes de su grupo. */
public record GetTaskByIdQuery(Long taskId, Long requesterId) {
}
