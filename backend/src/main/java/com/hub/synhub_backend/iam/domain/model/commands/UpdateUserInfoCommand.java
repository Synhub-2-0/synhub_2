package com.hub.synhub_backend.iam.domain.model.commands;

public record UpdateUserInfoCommand(
        String name,
        String surname,
        String urlImage,
        String email
) {
}
