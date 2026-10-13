package com.hub.synhub_backend.tasks.aplication.internal.queryservices;

import com.hub.synhub_backend.groups.interfaces.acl.GroupsContextFacade;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.queries.*;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import com.hub.synhub_backend.tasks.domain.services.TaskQueryService;
import com.hub.synhub_backend.tasks.infrastructure.persistence.jpa.repositories.TaskRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TaskQueryServiceImpl implements TaskQueryService {

    private final TaskRepository taskRepository;
    private final GroupsContextFacade groupsContextFacade;

    public TaskQueryServiceImpl(TaskRepository taskRepository, GroupsContextFacade groupsContextFacade) {
        this.taskRepository = taskRepository;
        this.groupsContextFacade = groupsContextFacade;
    }

    private void requireMember(Long groupId, Long requesterId) {
        if (!groupsContextFacade.isMember(groupId, requesterId)) {
            throw new AccessDeniedException("Only members of the group can see its tasks");
        }
    }


    @Override
    public List<Task> handle(GetAllTasksQuery query) {
        return this.taskRepository.findAll();
    }

    @Override
    public Optional<Task> handle(GetTaskByIdQuery query) {
        var task = this.taskRepository.findById(query.taskId());
        task.ifPresent(found -> requireMember(found.getProfile().getGroup().getId(), query.requesterId()));
        return task;
    }

    @Override
    public List<Task> handle(GetAllTasksByUserId query) {
        return this.taskRepository.findByProfile_User_Id(query.userId());
    }

    @Override
    public List<Task> handle(GetAllTaskByStatusQuery query) {
        TaskStatus taskStatus = TaskStatus.valueOf(query.taskStatus());
        return taskRepository.findByStatus(taskStatus);
    }

    @Override
    public List<Task> handle(GetAllTasksByGroupIdQuery query) {
        return taskRepository.findByProfile_Group_Id(query.groupId());
    }

    @Override
    public List<Task> handle(GetTasksByGroupIdAndStatus query) {
        requireMember(query.groupId(), query.requesterId());
        if (query.status() == null) {
            return taskRepository.findByProfile_Group_Id(query.groupId());
        }
        return taskRepository.findByProfile_Group_IdAndStatus(query.groupId(), query.status());
    }

    @Override
    public List<Task> handle(GetTasksByGroupIdAndUserIdQuery query) {
        requireMember(query.groupId(), query.requesterId());
        return taskRepository.findByProfile_Group_IdAndProfile_User_Id(query.groupId(), query.userId());
    }
}
