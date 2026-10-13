package com.hub.synhub_backend.groups.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record JoinGroupResource(@NotBlank String code) {
}
