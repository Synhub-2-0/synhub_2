package com.hub.synhub_backend.tasks.aplication.internal.commandservices;

import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupRepository;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.commands.*;
import com.hub.synhub_backend.tasks.domain.services.TaskCommandService;
import com.hub.synhub_backend.tasks.infrastructure.persistence.jpa.repositories.MemberRepository;
import com.hub.synhub_backend.tasks.infrastructure.persistence.jpa.repositories.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TaskCommandServiceImpl implements TaskCommandService {

    private final TaskRepository taskRepository;
    private final MemberRepository memberRepository;
    private final GroupRepository groupRepository;

    public TaskCommandServiceImpl(TaskRepository taskRepository, MemberRepository memberRepository, GroupRepository groupRepository) {
        this.taskRepository = taskRepository;
        this.memberRepository = memberRepository;
        this.groupRepository = groupRepository;
    }

    @Override
    public Optional<Task> handle(CreateTaskCommand command) {
        var task = new Task(command);
        var member = this.memberRepository.findById(command.memberId());
        if (member.isEmpty()) {
            throw new IllegalArgumentException("Member with id " + command.memberId() + " does not exist");
        }

        var groupId = member.get().getGroup() != null ? member.get().getGroup().getId() : null;

        if(groupId == null) {
            throw new IllegalArgumentException("Member with id " + command.memberId() + " does not belong to any group");
        }

        var group = this.groupRepository.findById(groupId);

        if (group.isEmpty()) {
            throw new IllegalArgumentException("Group with id " + groupId + " does not exist");
        }

        task.setMember(member.get());
        task.setGroup(group.get());
        member.get().addTask(task);

        this.memberRepository.save(member.get());
        var createdTask = taskRepository.save(task);

        return Optional.of(createdTask);
    }

    @Override
    public Optional<Task> handle(UpdateTaskCommand command) {
        var taskOpt = this.taskRepository.findById(command.taskId());
        var newMemberOpt = this.memberRepository.findById(command.memberId());

        if (taskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task with id " + command.taskId() + " does not exist");
        }
        if (newMemberOpt.isEmpty()) {
            throw new IllegalArgumentException("Member with id " + command.memberId() + " does not exist");
        }

        var task = taskOpt.get();
        var currentMember = task.getMember();
        var newMember = newMemberOpt.get();

        var groupId = newMember.getGroup() != null ? newMember.getGroup().getId() : null;

        if(groupId == null) {
            throw new IllegalArgumentException("Member with id " + command.memberId() + " does not belong to any group");
        }

        var group = this.groupRepository.findById(groupId);

        if (group.isEmpty()) {
            throw new IllegalArgumentException("Group with id " + groupId + " does not exist");
        }

        var newGroup = this.groupRepository.findById(newMember.getGroup().getId());

        if (currentMember != null && !currentMember.equals(newMember)) {
            currentMember.removeTask(task);
            this.memberRepository.save(currentMember);
            newMember.addTask(task);
            task.setMember(newMember);
            task.setGroup(newGroup.get());
            this.memberRepository.save(newMember);
        } else if (currentMember == null) {
            newMember.addTask(task);
            task.setMember(newMember);
            task.setGroup(newGroup.get());
            this.memberRepository.save(newMember);
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
        var taskId = command.taskId();
        if(!taskRepository.existsById(taskId)) {
            throw new IllegalArgumentException("Task with id " + taskId + " does not exist");
        }
        try {
            var member = this.taskRepository.findById(taskId).get().getMember();
            if (member != null) {
                member.removeTask(this.taskRepository.findById(taskId).get());
                this.memberRepository.save(member);
            }
            this.taskRepository.deleteById(taskId);
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
    public void handle(DeleteTasksByMemberId command) {
        var memberId = command.memberId();
        if(!this.memberRepository.existsById(memberId)) {
            throw new IllegalArgumentException("Member with id " + memberId + " does not exist");
        }
        try {
            var tasks = this.taskRepository.findByMember_Id(memberId);
            if (tasks.isEmpty()) {
                return;
            }
            for (var task : tasks) {
                var member = task.getMember();
                if (member != null) {
                    member.removeTask(task);
                    this.memberRepository.save(member);
                }
                this.taskRepository.delete(task);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error deleting tasks for member: " + e.getMessage());
        }
    }
}
