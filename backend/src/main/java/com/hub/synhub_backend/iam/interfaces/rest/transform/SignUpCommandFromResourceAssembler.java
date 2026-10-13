package com.hub.synhub_backend.iam.interfaces.rest.transform;

import com.hub.synhub_backend.iam.domain.model.commands.SignUpCommand;
import com.hub.synhub_backend.iam.domain.model.entities.Role;
import com.hub.synhub_backend.iam.interfaces.rest.resources.SignUpResource;

import java.util.List;

public class SignUpCommandFromResourceAssembler {
  /** El registro público siempre crea cuentas con el rol por defecto; los roles los asigna el sistema. */
  public static SignUpCommand toCommandFromResource(SignUpResource resource) {
    return new SignUpCommand(
            resource.username(),
            resource.name(),
            resource.surname(),
            resource.imgUrl(),
            resource.email(),
            resource.password(),
            List.of(Role.getDefaultRole()));
  }
}
