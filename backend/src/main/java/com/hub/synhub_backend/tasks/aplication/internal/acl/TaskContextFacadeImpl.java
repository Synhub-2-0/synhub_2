package com.hub.synhub_backend.tasks.aplication.internal.acl;

import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.commands.DeleteTasksByUserIdAndGroupIdCommand;
import com.hub.synhub_backend.tasks.domain.services.TaskCommandService;
import com.hub.synhub_backend.tasks.infrastructure.persistence.jpa.repositories.TaskRepository;
import com.hub.synhub_backend.tasks.interfaces.acl.TasksContextFacade;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TaskContextFacadeImpl implements TasksContextFacade {

    private final TaskCommandService taskCommandService;
    private final TaskRepository taskRepository;

    public TaskContextFacadeImpl(TaskCommandService taskCommandService, TaskRepository taskRepository) {
        this.taskCommandService = taskCommandService;
        this.taskRepository = taskRepository;
    }

    @Override
    public Optional<Task> getTaskById(Long id) {
        var task = taskRepository.findById(id);
        return task;
    }

    @Override
    public void deleteTasksByUserIdAndGroupId(Long userId, Long groupId) {
        var command = new DeleteTasksByUserIdAndGroupIdCommand(userId, groupId);
        taskCommandService.handle(command);
    }
}
