package com.hub.synhub_backend.groups.interfaces.rest.resources;

import com.hub.synhub_backend.iam.domain.model.valueobjects.Roles;
import com.hub.synhub_backend.iam.interfaces.rest.resources.UserResource;

public record UserInGroupResource(
        UserResource user,
        Roles roleInGroup
) {
}
