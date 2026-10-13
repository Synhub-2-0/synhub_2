package com.hub.synhub_backend.requests.domain.model.queries;

/** Request por id; solo la ven los integrantes del grupo de su tarea. */
public record GetRequestByIdQuery(Long requestId, Long requesterId) {}
