package com.hub.synhub_backend.groups.interfaces.rest.resources;

import com.hub.synhub_backend.iam.interfaces.rest.resources.UserResource;

public record InvitationResource(
        Long id,
        GroupResource group,
        UserResource user
) {
}
