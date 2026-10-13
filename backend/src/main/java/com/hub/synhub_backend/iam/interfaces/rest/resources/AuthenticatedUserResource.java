package com.hub.synhub_backend.iam.interfaces.rest.resources;

import java.util.List;

/**
 * Respuesta del login. El token no viaja en el cuerpo: va en la cookie HttpOnly de sesión (HUM-04).
 * {@code expiresAt} (ISO-8601, UTC) le indica a la web cuándo vence la sesión.
 */
public record AuthenticatedUserResource(Long id, String username, List<String> role, String expiresAt) {
}
