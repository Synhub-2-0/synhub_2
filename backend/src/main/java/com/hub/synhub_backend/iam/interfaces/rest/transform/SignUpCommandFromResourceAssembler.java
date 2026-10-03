package com.hub.synhub_backend.iam.interfaces.rest.transform;

import com.hub.synhub_backend.iam.domain.model.commands.SignUpCommand;
import com.hub.synhub_backend.iam.domain.model.entities.Role;
import com.hub.synhub_backend.iam.interfaces.rest.resources.SignUpResource;

import java.util.ArrayList;

public class SignUpCommandFromResourceAssembler {

  public static SignUpCommand toCommandFromResource(SignUpResource resource) {
    var roles = resource.accountRoles() != null
        ? resource.accountRoles().stream().map(accountRole -> Role.toRoleFromName(accountRole.name())).toList()
        : new ArrayList<Role>();
    return new SignUpCommand(
            resource.username(),
            resource.name(),
            resource.surname(),
            resource.imgUrl(),
            resource.email(),
            resource.password(),
            roles);
  }
}
