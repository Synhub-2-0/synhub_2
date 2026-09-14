package com.hub.synhub_backend.groups.interfaces.rest.resources;

public record CreateGroupResource(
        String name,
        String imgUrl,
        String description
) {
}
