package com.hub.synhub_backend.groups.interfaces.rest.resources;

public record InvitationResource(
        Long id,
        InvitationMemberResource member,
        GroupResource group
) {
}
