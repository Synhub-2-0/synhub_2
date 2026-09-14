package com.hub.synhub_backend.tasks.domain.services;

import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.commands.*;

import java.util.Optional;

public interface TaskCommandService {
    Optional<Task> handle(CreateTaskCommand command);
    Optional<Task> handle(UpdateTaskCommand command);
    void handle(DeleteTaskCommand command);
    Optional<Task> handle(UpdateTaskStatusCommand command);
    void handle(DeleteTasksByMemberId command);
}
