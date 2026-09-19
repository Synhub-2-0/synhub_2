package com.hub.synhub_backend.groups.interfaces.rest.resources;

import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupRole;
import com.hub.synhub_backend.iam.interfaces.rest.resources.UserResource;

public record UserInGroupResource(
        UserResource user,
        GroupRole roleInGroup
) {
}
