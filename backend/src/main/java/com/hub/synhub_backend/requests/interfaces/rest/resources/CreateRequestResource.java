package com.hub.synhub_backend.requests.interfaces.rest.resources;

public record CreateRequestResource(
        String description,
        String requestType
) {
}