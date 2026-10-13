package com.hub.synhub_backend.tasks.interfaces.rest.resources;


import com.hub.synhub_backend.groups.interfaces.rest.resources.GroupResourceReduced;
import com.hub.synhub_backend.iam.interfaces.rest.resources.PublicUserResource;

public record TaskResource(
        Long id,
        String title,
        String description,
        String dueDate,
        String createdAt,
        String updatedAt,
        String status,
        GroupResourceReduced group,
        PublicUserResource assignedTo,
        Integer difficulty,
        String context,
        String urgency,
        String labels,
        Boolean aiAccepted
) {
}
