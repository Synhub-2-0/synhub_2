package com.hub.synhub_backend.iam.interfaces.rest.transform;

import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.iam.interfaces.rest.resources.PublicUserResource;

public class PublicUserResourceFromEntityAssembler {
    public static PublicUserResource toResourceFromEntity(User user) {
        return new PublicUserResource(
                user.getId(), user.getUsername(), user.getName(), user.getSurname(), user.getImgUrl(), user.getEmail());
    }
}
