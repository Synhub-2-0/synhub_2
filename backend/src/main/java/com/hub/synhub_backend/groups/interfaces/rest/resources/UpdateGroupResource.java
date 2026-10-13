package com.hub.synhub_backend.groups.interfaces.rest.resources;

import jakarta.validation.constraints.Size;

/** Los campos vacíos o ausentes no cambian. */
public record UpdateGroupResource(
        @Size(max = 100) String name,
        @Size(max = 1024) String imgUrl,
        @Size(max = 2000) String description) {}
