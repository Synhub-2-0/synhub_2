package com.hub.synhub_backend.groups.domain.model.commands;

public record CreateInvitationCommand(
        Long userId,
        Long groupId
) {
}
