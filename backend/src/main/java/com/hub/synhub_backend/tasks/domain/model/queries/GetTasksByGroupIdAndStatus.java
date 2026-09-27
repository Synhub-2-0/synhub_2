package com.hub.synhub_backend.tasks.domain.model.queries;

import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;

public record GetTasksByGroupIdAndStatus(Long groupId, TaskStatus status) {
}
