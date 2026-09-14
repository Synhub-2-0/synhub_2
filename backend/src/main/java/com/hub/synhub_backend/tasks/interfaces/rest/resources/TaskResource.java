package com.hub.synhub_backend.tasks.interfaces.rest.resources;


public record TaskResource(
        Long id,
        String title,
        String description,
        String dueDate,
        String createdAt,
        String updatedAt,
        String status,
        TaskMemberResource member,
        Long groupId
) {
}
