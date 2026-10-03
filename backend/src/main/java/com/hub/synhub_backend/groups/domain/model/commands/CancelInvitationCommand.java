package com.hub.synhub_backend.groups.domain.model.commands;

public record CancelInvitationCommand(
        Long requesterId,
        Long invitationId
) {
}
