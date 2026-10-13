package com.hub.synhub_backend.iam.interfaces.rest.resources;

/**
 * Datos de un usuario que pueden ver los demás: nombre, correo y foto (HUM-06).
 * Los roles de la cuenta solo se exponen en {@link UserResource} (/users/me).
 */
public record PublicUserResource(Long id, String username, String name, String surname, String imgUrl, String email) {}
