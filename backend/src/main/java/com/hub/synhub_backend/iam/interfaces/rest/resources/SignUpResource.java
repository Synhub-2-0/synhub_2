package com.hub.synhub_backend.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registro público. No acepta roles: toda cuenta nueva recibe ROLE_USER (AUDIT SEC-05).
 * Un campo accountRoles enviado por un cliente antiguo se ignora. imgUrl es opcional.
 */
public record SignUpResource(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Size(max = 50) String surname,
        @Size(max = 1024) String imgUrl,
        @NotBlank @Email @Size(max = 254) String email,
        // BCrypt solo usa los primeros 72 bytes de la contraseña.
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
