package com.hub.synhub_backend.tasks.interfaces.acl;

import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;

import java.util.Optional;

public interface TasksContextFacade {
    Optional<Task> getTaskById(Long taskId);
    void deleteTasksByUserIdAndGroupId(Long userId, Long groupId);
    /** Aplica a la tarea el resultado de revisar su entrega (SUBMISSION). */
    Task applySubmissionReview(Long taskId, boolean approved);
}
