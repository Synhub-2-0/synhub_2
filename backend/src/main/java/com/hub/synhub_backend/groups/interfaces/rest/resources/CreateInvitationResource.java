package com.hub.synhub_backend.groups.interfaces.rest.resources;

public record CreateInvitationResource(
        Long userId,
        Long groupId
) {
}
