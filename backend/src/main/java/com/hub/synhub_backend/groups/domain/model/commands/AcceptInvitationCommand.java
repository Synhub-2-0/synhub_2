package com.hub.synhub_backend.groups.domain.model.commands;

public record AcceptInvitationCommand(
        Long userId,
        Long invitationId
) {
}
