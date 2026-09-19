package com.hub.synhub_backend.tasks.aplication.internal.commandservices;

import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupProfileRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupRepository;
import com.hub.synhub_backend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.commands.*;
import com.hub.synhub_backend.tasks.domain.services.TaskCommandService;
import com.hub.synhub_backend.tasks.domain.services.TaskStatusScheduler;
import com.hub.synhub_backend.tasks.infrastructure.persistence.jpa.repositories.TaskRepository;
import org.springframework.stereotype.Service;

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

    @Override
    public Optional<Task> handle(CreateTaskCommand command) {
        var task = new Task(command);
        var user = this.userRepository.findById(command.userId());
        if (user.isEmpty()) {
            throw new IllegalArgumentException("User with id " + command.userId() + " does not exist");
        }
        var group = this.groupRepository.findById(command.groupId());
        if (group.isEmpty()) {
            throw new IllegalArgumentException("Group with id " + command.groupId() + " does not exist");
        }
        var profile = this.groupProfileRepository.findByGroup_IdAndUser_Id(group.get().getId(), user.get().getId());
        if (profile.isEmpty()) {
            throw new IllegalArgumentException("Profile for user with id " + command.userId() + " in group with id " + command.groupId() + " not found");
        }

        profile.get().addTask(task);

        this.groupProfileRepository.save(profile.get());
        var createdTask = taskRepository.save(task);

        return Optional.of(createdTask);
    }

    @Override
    public Optional<Task> handle(UpdateTaskCommand command) {
        var taskOpt = this.taskRepository.findById(command.taskId());
        var newUserOpt = this.userRepository.findById(command.userId());

        if (taskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task with id " + command.taskId() + " does not exist");
        }
        if (newUserOpt.isEmpty()) {
            throw new IllegalArgumentException("User with id " + command.userId() + " does not exist");
        }

        var task = taskOpt.get();
        var currentUser = task.getProfile().getUser();

        var groupId = task.getProfile().getGroup().getId();
        var group = this.groupRepository.findById(groupId);

        if (group.isEmpty()) {
            throw new IllegalArgumentException("Group with id " + groupId + " does not exist");
        }

        var currentProfile = task.getProfile();
        var newProfileOpt = this.groupProfileRepository.findByGroup_IdAndUser_Id(groupId, command.userId());

        if (newProfileOpt.isEmpty()) {
            throw new IllegalArgumentException("User with id " + command.userId() + " is not part of group with id " + groupId);
        }

        var newProfile = newProfileOpt.get();

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

        try{
            var updatedTask = this.taskRepository.save(task);
            return Optional.of(updatedTask);
        } catch (Exception e){
            throw new IllegalArgumentException("Error updating task: " + e.getMessage());
        }
    }

    @Override
    public void handle(DeleteTaskCommand command) {
        var taskOpt = this.taskRepository.findById(command.taskId());
        if(taskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task with id " + command.taskId() + " does not exist");
        }
        var task = taskOpt.get();
        try {
            var profile = task.getProfile();
            if (profile != null) {
                profile.removeTask(task);
                this.groupProfileRepository.save(profile);
            }
            this.taskRepository.deleteById(task.getId());
        } catch (Exception e) {
            throw new IllegalArgumentException("Error deleting task: " + e.getMessage());
        }
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
