package com.hub.synhub_backend.groups.interfaces.rest.transform;

import com.hub.synhub_backend.groups.interfaces.rest.resources.UserInGroupResource;
import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.iam.domain.model.valueobjects.Roles;
import com.hub.synhub_backend.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;

public class UserInGroupResourceFromEntityAssembler {
    public static UserInGroupResource toResourceFromEntity(User user, Roles roleInGroup) {
        return new UserInGroupResource(
                UserResourceFromEntityAssembler.toResourceFromEntity(user),
                roleInGroup
        );
    }
}
