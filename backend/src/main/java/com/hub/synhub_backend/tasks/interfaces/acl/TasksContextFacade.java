package com.hub.synhub_backend.tasks.interfaces.acl;

import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;

import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;

import java.util.Optional;

public interface TasksContextFacade {
    Optional<Task> getTaskById(Long taskId);
    void deleteTasksByUserIdAndGroupId(Long userId, Long groupId);
    Task updateTaskStatus(Long requesterId, Long taskId, TaskStatus status);
}
