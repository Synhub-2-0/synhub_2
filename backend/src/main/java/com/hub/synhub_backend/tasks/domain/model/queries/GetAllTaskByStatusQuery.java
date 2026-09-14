package com.hub.synhub_backend.tasks.domain.model.queries;

/**
 * GetAllTaskByStatusQuery: Query to get all tasks by status
 * @param taskStatus
 */
public record GetAllTaskByStatusQuery(String taskStatus) {
}
