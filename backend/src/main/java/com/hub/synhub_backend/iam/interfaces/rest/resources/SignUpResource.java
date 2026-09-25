package com.hub.synhub_backend.iam.interfaces.rest.resources;

import com.hub.synhub_backend.iam.domain.model.valueobjects.AccountRole;

import java.util.List;

public record SignUpResource(
        String username,
        String name,
        String surname,
        String imgUrl,
        String email,
        String password,
        List<AccountRole> accountRoles
) {
}

