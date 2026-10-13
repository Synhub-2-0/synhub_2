package com.hub.synhub_backend.groups.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;

public record CreateInvitationResource(
        @NotNull Long userId, @NotNull Long groupId) {}
