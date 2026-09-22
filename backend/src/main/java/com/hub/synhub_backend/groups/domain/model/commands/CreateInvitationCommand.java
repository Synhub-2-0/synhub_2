package com.hub.synhub_backend.groups.domain.model.commands;

public record CreateInvitationCommand(
        Long requesterId,
        Long userId,
        Long groupId
) {
}
