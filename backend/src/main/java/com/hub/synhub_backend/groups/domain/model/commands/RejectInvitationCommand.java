package com.hub.synhub_backend.groups.domain.model.commands;

public record RejectInvitationCommand(
        Long leaderId,
        Long invitationId
) {
}
