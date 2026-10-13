package com.hub.synhub_backend.groups.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGroupResource(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 1024) String imgUrl,
        @Size(max = 2000) String description
) {
}
