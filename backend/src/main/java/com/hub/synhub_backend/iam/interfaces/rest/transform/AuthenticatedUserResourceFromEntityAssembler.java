package com.hub.synhub_backend.iam.interfaces.rest.transform;


import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.iam.domain.model.entities.Role;
import com.hub.synhub_backend.iam.interfaces.rest.resources.AuthenticatedUserResource;

public class AuthenticatedUserResourceFromEntityAssembler {

  public static AuthenticatedUserResource toResourceFromEntity(User user, String token) {
      var roles = user.getAccountRoles().stream()
              .map(Role::getStringName)
              .toList();
      return new AuthenticatedUserResource(user.getId(), user.getUsername(), token, roles);
  }
}
