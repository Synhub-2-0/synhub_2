package com.hub.synhub_backend.groups.interfaces.rest.resources;

public record UpdateGroupResource(
        String name,
        String imgUrl,
        String description
) {
}
