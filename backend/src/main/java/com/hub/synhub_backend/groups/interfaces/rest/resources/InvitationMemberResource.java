package com.hub.synhub_backend.groups.interfaces.rest.resources;

public record InvitationMemberResource(
        Long id,
        String username,
        String name,
        String surname,
        String imgUrl
) {
}
