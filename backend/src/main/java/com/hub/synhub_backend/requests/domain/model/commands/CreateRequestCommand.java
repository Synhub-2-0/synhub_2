package com.hub.synhub_backend.requests.domain.model.commands;

public record CreateRequestCommand(
        String description,
        String requestType,
        Long taskId
) {
}
