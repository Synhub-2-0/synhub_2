package com.hub.synhub_backend.tasks.interfaces.rest.resources;

public record TaskMemberResource(
        Long id,
        String name,
        String surname,
        String urlImage
) {
}
