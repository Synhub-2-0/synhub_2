package com.hub.synhub_backend.tasks.aplication.internal.commandservices;

import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupProfileRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupRepository;
import com.hub.synhub_backend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.commands.*;
import com.hub.synhub_backend.tasks.domain.services.TaskCommandService;
import com.hub.synhub_backend.tasks.infrastructure.persistence.jpa.repositories.TaskRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class TaskCommandServiceImpl implements TaskCommandService {

    private final TaskRepository taskRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupProfileRepository groupProfileRepository;

    public TaskCommandServiceImpl(
            TaskRepository taskRepository,
            UserRepository userRepository,
            GroupRepository groupRepository,
            GroupProfileRepository groupProfileRepository) {
        this.taskRepository = taskRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.groupProfileRepository = groupProfileRepository;
    }

    @Transactional
    @Override
    public Task handle(CreateTaskCommand command) {
        var user = this.userRepository.findById(command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + command.userId() + " does not exist"));
        var group = this.groupRepository.findById(command.groupId())
                .orElseThrow(() -> new ResourceNotFoundException("Group with id " + command.groupId() + " does not exist"));
        if (!group.getLeader().getId().equals(command.requesterId())) {
            throw new AccessDeniedException("User with id " + command.requesterId() + " is not the leader of the group");
        }
        var profile = this.groupProfileRepository.findByGroup_IdAndUser_Id(group.getId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + command.userId() + " is not in group with id " + command.groupId()));

        var task = new Task(command);
        profile.addTask(task);
        taskRepository.save(task);
        return task;
    }

    @Override
    @Transactional
    public Task handle(UpdateTaskCommand command) {
        var task = this.taskRepository.findById(command.taskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + command.taskId() + " does not exist"));
        this.userRepository.findById(command.newUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + command.newUserId() + " does not exist"));

        var currentUser = task.getProfile().getUser();
        var groupId = task.getProfile().getGroup().getId();

        var group = this.groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group with id " + groupId + " does not exist"));

        if (!group.getLeader().getId().equals(command.requesterId())) {
            throw new AccessDeniedException("Only the leader can edit task details");
        }

        var currentProfile = task.getProfile();
        var newProfile = this.groupProfileRepository.findByGroup_IdAndUser_Id(groupId, command.newUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + command.newUserId() + " is not part of group with id " + groupId));

        if (currentProfile != null && !currentProfile.equals(newProfile)) {
            currentProfile.removeTask(task);
            this.groupProfileRepository.save(currentProfile);
            newProfile.addTask(task);
            task.setProfile(newProfile);
            this.groupProfileRepository.save(newProfile);
        } else if (currentUser == null) {
            newProfile.addTask(task);
            task.setProfile(newProfile);
            this.groupProfileRepository.save(newProfile);
        }
        task.updateTask(command);
        return this.taskRepository.save(task);
    }

    @Override
    @Transactional
    public void handle(DeleteTaskCommand command) {
        var task = this.taskRepository.findById(command.taskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + command.taskId() + " does not exist"));
        if (!task.getProfile().getGroup().getLeader().getId().equals(command.requesterId())) {
            throw new AccessDeniedException("Only the leader of the group can delete tasks");
        }
        var profile = task.getProfile();
        if (profile != null) {
            profile.removeTask(task);
            this.groupProfileRepository.save(profile);
        }
        this.taskRepository.deleteById(task.getId());
    }

    @Override
    public Optional<Task> handle(UpdateTaskStatusCommand command) {
        var taskId = command.taskId();
        if(!taskRepository.existsById(taskId)) {
            throw new IllegalArgumentException("Task with id " + taskId + " does not exist");
        }

        var taskToUpdate = this.taskRepository.findById(taskId).get();

        try{
            taskToUpdate.updateStatus(command);
            var updatedTask = this.taskRepository.save(taskToUpdate);
            return Optional.of(updatedTask);
        } catch (Exception e){
            throw new IllegalArgumentException("Error updating task status: " + e.getMessage());
        }
    }

    @Override
    public void handle(DeleteTasksByUserIdAndGroupIdCommand command) {
        if (!this.userRepository.existsById(command.userId())) {
            throw new IllegalArgumentException("User with id " + command.userId() + " does not exist");
        }

        if (!this.groupRepository.existsById(command.groupId())) {
            throw new IllegalArgumentException("Group with id: " + command.groupId() + " does not exist");
        }

        var profileOpt = this.groupProfileRepository.findByGroup_IdAndUser_Id(command.groupId(), command.userId());
        if(profileOpt.isEmpty()) {
            throw new IllegalArgumentException("User with id " + command.userId() + " is not in group with id " + command.groupId());
        }

        var profile = profileOpt.get();

        try {
            var tasks = this.taskRepository.findByProfile_Id(profile.getId());
            if (tasks.isEmpty()) {
                return;
            }
            for (var task : tasks) {
                var taskProfile = task.getProfile();
                if (taskProfile != null) {
                    taskProfile.removeTask(task);
                    this.groupProfileRepository.save(taskProfile);
                }
                this.taskRepository.delete(task);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error deleting tasks for member: " + e.getMessage());
        }
    }
}
