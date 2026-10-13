package com.hub.synhub_backend.iam.interfaces.rest.transform;

import com.hub.synhub_backend.iam.domain.model.commands.SignUpCommand;
import com.hub.synhub_backend.iam.domain.model.entities.Role;
import com.hub.synhub_backend.iam.interfaces.rest.resources.SignUpResource;

import java.util.List;

public class SignUpCommandFromResourceAssembler {
  /** Avatar que sirve la web (frontend/public) cuando el usuario no indica una imagen; User.imgUrl es obligatorio. */
  public static final String DEFAULT_AVATAR = "/default-avatar.jpg";

  /** El registro público siempre crea cuentas con el rol por defecto; los roles los asigna el sistema. */
  public static SignUpCommand toCommandFromResource(SignUpResource resource) {
    return new SignUpCommand(
            resource.username(),
            resource.name(),
            resource.surname(),
            resource.imgUrl() == null || resource.imgUrl().isBlank() ? DEFAULT_AVATAR : resource.imgUrl(),
            resource.email(),
            resource.password(),
            List.of(Role.getDefaultRole()));
  }
}
