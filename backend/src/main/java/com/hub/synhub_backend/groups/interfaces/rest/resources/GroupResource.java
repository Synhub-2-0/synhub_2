package com.hub.synhub_backend.groups.interfaces.rest.resources;

import com.hub.synhub_backend.iam.interfaces.rest.resources.UserResource;

import java.util.List;

public record GroupResource(
        Long id,
        String name,
        String imgUrl,
        String description,
        String code,
        Integer memberCount,
        List<UserResource> users
) {
}
