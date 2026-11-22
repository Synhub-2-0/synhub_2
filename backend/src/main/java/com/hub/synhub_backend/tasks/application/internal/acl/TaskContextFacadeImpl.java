package com.hub.synhub_backend.tasks.application.internal.acl;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.commands.DeleteTasksByGroupIdCommand;
import com.hub.synhub_backend.tasks.domain.model.commands.DeleteTasksByUserIdAndGroupIdCommand;
import com.hub.synhub_backend.tasks.domain.model.commands.UpdateTaskStatusCommand;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import com.hub.synhub_backend.tasks.domain.services.TaskCommandService;
import com.hub.synhub_backend.tasks.domain.services.TaskQueryService;
import com.hub.synhub_backend.tasks.infrastructure.persistence.jpa.repositories.TaskRepository;
import com.hub.synhub_backend.tasks.interfaces.acl.TasksContextFacade;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class TaskContextFacadeImpl implements TasksContextFacade {
    private final TaskCommandService taskCommandService;
    private final TaskQueryService taskQueryService;
    private final TaskRepository taskRepository;

    public TaskContextFacadeImpl(
            TaskCommandService taskCommandService, TaskQueryService taskQueryService, TaskRepository taskRepository) {
        this.taskCommandService = taskCommandService;
        this.taskQueryService = taskQueryService;
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

    // DONE solo se alcanza por aquí: el endpoint de status lo rechaza (TaskStatusTransitionNotAllowedException).
    @Override
    public Task applySubmissionReview(Long taskId, boolean approved) {
        var task = taskRepository
                .findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + taskId + " does not exist"));
        task.applySubmissionReview(approved);
        return taskRepository.save(task);
    }

    @Override
    public Task completeForSubmission(Long taskId) {
        var task = taskRepository
                .findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + taskId + " does not exist"));
        task.ensureNotFinished();
        task.updateStatus(new UpdateTaskStatusCommand(null, taskId, TaskStatus.COMPLETED));
        return taskRepository.save(task);
    }

    @Override
    public Task putOnHoldForModification(Long taskId) {
        var task = taskRepository
                .findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + taskId + " does not exist"));
        task.ensureNotFinished();
        task.updateStatus(new UpdateTaskStatusCommand(null, taskId, TaskStatus.ON_HOLD));
        return taskRepository.save(task);
    }

    @Override
    public Task updateTaskStatus(Long taskId, TaskStatus status, Long requesterId) {
        var updateStatusCommand = new UpdateTaskStatusCommand(requesterId, taskId, status);
        var newTask = taskCommandService.handle(updateStatusCommand);
        return newTask;
    }

    @Override
    public void deleteAllTasksByGroupId(Long groupId) {
        taskCommandService.handle(new DeleteTasksByGroupIdCommand(groupId));
    }

    @Override
    public List<Task> fetchTasksByGroupId(Long groupId) {
        return taskRepository.findByProfile_Group_Id(groupId);
    }

    @Override
    public List<Task> fetchTasksByGroupIdAndUserId(Long groupId, Long userId) {
        return taskRepository.findByProfile_Group_IdAndProfile_User_Id(groupId, userId);
    }
}
