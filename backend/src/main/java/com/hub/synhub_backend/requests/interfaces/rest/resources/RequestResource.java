package com.hub.synhub_backend.requests.interfaces.rest.resources;

import com.hub.synhub_backend.tasks.interfaces.rest.resources.TaskResource;

public record RequestResource(
        Long id,
        String description,
        String requestType,
        String requestStatus,
        TaskResource task
) {
}
