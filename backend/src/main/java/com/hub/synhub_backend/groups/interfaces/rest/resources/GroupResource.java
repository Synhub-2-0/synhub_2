package com.hub.synhub_backend.groups.interfaces.rest.resources;

public record GroupResource(
        Long id,
        String name,
        String imgUrl,
        String description,
        String code,
        int memberCount
) {
}
