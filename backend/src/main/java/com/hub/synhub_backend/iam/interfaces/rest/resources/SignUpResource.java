package com.hub.synhub_backend.iam.interfaces.rest.resources;

/**
 * Registro público. No acepta roles: toda cuenta nueva recibe ROLE_USER (AUDIT SEC-05).
 * Un campo accountRoles enviado por un cliente antiguo se ignora.
 */
public record SignUpResource(
        String username,
        String name,
        String surname,
        String imgUrl,
        String email,
        String password
) {
}
