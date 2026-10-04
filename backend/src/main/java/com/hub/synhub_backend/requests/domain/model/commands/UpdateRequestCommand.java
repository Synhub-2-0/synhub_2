package com.hub.synhub_backend.requests.domain.model.commands;

public record UpdateRequestCommand(Long requestId, String requestStatus, Long requesterId) {
}
