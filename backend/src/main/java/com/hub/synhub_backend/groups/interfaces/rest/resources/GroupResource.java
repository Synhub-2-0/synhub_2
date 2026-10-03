package com.hub.synhub_backend.groups.interfaces.rest.resources;

import java.util.List;

public record GroupResource(
        Long id,
        String name,
        String imgUrl,
        String description,
        String code,
        List<UserInGroupResource> usersInGroup
) {
}
