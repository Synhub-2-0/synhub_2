package com.hub.synhub_backend.iam.interfaces.rest.transform;

import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.iam.domain.model.entities.Role;
import com.hub.synhub_backend.iam.interfaces.rest.resources.UserResource;

public class UserResourceFromEntityAssembler {
  public static UserResource toResourceFromEntity(User user) {
    var roles = user.getAccountRoles().stream()
        .map(Role::getStringName)
        .toList();
    return new UserResource(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getSurname(),
            user.getImgUrl(),
            user.getEmail(),
            roles);
  }
}
