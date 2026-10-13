package com.hub.synhub_backend.tasks.application.internal.commandservices;

import com.hub.synhub_backend.groups.interfaces.acl.GroupsContextFacade;
import com.hub.synhub_backend.iam.interfaces.acl.IamContextFacade;
import com.hub.synhub_backend.requests.interfaces.acl.RequestsContextFacade;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import com.hub.synhub_backend.tasks.domain.exceptions.TaskStatusTransitionNotAllowedException;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.commands.*;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import com.hub.synhub_backend.tasks.domain.services.TaskCommandService;
import com.hub.synhub_backend.tasks.infrastructure.persistence.jpa.repositories.TaskRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TaskCommandServiceImpl implements TaskCommandService {

    private final TaskRepository taskRepository;
    private final IamContextFacade iamContextFacade;
    private final GroupsContextFacade groupsContextFacade;
    private final RequestsContextFacade requestsContextFacade;

    /** Usuarios y membresías llegan por las ACL de iam y groups, no por sus repositorios (AUDIT ARC-01). */
    public TaskCommandServiceImpl(
            TaskRepository taskRepository,
            IamContextFacade iamContextFacade,
            GroupsContextFacade groupsContextFacade,
            RequestsContextFacade requestsContextFacade) {
        this.taskRepository = taskRepository;
        this.iamContextFacade = iamContextFacade;
        this.groupsContextFacade = groupsContextFacade;
        this.requestsContextFacade = requestsContextFacade;
    }

    @Transactional
    @Override
    public Task handle(CreateTaskCommand command) {
        var user = this.iamContextFacade
                .fetchUserById(command.userId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("User with id " + command.userId() + " does not exist"));
        var leaderId = this.groupsContextFacade
                .fetchLeaderIdByGroupId(command.groupId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Group with id " + command.groupId() + " does not exist"));
        if (!leaderId.equals(command.requesterId())) {
            throw new AccessDeniedException(
                    "User with id " + command.requesterId() + " is not the leader of the group");
        }
        var profile = this.groupsContextFacade
                .fetchMembership(command.groupId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id " + command.userId() + " is not in group with id " + command.groupId()));

        var task = new Task(command);
        profile.addTask(task);
        taskRepository.save(task);
        return task;
    }

    @Override
    @Transactional
    public Task handle(UpdateTaskCommand command) {
        var task = this.taskRepository
                .findById(command.taskId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Task with id " + command.taskId() + " does not exist"));
        this.iamContextFacade
                .fetchUserById(command.newUserId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("User with id " + command.newUserId() + " does not exist"));

        var currentUser = task.getProfile().getUser();
        var groupId = task.getProfile().getGroup().getId();

        if (!this.groupsContextFacade.isLeader(groupId, command.requesterId())) {
            throw new AccessDeniedException("Only the leader can edit task details");
        }
        task.ensureNotFinished();

        var currentProfile = task.getProfile();
        var newProfile = this.groupsContextFacade
                .fetchMembership(groupId, command.newUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id " + command.newUserId() + " is not part of group with id " + groupId));

        if (currentProfile != null && !currentProfile.equals(newProfile)) {
            // Las membresías son entidades gestionadas: los cambios se guardan al cerrar la transacción.
            currentProfile.removeTask(task);
            newProfile.addTask(task);
        } else if (currentUser == null) {
            newProfile.addTask(task);
        }
        task.updateTask(command);
        return this.taskRepository.save(task);
    }

    @Override
    @Transactional
    public void handle(DeleteTaskCommand command) {
        var task = this.taskRepository
                .findById(command.taskId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Task with id " + command.taskId() + " does not exist"));
        if (!task.getProfile().getGroup().getLeader().getId().equals(command.requesterId())) {
            throw new AccessDeniedException("Only the leader of the group can delete tasks");
        }
        task.ensureNotFinished();
        // Soft delete (HUM-03): la tarea conserva su perfil; solo sale de la colección en memoria.
        var profile = task.getProfile();
        if (profile != null) profile.detachTask(task);
        // Sus requests se borran con ella, en la misma transacción (AUDIT BUG-01).
        this.requestsContextFacade.deleteAllRequestsByTaskId(task.getId());
        this.taskRepository.delete(task);
    }

    @Override
    public Task handle(UpdateTaskStatusCommand command) {
        var taskToUpdate = this.taskRepository
                .findById(command.taskId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Task with id " + command.taskId() + " does not exist"));
        if (command.status() == TaskStatus.DONE)
            throw new TaskStatusTransitionNotAllowedException(
                    "A task can only be marked as DONE by approving its submission request");
        if (!(taskToUpdate.getProfile().getGroup().getLeader().getId().equals(command.requesterId())
                || taskToUpdate.getProfile().getUser().getId().equals(command.requesterId()))) {
            throw new AccessDeniedException("Only the leader or the assigned member can update the task status");
        }
        taskToUpdate.ensureNotFinished();
        taskToUpdate.updateStatus(command);
        return this.taskRepository.save(taskToUpdate);
    }

    @Override
    @Transactional
    public void handle(DeleteTasksByUserIdAndGroupIdCommand command) {
        if (this.iamContextFacade.fetchUserById(command.userId()).isEmpty()) {
            throw new IllegalArgumentException("User with id " + command.userId() + " does not exist");
        }

        if (this.groupsContextFacade.fetchLeaderIdByGroupId(command.groupId()).isEmpty()) {
            throw new IllegalArgumentException("Group with id: " + command.groupId() + " does not exist");
        }

        var profileOpt = this.groupsContextFacade.fetchMembership(command.groupId(), command.userId());
        if (profileOpt.isEmpty()) {
            throw new IllegalArgumentException(
                    "User with id " + command.userId() + " is not in group with id " + command.groupId());
        }

        var profile = profileOpt.get();

        try {
            var tasks = this.taskRepository.findByProfile_Id(profile.getId());
            if (tasks.isEmpty()) {
                return;
            }
            for (var task : tasks) {
                var taskProfile = task.getProfile();
                if (taskProfile != null) taskProfile.detachTask(task);
                this.requestsContextFacade.deleteAllRequestsByTaskId(task.getId());
                this.taskRepository.delete(task);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error deleting tasks for member: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void handle(DeleteTasksByGroupIdCommand command) {
        for (var task : this.taskRepository.findByProfile_Group_Id(command.groupId())) {
            this.requestsContextFacade.deleteAllRequestsByTaskId(task.getId());
            var profile = task.getProfile();
            if (profile != null) profile.detachTask(task);
            this.taskRepository.delete(task);
        }
    }
}
