package com.hub.synhub_backend.tasks.domain.model.queries;

/** Tareas de un integrante en un grupo; solo las ven los integrantes del grupo. */
public record GetTasksByGroupIdAndUserIdQuery(Long groupId, Long userId, Long requesterId) {}
