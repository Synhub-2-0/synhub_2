package com.hub.synhub_backend.requests.domain.model.commands;

/**
 * @param requesterId usuario que envía la solicitud; null cuando la genera el sistema (p. ej. vencimiento automático).
 */
public record CreateRequestCommand(
        String description,
        String requestType,
        Long taskId,
        Long requesterId
) {
    public CreateRequestCommand(String description, String requestType, Long taskId) {
        this(description, requestType, taskId, null);
    }
}
