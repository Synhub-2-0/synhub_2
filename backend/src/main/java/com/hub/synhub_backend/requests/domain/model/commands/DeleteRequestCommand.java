package com.hub.synhub_backend.requests.domain.model.commands;

/**
 * Borra una request de una tarea. La puede borrar el líder del grupo, o el integrante asignado
 * mientras su request siga PENDING (las EXPIRED las genera el sistema y solo las borra el líder).
 */
public record DeleteRequestCommand(Long taskId, Long requestId, Long requesterId) {}
