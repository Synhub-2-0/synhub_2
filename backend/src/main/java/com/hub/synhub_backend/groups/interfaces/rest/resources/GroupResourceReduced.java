package com.hub.synhub_backend.groups.interfaces.rest.resources;

import com.hub.synhub_backend.iam.interfaces.rest.resources.UserResource;

import java.util.List;

public record GroupResourceReduced(
        Long id,
        String name,
        String imgUrl,
        String description,
        String code,
        int memberCount,
        UserResource leader
) {
}
