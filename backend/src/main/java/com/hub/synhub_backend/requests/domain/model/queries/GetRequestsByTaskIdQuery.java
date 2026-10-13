package com.hub.synhub_backend.requests.domain.model.queries;

/** Requests de una tarea; solo las ven los integrantes del grupo de la tarea. */
public record GetRequestsByTaskIdQuery(Long taskId, Long requesterId) {
}
