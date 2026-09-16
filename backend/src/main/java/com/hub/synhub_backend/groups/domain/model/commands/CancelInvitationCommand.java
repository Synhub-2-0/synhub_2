package com.hub.synhub_backend.groups.domain.model.commands;

public record CancelInvitationCommand(
        Long userId,
        Long invitationId
) {
}
